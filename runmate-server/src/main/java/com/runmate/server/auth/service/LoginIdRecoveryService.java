package com.runmate.server.auth.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.runmate.server.auth.domain.PhoneVerification;
import com.runmate.server.auth.dto.AuthDtos.LoginIdRecoveryResponse;
import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.auth.mapper.PhoneVerificationMapper;
import com.runmate.server.user.domain.User;
import com.runmate.server.user.mapper.SocialAccountMapper;
import com.runmate.server.user.mapper.UserMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginIdRecoveryService {
    private final PhoneVerificationMapper phoneVerificationMapper;
    private final UserMapper userMapper;
    private final SocialAccountMapper socialAccountMapper;

    @Transactional
    public LoginIdRecoveryResponse recover(Long phoneVerificationId) {
        LocalDateTime now = LocalDateTime.now();
        PhoneVerification verification = phoneVerificationMapper.findById(phoneVerificationId)
                .orElseThrow(() -> new AuthException("휴대전화 인증 정보를 찾을 수 없습니다."));
        if (!verification.isUsableForLoginIdRecovery(now)) {
            throw new AuthException("휴대전화 인증이 만료되었거나 이미 사용되었습니다.");
        }
        User user = userMapper.findByPhoneNumber(verification.getPhoneNumber()).orElse(null);
        if (user == null) {
            return new LoginIdRecoveryResponse(false, null, null, List.of());
        }
        List<String> providers = socialAccountMapper.findProvidersByUserId(user.getUserId())
                .stream().map(Enum::name).toList();
        return new LoginIdRecoveryResponse(
                true,
                maskLoginId(user.getLoginId()),
                maskEmail(user.getEmail()),
                providers
        );
    }

    private String maskLoginId(String loginId) {
        if (loginId == null || loginId.isBlank()) return null;
        int visibleLength = Math.min(4, Math.max(1, loginId.length() / 2));
        return loginId.substring(0, visibleLength)
                + "*".repeat(Math.max(2, loginId.length() - visibleLength));
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return null;
        String[] parts = email.split("@", 2);
        String local = parts[0];
        int visibleLength = Math.min(3, Math.max(1, local.length() / 2));
        return local.substring(0, visibleLength)
                + "*".repeat(Math.max(3, local.length() - visibleLength))
                + "@" + parts[1];
    }
}


