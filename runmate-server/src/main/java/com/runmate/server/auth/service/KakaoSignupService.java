package com.runmate.server.auth.service;

import org.springframework.stereotype.Service;

import com.runmate.server.auth.dto.AuthDtos.KakaoSignupRequest;
import com.runmate.server.auth.dto.KakaoPendingProfile;
import com.runmate.server.auth.security.RunmatePrincipal;
import com.runmate.server.user.domain.SocialProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KakaoSignupService {
    private final SocialSignupService socialSignupService;

    public RunmatePrincipal signup(
            KakaoPendingProfile profile,
            KakaoSignupRequest request,
            String ipAddress,
            String userAgent
    ) {
        return socialSignupService.signup(
                SocialProvider.KAKAO,
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
