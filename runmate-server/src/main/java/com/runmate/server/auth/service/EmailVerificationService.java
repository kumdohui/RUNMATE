package com.runmate.server.auth.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.runmate.server.auth.domain.EmailVerification;
import com.runmate.server.auth.domain.EmailVerificationPurpose;
import com.runmate.server.auth.dto.AuthDtos.EmailVerificationConfirmResponse;
import com.runmate.server.auth.dto.AuthDtos.EmailVerificationIssueResponse;
import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.auth.mapper.EmailVerificationMapper;
import com.runmate.server.user.mapper.UserMapper;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private static final int EXPIRES_IN_SECONDS = 300;
    private static final int RESEND_INTERVAL_SECONDS = 60;
    private static final int HOURLY_ISSUE_LIMIT = 5;
    private static final int DAILY_ISSUE_LIMIT = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailVerificationMapper emailVerificationMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mailUsername;

    @Transactional
    public EmailVerificationIssueResponse issue(
            String rawEmail,
            EmailVerificationPurpose requestedPurpose
    ) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        EmailVerificationPurpose purpose = requestedPurpose == null
                ? EmailVerificationPurpose.SIGNUP
                : requestedPurpose;
        if (purpose != EmailVerificationPurpose.SIGNUP
                && purpose != EmailVerificationPurpose.RESET_PASSWORD) {
            throw new AuthException("지원하지 않는 이메일 인증 목적입니다.");
        }
        if (purpose == EmailVerificationPurpose.SIGNUP && userMapper.existsByEmail(email)) {
            throw new AuthException("이미 사용 중인 이메일입니다.");
        }
        boolean passwordResetRecipientExists = purpose != EmailVerificationPurpose.RESET_PASSWORD
                || userMapper.findByEmail(email)
                        .filter(user -> user.getPasswordHash() != null)
                        .isPresent();

        LocalDateTime now = LocalDateTime.now();
        validateIssueRateLimit(email, purpose, now);

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        EmailVerification verification = EmailVerification.create(
                email,
                passwordEncoder.encode(code),
                purpose,
                now.plusSeconds(EXPIRES_IN_SECONDS)
        );
        EmailVerification saved = emailVerificationMapper.save(verification);
        if (passwordResetRecipientExists) {
            sendCode(email, code, purpose);
        }

        return new EmailVerificationIssueResponse(
                saved.getVerificationId(),
                EXPIRES_IN_SECONDS
        );
    }

    private void sendCode(String email, String code, EmailVerificationPurpose purpose) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(mailUsername, "RUNMATE");
            helper.setTo(email);
            boolean passwordReset = purpose == EmailVerificationPurpose.RESET_PASSWORD;
            helper.setSubject(passwordReset
                    ? "[RUNMATE] 비밀번호 재설정 인증번호"
                    : "[RUNMATE] 회원가입 이메일 인증번호");
            helper.setText(buildMailHtml(code, passwordReset), true);
            mailSender.send(message);
        } catch (MailException | MessagingException | java.io.UnsupportedEncodingException exception) {
            if (purpose == EmailVerificationPurpose.RESET_PASSWORD) {
                log.warn("비밀번호 재설정 인증 메일을 발송하지 못했습니다: {}", email, exception);
                return;
            }
            throw new AuthException("인증 메일을 보내지 못했습니다. 메일 설정을 확인해 주세요.");
        }
    }

    private String buildMailHtml(String code, boolean passwordReset) {
        return """
                <!doctype html>
                <html lang="ko">
                <body style="margin:0;padding:0;background:#f3f7f4;font-family:Arial,'Noto Sans KR',sans-serif;color:#173427;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f3f7f4;padding:40px 16px;">
                    <tr><td align="center">
                      <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:560px;background:#ffffff;border:1px solid #dfe8e2;border-radius:20px;overflow:hidden;box-shadow:0 16px 40px rgba(23,52,39,.08);">
                        <tr><td style="height:8px;background:#1c6b48;"></td></tr>
                        <tr><td style="padding:38px 42px 16px;">
                          <div style="font-size:15px;font-weight:800;letter-spacing:.12em;color:#1c6b48;">RUNMATE</div>
                          <h1 style="margin:18px 0 12px;font-size:27px;line-height:1.35;color:#173427;">%s</h1>
                          <p style="margin:0;color:#68766f;font-size:15px;line-height:1.75;">%s</p>
                        </td></tr>
                        <tr><td style="padding:14px 42px;">
                          <div style="padding:24px;text-align:center;background:#eef6f1;border:1px solid #cfe1d5;border-radius:14px;">
                            <div style="margin-bottom:9px;color:#6c7c73;font-size:12px;font-weight:700;">이메일 인증번호</div>
                            <div style="color:#175a3c;font-size:36px;font-weight:800;letter-spacing:.18em;">%s</div>
                          </div>
                          <p style="margin:14px 0 0;text-align:center;color:#8a6358;font-size:13px;">인증번호는 발급 후 <strong>5분</strong> 동안 유효합니다.</p>
                        </td></tr>
                        <tr><td style="padding:24px 42px;background:#f8faf8;border-top:1px solid #e7ece9;">
                          <p style="margin:0;color:#8a958f;font-size:12px;line-height:1.7;">본인이 요청한 인증이 아니라면 별도의 조치 없이 이 메일을 삭제해 주세요.<br>이 인증번호는 다른 사람에게 알려주지 마세요.</p>
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(
                        passwordReset ? "비밀번호 재설정을 확인해 주세요" : "이메일 인증을 완료해 주세요",
                        passwordReset ? "비밀번호 재설정 화면에 아래 인증번호를 입력해 주세요." : "회원가입 화면에 아래 인증번호를 입력해 주세요.",
                        code
                );
    }

    private void validateIssueRateLimit(
            String email,
            EmailVerificationPurpose purpose,
            LocalDateTime now
    ) {
        emailVerificationMapper
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(
                        email,
                        purpose
                )
                .ifPresent(latest -> {
                    LocalDateTime availableAt = latest.getCreatedAt()
                            .plusSeconds(RESEND_INTERVAL_SECONDS);
                    if (availableAt.isAfter(now)) {
                        long remainingSeconds = Math.max(
                                1,
                                Duration.between(now, availableAt).toSeconds() + 1
                        );
                        throw new AuthException(
                                "인증번호는 " + remainingSeconds + "초 후 다시 받을 수 있습니다."
                        );
                    }
                });

        long hourlyCount = emailVerificationMapper
                .countByEmailAndPurposeAndCreatedAtGreaterThanEqual(
                        email,
                        purpose,
                        now.minusHours(1)
                );
        if (hourlyCount >= HOURLY_ISSUE_LIMIT) {
            throw new AuthException("인증번호는 최근 1시간 동안 최대 5회까지 받을 수 있습니다.");
        }

        long dailyCount = emailVerificationMapper
                .countByEmailAndPurposeAndCreatedAtGreaterThanEqual(
                        email,
                        purpose,
                        LocalDate.now().atStartOfDay()
                );
        if (dailyCount >= DAILY_ISSUE_LIMIT) {
            throw new AuthException("인증번호는 하루 최대 10회까지 받을 수 있습니다.");
        }
    }

    @Transactional(noRollbackFor = AuthException.class)
    public EmailVerificationConfirmResponse confirm(Long verificationId, String code) {
        EmailVerification verification = emailVerificationMapper.findById(verificationId)
                .orElseThrow(() -> new AuthException("이메일 인증 정보를 찾을 수 없습니다."));

        LocalDateTime now = LocalDateTime.now();
        if (verification.getConsumedAt() != null) {
            throw new AuthException("이미 사용된 이메일 인증입니다.");
        }
        if (verification.getVerifiedAt() != null) {
            return new EmailVerificationConfirmResponse(verificationId, true);
        }
        if (!verification.getExpiresAt().isAfter(now)) {
            throw new AuthException("인증 시간이 만료되었습니다. 인증번호를 다시 받아 주세요.");
        }
        if (!verification.canAttempt(now)) {
            throw new AuthException("인증번호 입력 횟수를 초과했습니다. 인증번호를 다시 받아 주세요.");
        }
        if (!passwordEncoder.matches(code, verification.getCodeHash())) {
            verification.recordFailedAttempt();
            throw new AuthException("인증번호가 올바르지 않습니다.");
        }

        verification.verify(now);
        return new EmailVerificationConfirmResponse(verificationId, true);
    }
}


