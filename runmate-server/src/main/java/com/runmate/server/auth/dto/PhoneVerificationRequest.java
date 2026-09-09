package com.runmate.server.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PhoneVerificationRequest(
        @NotBlank(message = "휴대전화 번호는 필수입니다.")
        @Pattern(
                regexp = "^01[016789][0-9]{7,8}$",
                message = "휴대전화 번호를 확인해 주세요."
        )
        String phoneNumber,
        Boolean accountLinkingAllowed
) {
}
