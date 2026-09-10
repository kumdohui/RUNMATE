package com.runmate.server.auth.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;



public record SignupRequest(

        @NotBlank(message = "아이디는 필수입니다.")
        @Pattern(
                regexp = "^[a-z0-9]{4,20}$",
                message = "아이디는 4~20자의 영문 소문자와 숫자만 사용할 수 있습니다."
        )
        String loginId,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*\\p{Punct})[A-Za-z\\d\\p{Punct}]{8,20}$",
                message = "비밀번호는 8~20자이며 영문자, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다."
        )
        String password,

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(
                min = 2,
                max = 20,
                message = "닉네임은 2~20자여야 합니다."
        )
        String nickname,

        @NotNull(message = "이메일 인증 정보는 필수입니다.")
        @Positive(message = "이메일 인증 정보를 확인해 주세요.")
        Long emailVerificationId,

        @NotNull(message = "전화번호 인증 정보는 필수입니다.")
        @Positive(message = "전화번호 인증 정보를 확인해 주세요.")
        Long phoneVerificationId,

        @NotEmpty(message = "필수 약관 동의 정보가 필요합니다.")
        List<
                @NotNull(message = "약관 번호는 필수입니다.")
                @Positive(message = "약관 동의 정보를 확인해 주세요.")
                Long
        > termIds,

        @AssertTrue(message = "만 14세 이상만 가입할 수 있습니다.")
        boolean ageConfirmed,

        boolean linkExistingAccount
) {
}
