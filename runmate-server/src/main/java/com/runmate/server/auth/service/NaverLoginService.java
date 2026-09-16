package com.runmate.server.auth.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.runmate.server.auth.dto.AuthDtos.NaverLoginResult;
import com.runmate.server.auth.dto.NaverPendingProfile;
import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.auth.security.RunmatePrincipal;
import com.runmate.server.user.domain.SocialAccount;
import com.runmate.server.user.domain.SocialProvider;
import com.runmate.server.user.domain.User;
import com.runmate.server.user.domain.UserStatus;
import com.runmate.server.user.repository.SocialAccountRepository;
import com.runmate.server.user.service.ProfileImageUrlResolver;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;

@Service
@RequiredArgsConstructor
public class NaverLoginService {
    private static final String TOKEN_URL = "https://nid.naver.com/oauth2.0/token";
    private static final String USER_INFO_URL = "https://openapi.naver.com/v1/nid/me";

    private final SocialAccountRepository socialAccountRepository;
    private final ProfileImageUrlResolver profileImageUrlResolver;

    @Value("${naver.client-id}")
    private String clientId;

    @Value("${naver.client-secret}")
    private String clientSecret;

    @Transactional
    public NaverLoginResult login(String authorizationCode, String state) {
        validateConfiguration();
        try {
            String accessToken = requestAccessToken(authorizationCode, state);
            NaverPendingProfile profile = requestProfile(accessToken);
            return socialAccountRepository
                    .findByProviderAndProviderUserId(SocialProvider.NAVER, profile.providerUserId())
                    .map(account -> loginExistingUser(account, profile))
                    .orElseGet(() -> NaverLoginResult.signupRequired(profile));
        } catch (AuthException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AuthException("네이버 로그인 처리 중 오류가 발생했습니다.", exception);
        }
    }

    private String requestAccessToken(String authorizationCode, String state) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("code", authorizationCode);
        form.add("state", state);

        JsonNode response = RestClient.create()
                .post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(JsonNode.class);
        String accessToken = response == null ? null : response.path("access_token").asString(null);
        if (accessToken == null || accessToken.isBlank()) {
            throw new AuthException("네이버 액세스 토큰을 발급받지 못했습니다.");
        }
        return accessToken;
    }

    private NaverPendingProfile requestProfile(String accessToken) {
        JsonNode response = RestClient.create()
                .get()
                .uri(USER_INFO_URL)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(JsonNode.class);
        JsonNode profile = response == null ? null : response.path("response");
        String providerUserId = profile == null ? null : profile.path("id").asString(null);
        if (providerUserId == null || providerUserId.isBlank()) {
            throw new AuthException("네이버 사용자 정보를 가져오지 못했습니다.");
        }
        return new NaverPendingProfile(
                providerUserId,
                profile.path("nickname").asString(""),
                profile.path("profile_image").asString(null)
        );
    }

    private NaverLoginResult loginExistingUser(SocialAccount socialAccount, NaverPendingProfile profile) {
        User user = socialAccount.getUser();
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new AuthException("이용이 정지된 계정입니다.");
        }
        if (user.getStatus() == UserStatus.DELETED) {
            throw new AuthException("탈퇴한 계정입니다.");
        }
        LocalDateTime now = LocalDateTime.now();
        user.recordLoginSuccess(now);
        socialAccount.recordLogin(now);
        return NaverLoginResult.authenticated(new RunmatePrincipal(
                user.getUserId(), user.getLoginId(), user.getNickname(), user.getRole().name(),
                profileImageUrlResolver.resolve(user, profile.profileImageUrl())
        ));
    }

    private void validateConfiguration() {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            throw new AuthException("네이버 로그인 설정이 완료되지 않았습니다.");
        }
    }
}
