package com.runmate.server.auth.domain;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification {
    private Long verificationId;

    private String email;

    private String codeHash;

    private EmailVerificationPurpose purpose;

    private Integer attemptCount = 0;

    private LocalDateTime expiresAt;

    private LocalDateTime verifiedAt;

    private LocalDateTime consumedAt;

    private LocalDateTime createdAt; 

    public static EmailVerification create(
            String email,
            String codeHash,
            EmailVerificationPurpose purpose,
            LocalDateTime expiresAt
    ) {
        EmailVerification verification = new EmailVerification();
        verification.email = email;
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

    public boolean isUsableForSignup(LocalDateTime now){
        return purpose == EmailVerificationPurpose.SIGNUP
                && verifiedAt != null
                && consumedAt == null
                && verifiedAt.plusMinutes(30).isAfter(now);
    }
    public boolean isUsableForPasswordReset(LocalDateTime now) {
        return purpose == EmailVerificationPurpose.RESET_PASSWORD
                && verifiedAt != null
                && consumedAt == null
                && verifiedAt.plusMinutes(30).isAfter(now);
    }
    public void consume(LocalDateTime now){
        this.consumedAt = now;
    }
}
