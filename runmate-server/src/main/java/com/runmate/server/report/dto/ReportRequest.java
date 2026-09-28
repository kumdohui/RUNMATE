package com.runmate.server.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportRequest(
        @NotBlank(message = "신고 대상을 선택해 주세요.") String targetType,
        @NotNull(message = "신고 대상을 선택해 주세요.") Long targetId,
        @NotBlank(message = "신고 사유를 선택해 주세요.") String reason,
        @Size(max = 1000, message = "상세 내용은 1000자 이하로 입력해 주세요.") String detail
) {
}
