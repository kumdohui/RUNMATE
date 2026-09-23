package com.runmate.server.post.dto;

import java.time.LocalDateTime;

public record MyCommentRow(
        long commentId,
        long postId,
        String postTitle,
        String content,
        LocalDateTime createdAt
) {
}
