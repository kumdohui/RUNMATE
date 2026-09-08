package com.runmate.server.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailVerificationRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 주소를 확인해 주세요.")
        String email
) {
}
