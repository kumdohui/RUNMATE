package com.runmate.server.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EmailVerificationConfirmRequest(
        @NotBlank(message = "인증번호를 입력해 주세요.")
        @Pattern(regexp = "^\\d{6}$", message = "인증번호는 6자리 숫자입니다.")
        String code
) {
}
