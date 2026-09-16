package com.runmate.server.auth.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.runmate.server.auth.domain.EmailVerificationPurpose;
import com.runmate.server.auth.domain.PhoneVerification;
import com.runmate.server.auth.dto.AuthDtos.EmailVerificationIssueResponse;
import com.runmate.server.auth.dto.AuthDtos.PasswordRecoveryResponse;
import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.auth.repository.PhoneVerificationRepository;
import com.runmate.server.user.domain.User;
import com.runmate.server.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasswordRecoveryService {
    private final UserRepository userRepository;
    private final PhoneVerificationRepository phoneVerificationRepository;
    private final EmailVerificationService emailVerificationService;

    @Transactional
    public PasswordRecoveryResponse startByLoginId(String rawLoginId) {
        User user = userRepository.findByLoginId(rawLoginId.trim())
                .filter(found -> found.getPasswordHash() != null && found.getEmail() != null)
                .orElseThrow(() -> new AuthException("입력한 아이디로 일반 로그인 계정을 찾을 수 없습니다."));
        return issueFor(user);
    }

    @Transactional
    public PasswordRecoveryResponse startFromPhoneVerification(Long phoneVerificationId) {
        LocalDateTime now = LocalDateTime.now();
        PhoneVerification verification = phoneVerificationRepository.findById(phoneVerificationId)
                .orElseThrow(() -> new AuthException("휴대전화 인증 정보를 찾을 수 없습니다."));
        if (!verification.isUsableForLoginIdRecovery(now)) {
            throw new AuthException("휴대전화 인증이 만료되었거나 이미 사용되었습니다.");
        }
        User user = userRepository.findByPhoneNumber(verification.getPhoneNumber())
                .filter(found -> found.getPasswordHash() != null && found.getEmail() != null)
                .orElseThrow(() -> new AuthException("비밀번호를 재설정할 일반 로그인 계정이 없습니다."));
        verification.consume(now);
        return issueFor(user);
    }

    private PasswordRecoveryResponse issueFor(User user) {
        EmailVerificationIssueResponse issued = emailVerificationService.issue(
                user.getEmail(),
                EmailVerificationPurpose.RESET_PASSWORD
        );
        return new PasswordRecoveryResponse(issued.verificationId(), maskEmail(user.getEmail()));
    }

    private String maskEmail(String email) {
        String[] parts = email.split("@", 2);
        String local = parts[0];
        int visibleLength = Math.min(3, Math.max(1, local.length() / 2));
        return local.substring(0, visibleLength)
                + "*".repeat(Math.max(3, local.length() - visibleLength))
                + "@" + parts[1];
    }
}
