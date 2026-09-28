package com.runmate.server.post.dto;

import java.time.LocalDateTime;

public record CommentResponse(
        long commentId,
        long postId,
        long authorId,
        String authorNickname,
        String authorProfileImageUrl,
        Long parentCommentId,
        String replyToNickname,
        String content,
        boolean owner,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean authorAdmin
) {
}
