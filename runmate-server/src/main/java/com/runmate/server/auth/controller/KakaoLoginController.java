package com.runmate.server.auth.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.runmate.server.auth.dto.KakaoLoginResult;
import com.runmate.server.auth.dto.KakaoPendingProfile;
import com.runmate.server.auth.dto.KakaoPendingProfileResponse;
import com.runmate.server.auth.dto.KakaoSignupRequest;
import com.runmate.server.auth.exception.KakaoLoginException;
import com.runmate.server.auth.security.RunmatePrincipal;
import com.runmate.server.auth.service.KakaoLoginService;
import com.runmate.server.auth.service.KakaoSignupService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth/kakao")
@RequiredArgsConstructor
public class KakaoLoginController {

    private static final String STATE_SESSION_KEY = "KAKAO_LOGIN_STATE";
    private static final String PENDING_PROFILE_SESSION_KEY = "KAKAO_PENDING_PROFILE";

    private static final String KAKAO_AUTHORIZE_URL =
            "https://kauth.kakao.com/oauth/authorize";

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    @Value("${kakao.frontend-url}")
    private String frontendUrl;

    private final KakaoLoginService kakaoLoginService;
    private final KakaoSignupService kakaoSignupService;
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    @GetMapping
    public ResponseEntity<Void> startLogin(
            @RequestParam(required = false) String prompt,
            HttpSession session
    ) {
        String state = UUID.randomUUID().toString();

        session.setAttribute(STATE_SESSION_KEY, state);

        UriComponentsBuilder authorizeUriBuilder = UriComponentsBuilder
                .fromUriString(KAKAO_AUTHORIZE_URL)
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state);

        if ("login".equals(prompt)) {
            authorizeUriBuilder.queryParam("prompt", "login");
        }

        URI authorizeUri = authorizeUriBuilder
                .build()
                .encode()
                .toUri();

        return ResponseEntity
                .status(302)
                .location(authorizeUri)
                .build();
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        HttpSession session = request.getSession(false);
        String savedState = session == null
                ? null
                : (String) session.getAttribute(STATE_SESSION_KEY);

        if (session != null) {
            session.removeAttribute(STATE_SESSION_KEY);
        }

        if (error != null || code == null || savedState == null || !savedState.equals(state)) {
            return redirectToFrontend("/login?kakao=failed");
        }

        try {
            KakaoLoginResult result = kakaoLoginService.login(code);
            if (result.requiresSignup()) {
                session.setAttribute(PENDING_PROFILE_SESSION_KEY, result.pendingProfile());
                return redirectToFrontend("/kakao-signup");
            }

            authenticate(result.principal(), request, response);
            return redirectToFrontend("/?kakao=success");
        } catch (KakaoLoginException exception) {
            return redirectToFrontend("/login?kakao=failed");
        }
    }

    @GetMapping("/pending")
    public KakaoPendingProfileResponse pending(HttpSession session) {
        KakaoPendingProfile profile = pendingProfile(session);
        return new KakaoPendingProfileResponse(profile.nickname(), profile.profileImageUrl());
    }

    @PostMapping("/signup")
    public com.runmate.server.auth.dto.LoginResponse signup(
            @jakarta.validation.Valid @RequestBody KakaoSignupRequest signupRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        HttpSession session = request.getSession(false);
        KakaoPendingProfile profile = pendingProfile(session);
        RunmatePrincipal principal = kakaoSignupService.signup(
                profile,
                signupRequest,
                request.getRemoteAddr(),
                request.getHeader("User-Agent")
        );
        session.removeAttribute(PENDING_PROFILE_SESSION_KEY);
        authenticate(principal, request, response);
        return new com.runmate.server.auth.dto.LoginResponse(
                principal.userId(),
                principal.loginId(),
                principal.nickname(),
                principal.role(),
                principal.profileImageUrl()
        );
    }

    private KakaoPendingProfile pendingProfile(HttpSession session) {
        if (session == null) {
            throw new KakaoLoginException("카카오 로그인 정보가 만료되었습니다.");
        }
        Object value = session.getAttribute(PENDING_PROFILE_SESSION_KEY);
        if (!(value instanceof KakaoPendingProfile profile)) {
            throw new KakaoLoginException("카카오 로그인 정보가 만료되었습니다.");
        }
        return profile;
    }

    private void authenticate(
            RunmatePrincipal principal,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + principal.role()))
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    private ResponseEntity<Void> redirectToFrontend(String path) {
        return ResponseEntity.status(302)
                .location(URI.create(frontendUrl + path))
                .build();
    }
}
