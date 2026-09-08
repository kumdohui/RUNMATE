package com.runmate.server.auth.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.runmate.server.auth.domain.PhoneVerification;
import com.runmate.server.auth.domain.PhoneVerificationPurpose;
import com.runmate.server.auth.dto.PhoneVerificationConfirmResponse;
import com.runmate.server.auth.dto.PhoneVerificationIssueResponse;
import com.runmate.server.auth.exception.SignupException;
import com.runmate.server.auth.repository.PhoneVerificationRepository;
import com.runmate.server.user.domain.User;
import com.runmate.server.user.repository.SocialAccountRepository;
import com.runmate.server.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PhoneVerificationService {

    private static final int EXPIRES_IN_SECONDS = 300;
    private static final int RESEND_INTERVAL_SECONDS = 60;
    private static final int HOURLY_ISSUE_LIMIT = 5;
    private static final int DAILY_ISSUE_LIMIT = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PhoneVerificationRepository phoneVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;

    @Value("${verification.development-mode:false}")
    private boolean developmentMode;

    @Transactional
    public PhoneVerificationIssueResponse issue(String rawPhoneNumber) {
        if (!developmentMode) {
            throw new SignupException("문자 발송 서비스가 설정되지 않았습니다.");
        }

        String phoneNumber = rawPhoneNumber.replace("-", "");
        LocalDateTime now = LocalDateTime.now();
        validateIssueRateLimit(phoneNumber, now);

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        PhoneVerification verification = PhoneVerification.createForSignup(
                phoneNumber,
                passwordEncoder.encode(code),
                now.plusSeconds(EXPIRES_IN_SECONDS)
        );
        PhoneVerification saved = phoneVerificationRepository.save(verification);

        return new PhoneVerificationIssueResponse(
                saved.getVerificationId(),
                code,
                EXPIRES_IN_SECONDS
        );
    }

    private void validateIssueRateLimit(String phoneNumber, LocalDateTime now) {
        phoneVerificationRepository
                .findTopByPhoneNumberAndPurposeOrderByCreatedAtDesc(
                        phoneNumber,
                        PhoneVerificationPurpose.SIGNUP
                )
                .ifPresent(latest -> {
                    LocalDateTime availableAt = latest.getCreatedAt()
                            .plusSeconds(RESEND_INTERVAL_SECONDS);
                    if (availableAt.isAfter(now)) {
                        long remainingSeconds = Math.max(
                                1,
                                Duration.between(now, availableAt).toSeconds() + 1
                        );
                        throw new SignupException(
                                "인증번호는 " + remainingSeconds + "초 후 다시 받을 수 있습니다."
                        );
                    }
                });

        long hourlyCount = phoneVerificationRepository
                .countByPhoneNumberAndPurposeAndCreatedAtGreaterThanEqual(
                        phoneNumber,
                        PhoneVerificationPurpose.SIGNUP,
                        now.minusHours(1)
                );
        if (hourlyCount >= HOURLY_ISSUE_LIMIT) {
            throw new SignupException(
                    "인증번호는 최근 1시간 동안 최대 5회까지 받을 수 있습니다."
            );
        }

        long dailyCount = phoneVerificationRepository
                .countByPhoneNumberAndPurposeAndCreatedAtGreaterThanEqual(
                        phoneNumber,
                        PhoneVerificationPurpose.SIGNUP,
                        LocalDate.now().atStartOfDay()
                );
        if (dailyCount >= DAILY_ISSUE_LIMIT) {
            throw new SignupException(
                    "인증번호는 하루 최대 10회까지 받을 수 있습니다."
            );
        }
    }

    @Transactional(noRollbackFor = SignupException.class)
    public PhoneVerificationConfirmResponse confirm(Long verificationId, String code) {
        PhoneVerification verification = phoneVerificationRepository.findById(verificationId)
                .orElseThrow(() -> new SignupException("휴대전화 인증 정보를 찾을 수 없습니다."));

        LocalDateTime now = LocalDateTime.now();
        if (verification.getConsumedAt() != null) {
            throw new SignupException("이미 사용된 휴대전화 인증입니다.");
        }
        if (verification.getVerifiedAt() != null) {
            return accountAwareResponse(verification);
        }
        if (!verification.getExpiresAt().isAfter(now)) {
            throw new SignupException("인증 시간이 만료되었습니다. 인증번호를 다시 받아 주세요.");
        }
        if (!verification.canAttempt(now)) {
            throw new SignupException("인증번호 입력 횟수를 초과했습니다. 인증번호를 다시 받아 주세요.");
        }
        if (!passwordEncoder.matches(code, verification.getCodeHash())) {
            verification.recordFailedAttempt();
            throw new SignupException("인증번호가 올바르지 않습니다.");
        }

        verification.verify(now);
        return accountAwareResponse(verification);
    }

    private PhoneVerificationConfirmResponse accountAwareResponse(
            PhoneVerification verification
    ) {
        User user = userRepository.findByPhoneNumber(verification.getPhoneNumber())
                .orElse(null);
        if (user == null) {
            return new PhoneVerificationConfirmResponse(
                    verification.getVerificationId(), true, false, false, java.util.List.of()
            );
        }

        java.util.List<String> providers = socialAccountRepository.findByUser(user)
                .stream()
                .map(account -> account.getProvider().name())
                .toList();
        boolean localLoginRegistered = user.getLoginId() != null
                && user.getPasswordHash() != null;
        return new PhoneVerificationConfirmResponse(
                verification.getVerificationId(),
                true,
                true,
                localLoginRegistered,
                providers
        );
    }
}
