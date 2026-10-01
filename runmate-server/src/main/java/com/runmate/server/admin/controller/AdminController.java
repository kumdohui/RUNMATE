package com.runmate.server.admin.controller;

import com.runmate.server.admin.dto.AdminDtos.LogFeedResponse;
import com.runmate.server.admin.dto.AdminDtos.MemberFeedResponse;
import com.runmate.server.admin.dto.AdminDtos.MemberStatusRequest;
import com.runmate.server.admin.dto.AdminDtos.ReportDecisionRequest;
import com.runmate.server.admin.dto.AdminDtos.ReportFeedResponse;
import com.runmate.server.admin.service.AdminService;
import com.runmate.server.admin.service.AdminReauthenticationService;
import com.runmate.server.admin.dto.AdminDtos.ReauthenticationRequest;
import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.auth.security.RunmatePrincipal;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;
    private final AdminReauthenticationService reauthenticationService;

    @PostMapping("/api/admin/reauthenticate")
    public ResponseEntity<Void> reauthenticate(
            @Valid @RequestBody ReauthenticationRequest request,
            Authentication authentication,
            HttpSession session
    ) {
        reauthenticationService.reauthenticate(
                principal(authentication).userId(),
                request.password(),
                session
        );
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/admin/reports")
    public ReportFeedResponse reports(@RequestParam(required = false) Integer page, Authentication authentication) {
        principal(authentication);
        return adminService.reports(page);
    }

    @PostMapping("/api/admin/reports/{reportId}/resolve")
    public ResponseEntity<Void> resolve(
            @PathVariable Long reportId,
            @Valid @RequestBody ReportDecisionRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        reauthenticationService.requireRecent(servletRequest.getSession());
        adminService.resolve(
                principal(authentication).userId(),
                reportId,
                request,
                clientIp(servletRequest)
        );
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/admin/reports/{reportId}/reject")
    public ResponseEntity<Void> reject(
            @PathVariable Long reportId,
            @Valid @RequestBody ReportDecisionRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        reauthenticationService.requireRecent(servletRequest.getSession());
        adminService.reject(
                principal(authentication).userId(),
                reportId,
                request,
                clientIp(servletRequest)
        );
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/admin/users")
    public MemberFeedResponse members(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            Authentication authentication
    ) {
        principal(authentication);
        return adminService.members(keyword, page);
    }

    @PatchMapping("/api/admin/users/{userId}/status")
    public ResponseEntity<Void> changeStatus(
            @PathVariable Long userId,
            @Valid @RequestBody MemberStatusRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        reauthenticationService.requireRecent(servletRequest.getSession());
        adminService.changeMemberStatus(
                principal(authentication).userId(),
                userId,
                request,
                clientIp(servletRequest)
        );
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/admin/logs")
    public LogFeedResponse logs(@RequestParam(required = false) Integer page, Authentication authentication) {
        principal(authentication);
        return adminService.logs(page);
    }

    private RunmatePrincipal principal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof RunmatePrincipal principal)) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return principal;
    }

    private String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
