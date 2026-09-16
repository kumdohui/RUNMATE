package com.runmate.server.user.dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class UserProfileDtos {
    private UserProfileDtos() {}

    public record ProfileResponse(Long userId, String loginId, String email, String phoneNumber,
            String nickname, String bio, String profileImageUrl, boolean localLoginRegistered,
            List<String> socialProviders, LocalDateTime createdAt) {}

    public record UpdateProfileRequest(
            @NotBlank(message = "닉네임을 입력해 주세요.")
            @Size(min = 2, max = 20, message = "닉네임은 2~20자로 입력해 주세요.") String nickname,
            @Size(max = 300, message = "소개는 300자 이하로 입력해 주세요.") String bio) {}

    public record ChangePasswordRequest(
            @NotBlank(message = "현재 비밀번호를 입력해 주세요.") String currentPassword,
            @NotBlank(message = "새 비밀번호를 입력해 주세요.")
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9])[!-~]{8,20}$",
                    message = "새 비밀번호는 영문자, 숫자, 특수문자를 포함한 8~20자로 입력해 주세요.")
            String newPassword) {}

    public record WithdrawRequest(
            String currentPassword,
            @NotBlank(message = "탈퇴 확인 문구를 입력해 주세요.") String confirmation) {}
}
