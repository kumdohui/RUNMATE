package com.runmate.server.report.dto;

import java.time.LocalDateTime;

public record AdminReportRow(
        long reportId,
        long reporterId,
        String reporterNickname,
        String targetType,
        long targetId,
        String reason,
        String detail,
        String status,
        LocalDateTime createdAt,
        String targetLabel,
        Long targetAuthorId
) {
}
