package com.runmate.server.auth.service;

import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.runmate.server.auth.exception.AuthException;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminMfaService {
    public static final int EXPIRES_IN_SECONDS = 300;
    private static final int MAX_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mailUsername;

    public String issue(Long userId, String email) {
        if (email == null || email.isBlank()) {
            throw new AuthException("관리자 계정에 등록된 이메일이 없습니다.");
        }
        String challengeId = UUID.randomUUID().toString();
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        Duration expiry = Duration.ofSeconds(EXPIRES_IN_SECONDS);
        redisTemplate.opsForValue().set(userKey(challengeId), userId.toString(), expiry);
        redisTemplate.opsForValue().set(codeKey(challengeId), passwordEncoder.encode(code), expiry);
        redisTemplate.opsForValue().set(attemptKey(challengeId), "0", expiry);
        send(email, code);
        return challengeId;
    }

    public void verify(String challengeId, Long expectedUserId, String code) {
        String storedUserId = redisTemplate.opsForValue().get(userKey(challengeId));
        String codeHash = redisTemplate.opsForValue().get(codeKey(challengeId));
        if (storedUserId == null || codeHash == null
                || !storedUserId.equals(expectedUserId.toString())) {
            throw new AuthException("관리자 인증이 만료되었습니다. 다시 로그인해 주세요.");
        }
        long attempts = incrementAttempts(challengeId);
        if (attempts > MAX_ATTEMPTS) {
            delete(challengeId);
            throw new AuthException("인증번호 입력 횟수를 초과했습니다. 다시 로그인해 주세요.");
        }
        if (!passwordEncoder.matches(code, codeHash)) {
            throw new AuthException("인증번호가 올바르지 않습니다.");
        }
        delete(challengeId);
    }

    private long incrementAttempts(String challengeId) {
        Long attempts = redisTemplate.opsForValue().increment(attemptKey(challengeId));
        return attempts == null ? MAX_ATTEMPTS + 1 : attempts;
    }

    private void delete(String challengeId) {
        redisTemplate.delete(java.util.List.of(
                userKey(challengeId),
                codeKey(challengeId),
                attemptKey(challengeId)
        ));
    }

    private void send(String email, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(mailUsername, "RUNMATE");
            helper.setTo(email);
            helper.setSubject("[RUNMATE] 관리자 로그인 인증번호");
            helper.setText(
                    "관리자 로그인 인증번호는 <strong>" + code
                            + "</strong>입니다. 5분 안에 입력해 주세요.",
                    true
            );
            mailSender.send(message);
        } catch (MailException | MessagingException | UnsupportedEncodingException exception) {
            throw new AuthException("관리자 인증 메일을 보내지 못했습니다.");
        }
    }

    private String userKey(String id) { return "security:admin-mfa:" + id + ":user"; }
    private String codeKey(String id) { return "security:admin-mfa:" + id + ":code"; }
    private String attemptKey(String id) { return "security:admin-mfa:" + id + ":attempts"; }
}
