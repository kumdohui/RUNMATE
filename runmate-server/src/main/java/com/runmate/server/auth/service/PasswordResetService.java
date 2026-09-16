package com.runmate.server.auth.service;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.runmate.server.auth.domain.EmailVerification;
import com.runmate.server.auth.dto.AuthDtos.PasswordResetRequest;
import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.auth.repository.EmailVerificationRepository;
import com.runmate.server.user.domain.User;
import com.runmate.server.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private final EmailVerificationRepository emailVerificationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectProvider<FindByIndexNameSessionRepository<? extends Session>> sessionRepositoryProvider;

    @Transactional
    public void reset(PasswordResetRequest request) {
        LocalDateTime now = LocalDateTime.now();
        EmailVerification verification = emailVerificationRepository
                .findById(request.emailVerificationId())
                .orElseThrow(() -> new AuthException("이메일 인증 정보를 찾을 수 없습니다."));
        if (!verification.isUsableForPasswordReset(now)) {
            throw new AuthException("비밀번호 재설정 인증이 만료되었거나 이미 사용되었습니다.");
        }

        User user = userRepository.findByEmail(verification.getEmail())
                .filter(found -> found.getPasswordHash() != null)
                .orElseThrow(() -> new AuthException("비밀번호를 재설정할 계정을 찾을 수 없습니다."));
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new AuthException("현재 사용 중인 비밀번호와 다른 비밀번호를 입력해 주세요.");
        }
        user.resetPassword(passwordEncoder.encode(request.newPassword()), now);
        verification.consume(now);

        FindByIndexNameSessionRepository<? extends Session> sessionRepository =
                sessionRepositoryProvider.getIfAvailable();
        if (sessionRepository != null) {
            sessionRepository.findByPrincipalName(user.getUserId().toString())
                    .keySet()
                    .forEach(sessionRepository::deleteById);
        }
    }
}
