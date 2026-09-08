package com.runmate.server.auth.dto;

public record EmailVerificationConfirmResponse(
        Long verificationId,
        boolean verified
) {
}
