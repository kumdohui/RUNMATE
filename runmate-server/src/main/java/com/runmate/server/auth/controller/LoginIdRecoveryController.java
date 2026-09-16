package com.runmate.server.auth.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.runmate.server.auth.dto.AuthDtos.LoginIdRecoveryRequest;
import com.runmate.server.auth.dto.AuthDtos.LoginIdRecoveryResponse;
import com.runmate.server.auth.service.LoginIdRecoveryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth/login-id")
@RequiredArgsConstructor
public class LoginIdRecoveryController {
    private final LoginIdRecoveryService loginIdRecoveryService;

    @PostMapping("/recovery")
    public LoginIdRecoveryResponse recover(@Valid @RequestBody LoginIdRecoveryRequest request) {
        return loginIdRecoveryService.recover(request.phoneVerificationId());
    }
}
