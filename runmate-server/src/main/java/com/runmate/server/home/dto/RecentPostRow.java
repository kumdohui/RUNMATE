package com.runmate.server.home.dto;

public record RecentPostRow(
        long postId,
        String categoryName,
        String title,
        String summary,
        String authorNickname,
        String storageKey,
        Double distanceKm,
        Integer durationSeconds,
        long likeCount,
        long commentCount,
        long authorId
) {
}
