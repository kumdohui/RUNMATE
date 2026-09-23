package com.runmate.server.post.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PostDetailResponse(
        Long postId,
        Long authorId,
        String authorNickname,
        Long categoryId,
        String categoryName,
        String title,
        String content,
        String status,
        boolean owner,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        RunningRecordDetail runningRecord,
        List<String> tags,
        List<PostImageResponse> images
) {
    public record RunningRecordDetail(LocalDate runningDate, BigDecimal distanceKm,
            int durationSeconds, String locationName, String region, String intensity, String weather) {
    }
}
