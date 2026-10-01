package com.runmate.server.auth.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.runmate.server.auth.dto.AuthDtos.CsrfTokenResponse;
import com.runmate.server.auth.dto.AuthDtos.LoginRequest;
import com.runmate.server.auth.dto.AuthDtos.LoginAttemptResponse;
import com.runmate.server.auth.dto.AuthDtos.LoginResponse;
import com.runmate.server.auth.dto.AuthDtos.AdminMfaRequest;
import com.runmate.server.auth.security.RunmatePrincipal;
import com.runmate.server.auth.service.LoginService;
import com.runmate.server.auth.service.LoginService.LoginCandidate;
import com.runmate.server.auth.service.AdminMfaService;
import com.runmate.server.auth.service.LoginRateLimitService;
import com.runmate.server.auth.exception.AuthException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class LoginController {
    private static final String PENDING_ADMIN_ID = "PENDING_ADMIN_ID";
    private static final String PENDING_MFA_ID = "PENDING_ADMIN_MFA_ID";
    private final LoginService loginService;
    private final AdminMfaService adminMfaService;
    private final LoginRateLimitService loginRateLimitService;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @PostMapping("/login")
    public LoginAttemptResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        String ipAddress = servletRequest.getRemoteAddr();
        loginRateLimitService.requireAllowed(ipAddress);
        LoginCandidate candidate;
        try {
            candidate = loginService.authenticate(request);
        } catch (AuthException exception) {
            if (exception.getStatus() == org.springframework.http.HttpStatus.UNAUTHORIZED) {
                loginRateLimitService.recordFailure(ipAddress);
            }
            throw exception;
        }
        loginRateLimitService.clear(ipAddress);
        if ("ADMIN".equals(candidate.principal().role())) {
            String challengeId = adminMfaService.issue(
                    candidate.principal().userId(),
                    candidate.email()
            );
            var session = servletRequest.getSession(true);
            session.setAttribute(PENDING_ADMIN_ID, candidate.principal().userId());
            session.setAttribute(PENDING_MFA_ID, challengeId);
            session.setMaxInactiveInterval(AdminMfaService.EXPIRES_IN_SECONDS);
            return LoginAttemptResponse.mfaRequired(
                    challengeId,
                    AdminMfaService.EXPIRES_IN_SECONDS
            );
        }
        RunmatePrincipal principal = loginService.completeLogin(candidate.principal().userId());
        authenticate(principal, servletRequest, servletResponse);
        return LoginAttemptResponse.authenticated(toResponse(principal));
    }

    @PostMapping("/login/admin-mfa")
    public LoginResponse confirmAdminMfa(
            @Valid @RequestBody AdminMfaRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        var session = servletRequest.getSession(false);
        if (session == null
                || !(session.getAttribute(PENDING_ADMIN_ID) instanceof Long userId)
                || !(session.getAttribute(PENDING_MFA_ID) instanceof String challengeId)) {
            throw new AuthException("관리자 인증이 만료되었습니다. 다시 로그인해 주세요.");
        }
        adminMfaService.verify(challengeId, userId, request.code());
        RunmatePrincipal principal = loginService.completeLogin(userId);
        session.removeAttribute(PENDING_ADMIN_ID);
        session.removeAttribute(PENDING_MFA_ID);
        authenticate(principal, servletRequest, servletResponse);
        return toResponse(principal);
    }

    private void authenticate(
            RunmatePrincipal principal,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + principal.role())));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, servletRequest, servletResponse);
        servletRequest.getSession().setMaxInactiveInterval(
                "ADMIN".equals(principal.role()) ? 15 * 60 : 30 * 60
        );
    }

    @GetMapping("/session")
    public ResponseEntity<LoginResponse> session(
            Authentication authentication,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof RunmatePrincipal principal)) {
            return ResponseEntity.noContent().build();
        }
        RunmatePrincipal refreshed = loginService.refresh(principal);
        if (refreshed == null) {
            if (servletRequest.getSession(false) != null) {
                servletRequest.getSession(false).invalidate();
            }
            SecurityContextHolder.clearContext();
            return ResponseEntity.noContent().build();
        }
        Authentication next = UsernamePasswordAuthenticationToken.authenticated(
                refreshed, null, List.of(new SimpleGrantedAuthority("ROLE_" + refreshed.role())));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(next);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, servletRequest, servletResponse);
        servletRequest.getSession().setMaxInactiveInterval(
                "ADMIN".equals(refreshed.role()) ? 15 * 60 : 30 * 60
        );
        return ResponseEntity.ok(toResponse(refreshed));
    }

    @GetMapping("/csrf")
    public CsrfTokenResponse csrf(CsrfToken csrfToken) {
        return new CsrfTokenResponse(csrfToken.getHeaderName(), csrfToken.getToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    private LoginResponse toResponse(RunmatePrincipal principal) {
        return new LoginResponse(principal.userId(), principal.loginId(), principal.nickname(), principal.role(), principal.profileImageUrl());
    }
}
