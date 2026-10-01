package com.runmate.server.user.domain;

import java.time.LocalDateTime;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount {

    private Long socialAccountId;

    private User user;

    private SocialProvider provider;

    private String providerUserId;

    private LocalDateTime connectedAt;

    private LocalDateTime lastLoginAt;

    public static SocialAccount create(
            User user,
            SocialProvider provider,
            String providerUserId,
            LocalDateTime now
    ) {
        SocialAccount account = new SocialAccount();
        account.user = user;
        account.provider = provider;
        account.providerUserId = providerUserId;
        account.lastLoginAt = now;
        return account;
    }

    public void recordLogin(LocalDateTime now) {
        lastLoginAt = now;
    }
}
