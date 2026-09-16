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
import com.runmate.server.auth.dto.AuthDtos.LoginResponse;
import com.runmate.server.auth.security.RunmatePrincipal;
import com.runmate.server.auth.service.LoginService;
import com.runmate.server.user.service.ProfileImageService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class LoginController {
    private final LoginService loginService;
    private final ProfileImageService profileImageService;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        RunmatePrincipal principal = loginService.login(request);
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + principal.role())));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, servletRequest, servletResponse);
        return toResponse(principal);
    }

    @GetMapping("/session")
    public ResponseEntity<LoginResponse> session(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof RunmatePrincipal principal)) {
            return ResponseEntity.noContent().build();
        }
        RunmatePrincipal refreshedPrincipal = principal.withProfileImageUrl(
                profileImageService.getViewUrl(principal.userId(), principal.profileImageUrl())
        );
        return ResponseEntity.ok(toResponse(refreshedPrincipal));
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
