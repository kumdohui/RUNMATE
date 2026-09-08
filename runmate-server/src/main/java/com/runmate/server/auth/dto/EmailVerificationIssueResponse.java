package com.runmate.server.auth.dto;

public record EmailVerificationIssueResponse(
        Long verificationId,
        int expiresInSeconds
) {
}
