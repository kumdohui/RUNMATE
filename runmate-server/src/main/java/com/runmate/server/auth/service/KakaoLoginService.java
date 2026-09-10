package com.runmate.server.auth.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;
import com.runmate.server.auth.dto.AuthDtos.KakaoLoginResult;
import com.runmate.server.auth.dto.KakaoPendingProfile;
import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.auth.security.RunmatePrincipal;
import com.runmate.server.user.domain.SocialAccount;
import com.runmate.server.user.domain.SocialProvider;
import com.runmate.server.user.domain.User;
import com.runmate.server.user.domain.UserStatus;
import com.runmate.server.user.repository.SocialAccountRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KakaoLoginService {

    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    private final SocialAccountRepository socialAccountRepository;

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.client-secret}")
    private String clientSecret;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    @Transactional
    public KakaoLoginResult login(String authorizationCode) {
        try {
            String accessToken = requestAccessToken(authorizationCode);
            KakaoPendingProfile profile = requestProfile(accessToken);

            return socialAccountRepository
                    .findByProviderAndProviderUserId(
                            SocialProvider.KAKAO,
                            profile.providerUserId()
                    )
                    .map(account -> loginExistingUser(account, profile))
                    .orElseGet(() -> KakaoLoginResult.signupRequired(profile));
        } catch (AuthException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AuthException("카카오 로그인 처리 중 오류가 발생했습니다.", exception);
        }
    }

    private String requestAccessToken(String authorizationCode) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("redirect_uri", redirectUri);
        form.add("code", authorizationCode);
        form.add("client_secret", clientSecret);

        JsonNode response = RestClient.create()
                .post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(JsonNode.class);

        String accessToken = response == null ? null : response.path("access_token").asText(null);
        if (accessToken == null || accessToken.isBlank()) {
            throw new AuthException("카카오 액세스 토큰을 발급받지 못했습니다.");
        }
        return accessToken;
    }

    private KakaoPendingProfile requestProfile(String accessToken) {
        JsonNode response = RestClient.create()
                .get()
                .uri(USER_INFO_URL)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(JsonNode.class);

        if (response == null || response.path("id").isMissingNode()) {
            throw new AuthException("카카오 사용자 정보를 가져오지 못했습니다.");
        }

        JsonNode properties = response.path("properties");
        String nickname = properties.path("nickname").asText("카카오 러너");
        String profileImageUrl = properties.path("profile_image").asText(null);

        return new KakaoPendingProfile(
                response.path("id").asText(),
                nickname,
                profileImageUrl
        );
    }

    private KakaoLoginResult loginExistingUser(
            SocialAccount socialAccount,
            KakaoPendingProfile profile
    ) {
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

        RunmatePrincipal principal = new RunmatePrincipal(
                user.getUserId(),
                user.getLoginId(),
                user.getNickname(),
                user.getRole().name(),
                user.getProfileImageUrl() == null
                        ? profile.profileImageUrl()
                        : user.getProfileImageUrl()
        );
        return KakaoLoginResult.authenticated(principal);
    }
}
