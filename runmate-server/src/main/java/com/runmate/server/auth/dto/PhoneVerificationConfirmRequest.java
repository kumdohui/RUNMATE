package com.runmate.server.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PhoneVerificationConfirmRequest(
        @NotBlank(message = "인증번호는 필수입니다.")
        @Pattern(regexp = "^[0-9]{6}$", message = "인증번호 6자리를 입력해 주세요.")
        String code
) {
}
