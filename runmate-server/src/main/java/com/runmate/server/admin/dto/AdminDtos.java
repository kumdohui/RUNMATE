package com.runmate.server.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public final class AdminDtos {
    private AdminDtos() {
    }

    public record ReauthenticationRequest(
            @NotBlank(message = "관리자 비밀번호를 입력해 주세요.") String password
    ) {
    }

    public record ReportDecisionRequest(
            @NotBlank(message = "처리 메모를 입력해 주세요.")
            @Size(max = 1000, message = "메모는 1000자 이하로 입력해 주세요.") String note,
            Boolean hideTarget,
            Boolean suspendUser
    ) {
    }

    public record MemberStatusRequest(
            @NotBlank(message = "변경할 상태를 선택해 주세요.") String status,
            @NotBlank(message = "처리 메모를 입력해 주세요.")
            @Size(max = 1000, message = "메모는 1000자 이하로 입력해 주세요.") String note
    ) {
    }

    public record ReportFeedResponse(List<ReportItem> reports, int currentPage, int totalPages, long total) {
    }

    public record ReportItem(
            long reportId,
            String reporterNickname,
            String targetType,
            long targetId,
            String targetLabel,
            String reason,
            String detail,
            String status,
            LocalDateTime createdAt
    ) {
    }

    public record MemberFeedResponse(List<MemberItem> users, int currentPage, int totalPages, long total) {
    }

    public record MemberItem(
            long userId,
            String loginId,
            String nickname,
            String role,
            String status,
            LocalDateTime createdAt
    ) {
    }

    public record LogFeedResponse(List<LogItem> logs, int currentPage, int totalPages, long total) {
    }

    public record LogItem(
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
}
