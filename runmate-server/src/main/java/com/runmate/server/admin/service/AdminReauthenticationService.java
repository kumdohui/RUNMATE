package com.runmate.server.admin.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.user.domain.User;
import com.runmate.server.user.mapper.UserMapper;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminReauthenticationService {
    private static final String SESSION_KEY = "ADMIN_REAUTHENTICATED_AT";
    private static final Duration VALIDITY = Duration.ofMinutes(5);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public void reauthenticate(Long userId, String password, HttpSession session) {
        User user = userMapper.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "관리자 계정을 찾을 수 없습니다."));
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "비밀번호가 올바르지 않습니다.");
        }
        session.setAttribute(SESSION_KEY, Instant.now().toEpochMilli());
    }

    public void requireRecent(HttpSession session) {
        Object value = session.getAttribute(SESSION_KEY);
        if (!(value instanceof Long authenticatedAt)
                || Instant.ofEpochMilli(authenticatedAt).plus(VALIDITY).isBefore(Instant.now())) {
            throw new AuthException(
                    HttpStatus.PRECONDITION_REQUIRED,
                    "중요 작업을 계속하려면 관리자 비밀번호를 다시 확인해 주세요."
            );
        }
    }
}
