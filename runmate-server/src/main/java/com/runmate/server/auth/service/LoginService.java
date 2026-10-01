package com.runmate.server.auth.service;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.runmate.server.auth.dto.AuthDtos.LoginRequest;
import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.auth.security.RunmatePrincipal;
import com.runmate.server.user.domain.User;
import com.runmate.server.user.domain.UserStatus;
import com.runmate.server.user.mapper.UserMapper;
import com.runmate.server.user.service.ProfileImageUrlResolver;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginService {
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 10;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final ProfileImageUrlResolver profileImageUrlResolver;

    @Transactional(noRollbackFor = AuthException.class)
    public LoginCandidate authenticate(LoginRequest request) {
        LocalDateTime now = LocalDateTime.now();
        User user = userMapper.findByLoginId(request.loginId()).orElseThrow(this::invalidCredentials);

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new AuthException(HttpStatus.FORBIDDEN, "이용이 정지된 계정입니다.");
        }
        if (user.getStatus() == UserStatus.DELETED) {
            throw new AuthException(HttpStatus.FORBIDDEN, "탈퇴한 계정입니다.");
        }
        if (user.isLocked(now)) {
            throw new AuthException(HttpStatus.TOO_MANY_REQUESTS, "로그인 시도가 잠시 제한되었습니다. 잠시 후 다시 시도해 주세요.");
        }
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.recordLoginFailure(now, MAX_LOGIN_ATTEMPTS, LOCK_MINUTES);
            userMapper.update(user);
            throw invalidCredentials();
        }
        return new LoginCandidate(principalOf(user), user.getEmail());
    }

    @Transactional
    public RunmatePrincipal completeLogin(Long userId) {
        User user = userMapper.findById(userId).orElseThrow(this::invalidCredentials);
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthException(HttpStatus.FORBIDDEN, "이용할 수 없는 계정입니다.");
        }
        user.recordLoginSuccess(LocalDateTime.now());
        userMapper.update(user);
        return principalOf(user);
    }

    @Transactional(readOnly = true)
    public RunmatePrincipal refresh(RunmatePrincipal current) {
        User user = userMapper.findById(current.userId()).orElse(null);
        if (user == null || user.getStatus() != UserStatus.ACTIVE) {
            return null;
        }
        return principalOf(user);
    }

    private RunmatePrincipal principalOf(User user) {
        return new RunmatePrincipal(user.getUserId(), user.getLoginId(), user.getNickname(),
                user.getRole().name(), profileImageUrlResolver.resolve(user));
    }

    private AuthException invalidCredentials() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.");
    }

    public record LoginCandidate(RunmatePrincipal principal, String email) {
    }
}

