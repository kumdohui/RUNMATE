package com.runmate.server.auth.dto;

import java.util.List;

import com.runmate.server.auth.security.RunmatePrincipal;
import com.runmate.server.auth.domain.EmailVerificationPurpose;
import com.runmate.server.auth.domain.PhoneVerificationPurpose;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** 인증 API에서 사용하는 요청·응답 형식을 기능별로 모은 컨테이너입니다. */
public final class AuthDtos {
    private AuthDtos() {
    }

    public record AvailabilityResponse(boolean available) {
    }

    public record CsrfTokenResponse(String headerName, String token) {
    }

    public record LoginRequest(
            @NotBlank(message = "아이디를 입력해 주세요.") String loginId,
            @NotBlank(message = "비밀번호를 입력해 주세요.") String password
    ) {
    }

    public record LoginResponse(
            Long userId,
            String loginId,
            String nickname,
            String role,
            String profileImageUrl
    ) {
    }

    public record LoginAttemptResponse(
            boolean mfaRequired,
            String mfaChallengeId,
            Integer expiresInSeconds,
            LoginResponse user
    ) {
        public static LoginAttemptResponse authenticated(LoginResponse user) {
            return new LoginAttemptResponse(false, null, null, user);
        }

        public static LoginAttemptResponse mfaRequired(String challengeId, int expiresInSeconds) {
            return new LoginAttemptResponse(true, challengeId, expiresInSeconds, null);
        }
    }

    public record AdminMfaRequest(
            @NotBlank(message = "인증번호를 입력해 주세요.")
            @Pattern(regexp = "^\\d{6}$", message = "인증번호는 6자리 숫자입니다.") String code
    ) {
    }

