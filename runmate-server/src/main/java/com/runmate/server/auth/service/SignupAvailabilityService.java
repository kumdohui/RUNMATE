package com.runmate.server.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.runmate.server.auth.domain.PhoneVerification;
import com.runmate.server.auth.dto.AuthDtos.AvailabilityResponse;
import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.auth.repository.PhoneVerificationRepository;
import com.runmate.server.user.domain.User;
import com.runmate.server.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SignupAvailabilityService {
    private final UserRepository userRepository;
    private final PhoneVerificationRepository phoneVerificationRepository;

    @Transactional(readOnly = true)
    public AvailabilityResponse checkLoginId(String loginId) {
        if (loginId == null || !loginId.matches("^[a-z0-9]{4,20}$")) {
            throw new AuthException("아이디는 4~20자의 영문 소문자와 숫자만 사용할 수 있습니다.");
        }
        return new AvailabilityResponse(!userRepository.existsByLoginId(loginId));
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse checkNickname(String nickname, Long phoneVerificationId) {
        String normalizedNickname = nickname == null ? "" : nickname.trim(); 
        int length = normalizedNickname.codePointCount(0, normalizedNickname.length());
        if (length < 2 || length > 20) {
            throw new AuthException("닉네임은 2~20자로 입력해 주세요.");
        }

        User nicknameOwner = userRepository.findByNickname(normalizedNickname).orElse(null);
        if (nicknameOwner == null) {
            return new AvailabilityResponse(true);
        }

        if (phoneVerificationId != null) {
            PhoneVerification verification = phoneVerificationRepository.findById(phoneVerificationId)
                    .orElse(null);
            if (verification != null && verification.getVerifiedAt() != null) {
                User verifiedUser = userRepository.findByPhoneNumber(verification.getPhoneNumber())
                        .orElse(null);
                if (verifiedUser != null && verifiedUser.getUserId().equals(nicknameOwner.getUserId())) {
                    return new AvailabilityResponse(true);
                }
            }
        }

        return new AvailabilityResponse(false);
    }
}
