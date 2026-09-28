package com.runmate.server.notification.controller;

import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.auth.security.RunmatePrincipal;
import com.runmate.server.notification.dto.NotificationFeedResponse;
import com.runmate.server.notification.dto.UnreadCountResponse;
import com.runmate.server.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping("/api/notifications/unread-count")
    public UnreadCountResponse unreadCount(Authentication authentication) {
        return notificationService.unreadCount(principal(authentication).userId());
    }

    @GetMapping("/api/notifications")
    public NotificationFeedResponse list(
            @RequestParam(required = false) Integer page,
            Authentication authentication
    ) {
        return notificationService.list(principal(authentication).userId(), page);
    }

    @PatchMapping("/api/notifications/{notificationId}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long notificationId, Authentication authentication) {
        notificationService.markRead(principal(authentication).userId(), notificationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/notifications/read-all")
    public ResponseEntity<Void> markAllRead(Authentication authentication) {
        notificationService.markAllRead(principal(authentication).userId());
        return ResponseEntity.noContent().build();
    }

    private RunmatePrincipal principal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof RunmatePrincipal principal)) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return principal;
    }
}
