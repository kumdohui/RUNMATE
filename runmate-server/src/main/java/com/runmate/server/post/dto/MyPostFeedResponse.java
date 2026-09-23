package com.runmate.server.post.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MyPostFeedResponse(
        List<Item> posts,
        int currentPage,
        int totalPages,
        long totalElements,
        double totalDistanceKm,
        long totalDurationSeconds,
        long publishedCount,
        long draftCount,
        long bookmarkCount,
        long myCommentCount
) {
    public record Item(
            Long postId,
            String categoryName,
            String title,
            String summary,
            String status,
            LocalDateTime publishedAt,
            LocalDateTime createdAt,
            String imageUrl,
            Double distanceKm,
            Integer durationSeconds,
            String region,
            long likeCount,
            long commentCount
    ) {
    }
}
