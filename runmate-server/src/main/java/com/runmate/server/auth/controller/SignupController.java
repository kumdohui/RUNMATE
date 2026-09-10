package com.runmate.server.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.runmate.server.auth.dto.AuthDtos.SignupRequest;
import com.runmate.server.auth.dto.AuthDtos.SignupResponse;
import com.runmate.server.auth.dto.AuthDtos.AvailabilityResponse;
import com.runmate.server.auth.service.SignupAvailabilityService;
import com.runmate.server.auth.service.SignupService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class SignupController {

    private final SignupService signupService;
    private final SignupAvailabilityService signupAvailabilityService;

    @GetMapping("/login-id/availability")
    public AvailabilityResponse checkLoginId(@RequestParam String loginId) {
        return signupAvailabilityService.checkLoginId(loginId);
    }

    @GetMapping("/nickname/availability")
    public AvailabilityResponse checkNickname(
            @RequestParam String nickname,
            @RequestParam(required = false) Long phoneVerificationId
    ) {
        return signupAvailabilityService.checkNickname(nickname, phoneVerificationId);
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(
            @Valid @RequestBody SignupRequest request,
            HttpServletRequest httpRequest
    ) {
        String ipAddress = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");

        SignupResponse response = signupService.signup(
                request,
                ipAddress,
                userAgent
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
