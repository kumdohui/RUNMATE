package com.runmate.server.user.domain;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "login_id", length = 50, unique = true)
    private String loginId;

    @Column(name = "email", length = 255, unique = true)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "phone_number", length = 20, unique = true)
    private String phoneNumber;

    @Column(name = "nickname", nullable = false, length = 50, unique = true)
    private String nickname;

    @Column(name = "profile_image_url", length = 1000)
    private String profileImageUrl;

    @Column(name = "profile_image_storage_key", length = 255)
    private String profileImageStorageKey;

    @Column(name = "profile_image_content_type", length = 100)
    private String profileImageContentType;

    @Column(name = "profile_image_file_size")
    private Long profileImageFileSize;

    @Column(name = "bio", length = 300)
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(name = "phone_verified_at")
    private LocalDateTime phoneVerifiedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "login_fail_count", nullable = false)
    private Integer loginFailCount = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
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
