package com.runmate.server.auth.domain;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhoneVerification {
    private Long verificationId;

    private String phoneNumber;

    private String codeHash;

    private PhoneVerificationPurpose purpose;

    private Integer attemptCount = 0;

    private LocalDateTime expiresAt;

    private LocalDateTime verifiedAt;

    private LocalDateTime consumedAt;

    private LocalDateTime createdAt;

    public static PhoneVerification create(
            String phoneNumber,
            String codeHash,
            PhoneVerificationPurpose purpose,
            LocalDateTime expiresAt
    ) {
        PhoneVerification verification = new PhoneVerification();
        verification.phoneNumber = phoneNumber;
        verification.codeHash = codeHash;
        verification.purpose = purpose;
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

    public boolean isUsableForSignup(LocalDateTime now) {
        return purpose == PhoneVerificationPurpose.SIGNUP
                && verifiedAt != null
                && consumedAt == null
                && verifiedAt.plusMinutes(30).isAfter(now);
    }

    public boolean isUsableForLoginIdRecovery(LocalDateTime now) {
        return purpose == PhoneVerificationPurpose.FIND_LOGIN_ID
                && verifiedAt != null
                && consumedAt == null
                && verifiedAt.plusMinutes(30).isAfter(now);
    }

    public void consume(LocalDateTime now) {
        this.consumedAt = now;
    }
}