    public record SignupRequest(
            @NotBlank(message = "아이디는 필수입니다.")
            @Pattern(regexp = "^[a-z0-9]{4,20}$", message = "아이디는 4~20자의 영문 소문자와 숫자만 사용할 수 있습니다.")
            String loginId,
            @NotBlank(message = "비밀번호는 필수입니다.")
            @Pattern(
                    regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*\\p{Punct})[A-Za-z\\d\\p{Punct}]{8,20}$",
                    message = "비밀번호는 8~20자이며 영문자, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다."
            )
            String password,
            @NotBlank(message = "닉네임은 필수입니다.")
            @Size(min = 2, max = 20, message = "닉네임은 2~20자여야 합니다.")
            String nickname,
            @NotNull(message = "이메일 인증 정보는 필수입니다.")
            @Positive(message = "이메일 인증 정보를 확인해 주세요.") Long emailVerificationId,
            @NotNull(message = "전화번호 인증 정보는 필수입니다.")
            @Positive(message = "전화번호 인증 정보를 확인해 주세요.") Long phoneVerificationId,
            @NotEmpty(message = "필수 약관 동의 정보가 필요합니다.")
            List<@Valid @NotNull(message = "약관 동의 정보는 필수입니다.") TermConsentRequest> termConsents,
            @AssertTrue(message = "만 14세 이상만 가입할 수 있습니다.") boolean ageConfirmed,
            boolean linkExistingAccount
    ) {
    }

    public record SignupResponse(Long userId, boolean linkedExistingAccount) {
    }

    public record TermConsentRequest(
            @NotNull(message = "약관 번호는 필수입니다.")
            @Positive(message = "약관 동의 정보를 확인해 주세요.")
            Long termId,
            @NotBlank(message = "약관 내용 확인값은 필수입니다.")
            @Pattern(regexp = "^[0-9a-fA-F]{64}$", message = "약관 내용 확인값을 확인해 주세요.")
            String contentHash
    ) {
    }

    public record EmailVerificationRequest(
            @NotBlank(message = "이메일은 필수입니다.")
            @Email(message = "이메일 주소를 확인해 주세요.") String email,
            EmailVerificationPurpose purpose
    ) {
    }

    public record PasswordResetRequest(
            @NotNull(message = "이메일 인증 정보는 필수입니다.")
            @Positive(message = "이메일 인증 정보를 확인해 주세요.") Long emailVerificationId,
            @NotBlank(message = "새 비밀번호를 입력해 주세요.")
            @Pattern(
                    regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*\\p{Punct})[A-Za-z\\d\\p{Punct}]{8,20}$",
                    message = "비밀번호는 8~20자이며 영문자, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다."
            )
            String newPassword
    ) {
    }

    public record LoginIdRecoveryRequest(
            @NotNull(message = "휴대전화 인증 정보는 필수입니다.")
            @Positive(message = "휴대전화 인증 정보를 확인해 주세요.") Long phoneVerificationId
    ) {
    }

    public record LoginIdRecoveryResponse(
            boolean accountFound,
            String maskedLoginId,
            String maskedEmail,
            List<String> socialProviders
    ) {
    }

    public record PasswordRecoveryRequest(
            @NotBlank(message = "아이디를 입력해 주세요.") String loginId
    ) {
    }

    public record PasswordRecoveryFromPhoneRequest(
            @NotNull(message = "휴대전화 인증 정보는 필수입니다.")
            @Positive(message = "휴대전화 인증 정보를 확인해 주세요.") Long phoneVerificationId
    ) {
    }

    public record PasswordRecoveryResponse(
            Long emailVerificationId,
            String maskedEmail
    ) {
    }

    public record EmailVerificationConfirmRequest(
            @NotBlank(message = "인증번호를 입력해 주세요.")
            @Pattern(regexp = "^\\d{6}$", message = "인증번호는 6자리 숫자입니다.") String code
    ) {
    }

    public record EmailVerificationIssueResponse(Long verificationId, int expiresInSeconds) {
    }

    public record EmailVerificationConfirmResponse(Long verificationId, boolean verified) {
    }

    public record PhoneVerificationRequest(
            @NotBlank(message = "휴대전화 번호는 필수입니다.")
            @Pattern(regexp = "^01[016789][0-9]{7,8}$", message = "휴대전화 번호를 확인해 주세요.")
            String phoneNumber,
            Boolean accountLinkingAllowed,
            PhoneVerificationPurpose purpose
    ) {
    }

    public record PhoneVerificationConfirmRequest(
            @NotBlank(message = "인증번호는 필수입니다.")
            @Pattern(regexp = "^[0-9]{6}$", message = "인증번호 6자리를 입력해 주세요.") String code
    ) {
    }

    public record PhoneVerificationIssueResponse(
            Long verificationId,
            String developmentCode,
            int expiresInSeconds
    ) {
    }

    public record PhoneVerificationConfirmResponse(
            Long verificationId,
            boolean verified,
            boolean existingAccount,
            boolean localLoginRegistered,
            List<String> socialProviders,
            String existingNickname
    ) {
    }

    public record KakaoSignupRequest(
            String nickname,
            @NotNull(message = "휴대전화 인증 정보는 필수입니다.")
            @Positive(message = "휴대전화 인증 번호를 확인해 주세요.") Long phoneVerificationId,
            @NotEmpty(message = "필수 약관 동의 정보가 필요합니다.")
            List<@Valid @NotNull TermConsentRequest> termConsents,
            @AssertTrue(message = "만 14세 이상만 가입할 수 있습니다.") boolean ageConfirmed
    ) {
    }

    public record KakaoPendingProfileResponse(String nickname, String profileImageUrl) {
    }

    public record KakaoLoginResult(
            RunmatePrincipal principal,
            com.runmate.server.auth.dto.KakaoPendingProfile pendingProfile
    ) {
        public static KakaoLoginResult authenticated(RunmatePrincipal principal) {
            return new KakaoLoginResult(principal, null);
        }

        public static KakaoLoginResult signupRequired(
                com.runmate.server.auth.dto.KakaoPendingProfile pendingProfile
        ) {
            return new KakaoLoginResult(null, pendingProfile);
        }

        public boolean requiresSignup() {
            return pendingProfile != null;
        }
    }

    public record NaverSignupRequest(
            String nickname,
            @NotNull(message = "휴대전화 인증 정보는 필수입니다.")
            @Positive(message = "휴대전화 인증 번호를 확인해 주세요.") Long phoneVerificationId,
            @NotEmpty(message = "필수 약관 동의 정보가 필요합니다.")
            List<@Valid @NotNull TermConsentRequest> termConsents,
            @AssertTrue(message = "만 14세 이상만 가입할 수 있습니다.") boolean ageConfirmed
    ) {
    }

    public record NaverPendingProfileResponse(String nickname, String profileImageUrl) {
    }

    public record NaverLoginResult(
            RunmatePrincipal principal,
            com.runmate.server.auth.dto.NaverPendingProfile pendingProfile
    ) {
        public static NaverLoginResult authenticated(RunmatePrincipal principal) {
            return new NaverLoginResult(principal, null);
        }

        public static NaverLoginResult signupRequired(
                com.runmate.server.auth.dto.NaverPendingProfile pendingProfile
        ) {
            return new NaverLoginResult(null, pendingProfile);
        }

        public boolean requiresSignup() {
            return pendingProfile != null;
        }
    }
}
