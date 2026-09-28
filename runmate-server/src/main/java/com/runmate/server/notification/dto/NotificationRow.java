package com.runmate.server.notification.dto;

import java.time.LocalDateTime;

public record NotificationRow(
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
