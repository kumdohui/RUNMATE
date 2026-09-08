package com.runmate.server.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.runmate.server.auth.dto.LoginIdAvailabilityResponse;
import com.runmate.server.auth.exception.SignupException;
import com.runmate.server.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginIdAvailabilityService {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public LoginIdAvailabilityResponse check(String loginId) {
        if (loginId == null || !loginId.matches("^[a-z0-9]{4,20}$")) {
            throw new SignupException("아이디는 4~20자의 영문 소문자와 숫자만 사용할 수 있습니다.");
        }
        return new LoginIdAvailabilityResponse(!userRepository.existsByLoginId(loginId));
    }
}
