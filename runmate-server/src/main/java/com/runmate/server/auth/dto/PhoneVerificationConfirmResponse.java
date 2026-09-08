package com.runmate.server.auth.dto;

import java.util.List;

public record PhoneVerificationConfirmResponse(
        Long verificationId,
        boolean verified,
        boolean existingAccount,
        boolean localLoginRegistered,
        List<String> socialProviders
) {
}
