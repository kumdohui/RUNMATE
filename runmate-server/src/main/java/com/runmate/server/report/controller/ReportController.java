package com.runmate.server.report.controller;

import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.auth.security.RunmatePrincipal;
import com.runmate.server.report.dto.ReportRequest;
import com.runmate.server.report.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;

    @PostMapping("/api/reports")
    public ResponseEntity<Void> submit(@Valid @RequestBody ReportRequest request, Authentication authentication) {
        reportService.submit(principal(authentication).userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    private RunmatePrincipal principal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof RunmatePrincipal principal)) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return principal;
    }
}
