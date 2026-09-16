package com.runmate.server.user.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.auth.security.RunmatePrincipal;
import com.runmate.server.user.dto.ProfileImageResponse;
import com.runmate.server.user.service.ProfileImageService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users/me/profile-image")
@RequiredArgsConstructor
public class ProfileImageController {

    private final ProfileImageService profileImageService;
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    @PostMapping
    public ProfileImageResponse change(
            @RequestPart("image") MultipartFile image,
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        RunmatePrincipal principal = principal(authentication);
        ProfileImageResponse result = profileImageService.change(principal.userId(), image);
        replacePrincipal(principal.withProfileImageUrl(result.profileImageUrl()), request, response);
        return result;
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        RunmatePrincipal principal = principal(authentication);
        profileImageService.delete(principal.userId());
        replacePrincipal(principal.withProfileImageUrl(null), request, response);
        return ResponseEntity.noContent().build();
    }

    private RunmatePrincipal principal(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof RunmatePrincipal principal)) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return principal;
    }

    private void replacePrincipal(
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
}
