package com.runmate.server.auth.dto;

import com.runmate.server.auth.security.RunmatePrincipal;

public record KakaoLoginResult(
        RunmatePrincipal principal,
        KakaoPendingProfile pendingProfile
) {
    public static KakaoLoginResult authenticated(RunmatePrincipal principal) {
        return new KakaoLoginResult(principal, null);
    }

    public static KakaoLoginResult signupRequired(KakaoPendingProfile pendingProfile) {
        return new KakaoLoginResult(null, pendingProfile);
    }

    public boolean requiresSignup() {
        return pendingProfile != null;
    }
}
