package com.runmate.server.auth.dto;

public record PhoneVerificationIssueResponse(
        Long verificationId,
        String developmentCode,
        int expiresInSeconds
) {
}
