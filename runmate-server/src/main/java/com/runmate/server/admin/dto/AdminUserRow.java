package com.runmate.server.admin.dto;

import java.time.LocalDateTime;

public record AdminUserRow(
        long userId,
        String loginId,
        String nickname,
        String role,
        String status,
        LocalDateTime createdAt
) {
}
