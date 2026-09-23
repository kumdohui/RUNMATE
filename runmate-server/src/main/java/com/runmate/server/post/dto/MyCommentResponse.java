package com.runmate.server.post.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MyCommentResponse(
        List<Item> comments,
        int currentPage,
        int totalPages,
        long totalElements
) {
    public record Item(
            long commentId,
            long postId,
            String postTitle,
            String content,
            LocalDateTime createdAt
    ) {
    }
}
