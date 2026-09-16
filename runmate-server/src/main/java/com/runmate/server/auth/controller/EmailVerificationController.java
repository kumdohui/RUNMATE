package com.runmate.server.auth.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.runmate.server.auth.dto.AuthDtos.EmailVerificationConfirmRequest;
import com.runmate.server.auth.dto.AuthDtos.EmailVerificationConfirmResponse;
import com.runmate.server.auth.dto.AuthDtos.EmailVerificationIssueResponse;
import com.runmate.server.auth.dto.AuthDtos.EmailVerificationRequest;
import com.runmate.server.auth.service.EmailVerificationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth/email-verifications")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    @PostMapping
    public EmailVerificationIssueResponse issue(
            @Valid @RequestBody EmailVerificationRequest request
    ) {
        return emailVerificationService.issue(request.email(), request.purpose());
    }

    @PostMapping("/{verificationId}/confirm")
    public EmailVerificationConfirmResponse confirm(
            @PathVariable Long verificationId,
            @Valid @RequestBody EmailVerificationConfirmRequest request
    ) {
        return emailVerificationService.confirm(verificationId, request.code());
    }
}
