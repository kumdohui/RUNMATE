package com.runmate.server.user.dto;

import java.util.List;

public final class PublicProfileDtos {
    private PublicProfileDtos() {
    }

    public record PublicProfileResponse(
            long userId,
            String nickname,
            String bio,
            String profileImageUrl,
            boolean withdrawn,
            boolean admin,
            boolean suspended
    ) {
    }

    public record PublicPostFeedResponse(
            List<PublicPostItem> posts,
            int currentPage,
            int totalPages,
            long total
    ) {
    }

    public record PublicPostItem(
            long postId,
            String categoryName,
            String title,
            String summary,
            String imageUrl,
            Double distanceKm,
            Integer durationSeconds,
            long likeCount,
            long commentCount
    ) {
    }
}
