package com.runmate.server.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.runmate.server.auth.dto.AuthDtos.PasswordResetRequest;
import com.runmate.server.auth.dto.AuthDtos.PasswordRecoveryRequest;
import com.runmate.server.auth.dto.AuthDtos.PasswordRecoveryFromPhoneRequest;
import com.runmate.server.auth.dto.AuthDtos.PasswordRecoveryResponse;
import com.runmate.server.auth.service.PasswordRecoveryService;
import com.runmate.server.auth.service.PasswordResetService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth/password")
@RequiredArgsConstructor
public class PasswordResetController {
    private final PasswordResetService passwordResetService;
    private final PasswordRecoveryService passwordRecoveryService;

    @PostMapping("/recovery")
    public PasswordRecoveryResponse recovery(
            @Valid @RequestBody PasswordRecoveryRequest request
    ) {
        return passwordRecoveryService.startByLoginId(request.loginId());
    }

    @PostMapping("/recovery/from-phone")
    public PasswordRecoveryResponse recoveryFromPhone(
            @Valid @RequestBody PasswordRecoveryFromPhoneRequest request
    ) {
        return passwordRecoveryService.startFromPhoneVerification(request.phoneVerificationId());
    }

    @PatchMapping("/reset")
    public ResponseEntity<Void> reset(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.reset(request);
        return ResponseEntity.noContent().build();
    }
}
