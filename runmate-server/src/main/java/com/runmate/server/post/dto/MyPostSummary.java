package com.runmate.server.post.dto;

public record MyPostSummary(
        long postCount,
        long publishedCount,
        long draftCount,
        Double totalDistanceKm,
        Long totalDurationSeconds
) {
}
