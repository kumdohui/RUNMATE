package com.runmate.server.admin.dto;

import java.time.LocalDateTime;

public record AdminLogRow(
        long logId,
        String adminNickname,
        String summary,
        String note,
        LocalDateTime createdAt,
        String status,
        String targetType,
        long targetId,
        String targetLabel,
        String reason,
        String detail,
        String reporterNickname,
        String targetNickname,
        String targetStatus
) {
}
