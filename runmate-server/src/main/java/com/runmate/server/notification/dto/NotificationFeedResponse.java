package com.runmate.server.notification.dto;

import java.time.LocalDateTime;
import java.util.List;

public record NotificationFeedResponse(
        List<Item> notifications,
        int currentPage,
        int totalPages,
        long total,
        long unreadCount
) {
    public record Item(
            long notificationId,
            String type,
            long actorId,
            String actorNickname,
            Long postId,
            String postTitle,
            Long commentId,
            boolean read,
            LocalDateTime createdAt
    ) {
    }
}
