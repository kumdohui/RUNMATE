package com.runmate.server.auth.service;

import org.springframework.stereotype.Service;

import com.runmate.server.auth.dto.AuthDtos.NaverSignupRequest;
import com.runmate.server.auth.dto.NaverPendingProfile;
import com.runmate.server.auth.security.RunmatePrincipal;
import com.runmate.server.user.domain.SocialProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NaverSignupService {
    private final SocialSignupService socialSignupService;

    public RunmatePrincipal signup(
            NaverPendingProfile profile,
            NaverSignupRequest request,
            String ipAddress,
            String userAgent
    ) {
        return socialSignupService.signup(
                SocialProvider.NAVER,
                profile.providerUserId(),
                profile.profileImageUrl(),
                request.nickname(),
                request.phoneVerificationId(),
                request.termConsents(),
                ipAddress,
                userAgent
        );
    }
}
