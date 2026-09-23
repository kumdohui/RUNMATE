package com.runmate.server.post.dto;

import java.time.LocalDateTime;

public record MyPostRow(
        Long postId,
        String categoryName,
        String title,
        String summary,
        String status,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        String storageKey,
        Double distanceKm,
        Integer durationSeconds,
        String region,
        long likeCount,
        long commentCount
) {
}
