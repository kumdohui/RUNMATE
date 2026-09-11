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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.runmate.server.auth.dto.AuthDtos.LoginResponse;
import com.runmate.server.auth.dto.AuthDtos.NaverLoginResult;
import com.runmate.server.auth.dto.AuthDtos.NaverPendingProfileResponse;
import com.runmate.server.auth.dto.AuthDtos.NaverSignupRequest;
import com.runmate.server.auth.dto.NaverPendingProfile;
import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.auth.security.RunmatePrincipal;
import com.runmate.server.auth.service.NaverLoginService;
import com.runmate.server.auth.service.NaverSignupService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth/naver")
@RequiredArgsConstructor
public class NaverLoginController {
    private static final String STATE_SESSION_KEY = "NAVER_LOGIN_STATE";
    private static final String PENDING_PROFILE_SESSION_KEY = "NAVER_PENDING_PROFILE";
    private static final String AUTHORIZE_URL = "https://nid.naver.com/oauth2.0/authorize";

    @Value("${naver.client-id}")
    private String clientId;
    @Value("${naver.redirect-uri}")
    private String redirectUri;
    @Value("${naver.frontend-url}")
    private String frontendUrl;

    private final NaverLoginService naverLoginService;
    private final NaverSignupService naverSignupService;
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    @GetMapping
    public ResponseEntity<Void> startLogin(HttpSession session) {
        if (clientId.isBlank()) {
            throw new AuthException("네이버 로그인 설정이 완료되지 않았습니다.");
        }
        String state = UUID.randomUUID().toString();
        session.setAttribute(STATE_SESSION_KEY, state);
        URI authorizeUri = UriComponentsBuilder.fromUriString(AUTHORIZE_URL)
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state)
                .queryParam("auth_type", "reauthenticate")
                .build().encode().toUri();
        return ResponseEntity.status(302).location(authorizeUri).build();
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
        String savedState = session == null ? null : (String) session.getAttribute(STATE_SESSION_KEY);
        if (session != null) {
            session.removeAttribute(STATE_SESSION_KEY);
        }
        if (error != null || code == null || savedState == null || !savedState.equals(state)) {
            return redirectToFrontend("/login?naver=failed");
        }
        try {
            NaverLoginResult result = naverLoginService.login(code, state);
            if (result.requiresSignup()) {
                session.setAttribute(PENDING_PROFILE_SESSION_KEY, result.pendingProfile());
                return redirectToFrontend("/naver-signup");
            }
            authenticate(result.principal(), request, response);
            return redirectToFrontend("/?naver=success");
        } catch (AuthException exception) {
            return redirectToFrontend("/login?naver=failed");
        }
    }

    @GetMapping("/pending")
    public NaverPendingProfileResponse pending(HttpSession session) {
        NaverPendingProfile profile = pendingProfile(session);
        return new NaverPendingProfileResponse(profile.nickname(), profile.profileImageUrl());
    }

    @PostMapping("/signup")
    public LoginResponse signup(
            @Valid @RequestBody NaverSignupRequest signupRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        HttpSession session = request.getSession(false);
        NaverPendingProfile profile = pendingProfile(session);
        RunmatePrincipal principal = naverSignupService.signup(
                profile, signupRequest, request.getRemoteAddr(), request.getHeader("User-Agent"));
        session.removeAttribute(PENDING_PROFILE_SESSION_KEY);
        authenticate(principal, request, response);
        return new LoginResponse(principal.userId(), principal.loginId(), principal.nickname(),
                principal.role(), principal.profileImageUrl());
    }

    private NaverPendingProfile pendingProfile(HttpSession session) {
        if (session == null) {
            throw new AuthException("네이버 로그인 정보가 만료되었습니다.");
        }
        Object value = session.getAttribute(PENDING_PROFILE_SESSION_KEY);
        if (!(value instanceof NaverPendingProfile profile)) {
            throw new AuthException("네이버 로그인 정보가 만료되었습니다.");
        }
        return profile;
    }

    private void authenticate(RunmatePrincipal principal, HttpServletRequest request,
                              HttpServletResponse response) {
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + principal.role())));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    private ResponseEntity<Void> redirectToFrontend(String path) {
        return ResponseEntity.status(302).location(URI.create(frontendUrl + path)).build();
    }
}
