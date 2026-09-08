package com.runmate.server.auth.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;

public record KakaoSignupRequest(
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 20, message = "닉네임은 2~20자여야 합니다.")
        String nickname,

        @NotNull(message = "휴대전화 인증 정보는 필수입니다.")
        @Positive(message = "휴대전화 인증 번호를 확인해 주세요.")
        Long phoneVerificationId,

        @NotEmpty(message = "필수 약관 동의 정보가 필요합니다.")
        List<@NotNull @Positive Long> termIds,

        @AssertTrue(message = "만 14세 이상만 가입할 수 있습니다.")
        boolean ageConfirmed
) {
}
