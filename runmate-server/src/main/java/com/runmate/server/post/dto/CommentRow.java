package com.runmate.server.post.dto;

import java.time.LocalDateTime;

public record CommentRow(
        long commentId,
        long postId,
        long authorId,
        String authorNickname,
        String authorProfileImageUrl,
        String authorProfileImageStorageKey,
        Long parentCommentId,
        String replyToNickname,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean authorAdmin
) {
}
