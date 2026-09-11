package com.runmate.server.user.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "social_accounts")
public class SocialAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "social_account_id")
    private Long socialAccountId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private SocialProvider provider;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @Column(name = "provider_email", length = 255)
    private String providerEmail;

    @Column(name = "connected_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime connectedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    public static SocialAccount createKakao(
            User user,
            String providerUserId,
            LocalDateTime now
    ) {
        SocialAccount account = new SocialAccount();
        account.user = user;
        account.provider = SocialProvider.KAKAO;
        account.providerUserId = providerUserId;
        account.lastLoginAt = now;
        return account;
    }

    public static SocialAccount createNaver(
            User user,
            String providerUserId,
            LocalDateTime now
    ) {
        SocialAccount account = new SocialAccount();
        account.user = user;
        account.provider = SocialProvider.NAVER;
        account.providerUserId = providerUserId;
        account.lastLoginAt = now;
        return account;
    }

    public void recordLogin(LocalDateTime now) {
        lastLoginAt = now;
    }
}
