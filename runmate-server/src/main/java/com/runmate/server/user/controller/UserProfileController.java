package com.runmate.server.user.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.auth.security.RunmatePrincipal;
import com.runmate.server.user.dto.UserProfileDtos.ProfileResponse;
import com.runmate.server.user.dto.UserProfileDtos.ChangePasswordRequest;
import com.runmate.server.user.dto.UserProfileDtos.WithdrawRequest;
import com.runmate.server.user.dto.UserProfileDtos.UpdateProfileRequest;
import com.runmate.server.user.service.UserProfileService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserProfileController {
    private final UserProfileService userProfileService;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @GetMapping
    public ProfileResponse get(Authentication authentication) {
        return userProfileService.get(principal(authentication).userId());
    }

    @PatchMapping
    public ProfileResponse update(@Valid @RequestBody UpdateProfileRequest body,
            Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        RunmatePrincipal principal = principal(authentication);
        ProfileResponse result = userProfileService.update(principal.userId(), body);
        replacePrincipal(principal.withNickname(result.nickname()), request, response);
        return result;
    }

    @PatchMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest body,
            Authentication authentication, HttpServletRequest request) {
        RunmatePrincipal principal = principal(authentication);
        userProfileService.changePassword(principal.userId(), body);
        if (request.getSession(false) != null) request.getSession(false).invalidate();
        SecurityContextHolder.clearContext();
    }

    @DeleteMapping("/social-accounts/{provider}")
    public ProfileResponse disconnectSocialAccount(@PathVariable String provider,
            Authentication authentication) {
        return userProfileService.disconnectSocialAccount(principal(authentication).userId(), provider);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@Valid @RequestBody WithdrawRequest body,
            Authentication authentication, HttpServletRequest request) {
        userProfileService.withdraw(principal(authentication).userId(), body);
        if (request.getSession(false) != null) request.getSession(false).invalidate();
        SecurityContextHolder.clearContext();
    }

    private RunmatePrincipal principal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof RunmatePrincipal principal))
            throw new AuthException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        return principal;
    }

    private void replacePrincipal(RunmatePrincipal principal, HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + principal.role())));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
