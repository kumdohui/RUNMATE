package com.runmate.server.auth.domain;

import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
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
@Table(name = "email_verifications")
public class EmailVerification {
   @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "verification_id")
    private Long verificationId;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "code_hash", nullable = false, length = 255)
    private String codeHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false)
    private EmailVerificationPurpose purpose;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; 

    public static EmailVerification createForSignup(
            String email,
            String codeHash,
            LocalDateTime expiresAt
    ) {
        EmailVerification verification = new EmailVerification();
        verification.email = email;
        verification.codeHash = codeHash;
        verification.purpose = EmailVerificationPurpose.SIGNUP;
        verification.expiresAt = expiresAt;
        return verification;
    }

    public boolean canAttempt(LocalDateTime now) {
        return consumedAt == null
                && verifiedAt == null
                && expiresAt.isAfter(now)
                && attemptCount < 5;
    }

    public void recordFailedAttempt() {
        attemptCount++;
    }

    public void verify(LocalDateTime now) {
        verifiedAt = now;
    }

    public boolean isUsableForSignup(LocalDateTime now){
        return purpose == EmailVerificationPurpose.SIGNUP
                && verifiedAt != null
                && consumedAt == null
                && verifiedAt.plusMinutes(30).isAfter(now);
    }
    public void consume(LocalDateTime now){
        this.consumedAt = now;
    }
}
