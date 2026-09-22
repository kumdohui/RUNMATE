package com.runmate.server.user.domain;

import java.time.LocalDateTime;


import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    private Long userId;

    private String loginId;

    private String email;

    private String passwordHash;

    private String phoneNumber;

    private String nickname;

    private String profileImageUrl;

    private String profileImageStorageKey;

    private String profileImageContentType;

    private Long profileImageFileSize;

    private String bio;

    private UserRole role = UserRole.USER;

    private UserStatus status = UserStatus.ACTIVE;

    private LocalDateTime emailVerifiedAt;

    private LocalDateTime phoneVerifiedAt;

    private LocalDateTime lastLoginAt;

    private Integer loginFailCount = 0;

    private LocalDateTime lockedUntil;

    private LocalDateTime passwordChangedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    public static User create(String loginId, String passwordHash, String email, String phoneNumber, String nickname, LocalDateTime emailVerifiedAt, LocalDateTime phoneVerifiedAt) {
        User user = new User();

        user.loginId = loginId;
        user.passwordHash = passwordHash;
        user.email = email;
        user.phoneNumber = phoneNumber;
        user.nickname = nickname;
        user.emailVerifiedAt = emailVerifiedAt;
        user.phoneVerifiedAt = phoneVerifiedAt;
        user.passwordChangedAt = LocalDateTime.now();
        
        return user;
    }

    public static User createSocial(
            String phoneNumber,
            String nickname,
            LocalDateTime phoneVerifiedAt
    ) {
        User user = new User();
        user.phoneNumber = phoneNumber;
        user.nickname = nickname;
        user.phoneVerifiedAt = phoneVerifiedAt;
        return user;
    }

    public void addLocalLogin(
            String loginId,
            String passwordHash,
            String email,
            LocalDateTime emailVerifiedAt
    ) {
        if (this.loginId != null || this.passwordHash != null) {
            throw new IllegalStateException("이미 일반 로그인이 등록된 회원입니다.");
        }
        this.loginId = loginId;
        this.passwordHash = passwordHash;
        this.email = email;
        this.emailVerifiedAt = emailVerifiedAt;
        this.passwordChangedAt = LocalDateTime.now();
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public void changeBio(String bio) {
        this.bio = bio;
    }

    public void changeProfileImage(
            String imageUrl,
            String storageKey,
            String contentType,
            long fileSize
    ) {
        this.profileImageUrl = imageUrl;
        this.profileImageStorageKey = storageKey;
        this.profileImageContentType = contentType;
        this.profileImageFileSize = fileSize;
    }

    public void removeProfileImage() {
        this.profileImageUrl = null;
        this.profileImageStorageKey = null;
        this.profileImageContentType = null;
        this.profileImageFileSize = null;
    }

    public boolean isLocked(LocalDateTime now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public void recordLoginFailure(LocalDateTime now, int maxAttempts, int lockMinutes) {
        loginFailCount = (loginFailCount == null ? 0 : loginFailCount) + 1;
        if (loginFailCount >= maxAttempts) {
            lockedUntil = now.plusMinutes(lockMinutes);
        }
    }

    public void recordLoginSuccess(LocalDateTime now) {
        loginFailCount = 0;
        lockedUntil = null;
        lastLoginAt = now;
    }

    public void resetPassword(String passwordHash, LocalDateTime changedAt) {
        this.passwordHash = passwordHash;
        this.passwordChangedAt = changedAt;
        this.loginFailCount = 0;
        this.lockedUntil = null;
    }

    public void anonymizeAndWithdraw(LocalDateTime withdrawnAt) {
        this.loginId = null;
        this.email = null;
        this.passwordHash = null;
        this.phoneNumber = null;
        this.nickname = "탈퇴한 사용자_" + this.userId;
        this.bio = null;
        removeProfileImage();
        this.status = UserStatus.DELETED;
        this.deletedAt = withdrawnAt;
        this.emailVerifiedAt = null;
        this.phoneVerifiedAt = null;
        this.lastLoginAt = null;
        this.passwordChangedAt = null;
        this.loginFailCount = 0;
        this.lockedUntil = null;
    }
}
