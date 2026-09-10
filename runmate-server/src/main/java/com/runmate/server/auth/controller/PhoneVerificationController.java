package com.runmate.server.auth.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.runmate.server.auth.dto.AuthDtos.PhoneVerificationConfirmRequest;
import com.runmate.server.auth.dto.AuthDtos.PhoneVerificationConfirmResponse;
import com.runmate.server.auth.dto.AuthDtos.PhoneVerificationIssueResponse;
import com.runmate.server.auth.dto.AuthDtos.PhoneVerificationRequest;
import com.runmate.server.auth.service.PhoneVerificationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth/phone-verifications")
@RequiredArgsConstructor
public class PhoneVerificationController {

    private final PhoneVerificationService phoneVerificationService;

    @PostMapping
    public PhoneVerificationIssueResponse issue(
            @Valid @RequestBody PhoneVerificationRequest request
    ) {
        return phoneVerificationService.issue(
                request.phoneNumber(),
                Boolean.TRUE.equals(request.accountLinkingAllowed())
        );
    }

    @PostMapping("/{verificationId}/confirm")
    public PhoneVerificationConfirmResponse confirm(
            @PathVariable Long verificationId,
            @Valid @RequestBody PhoneVerificationConfirmRequest request
    ) {
        return phoneVerificationService.confirm(verificationId, request.code());
    }
}
