package com.runmate.server.post.dto;

public record MyPostSearchCondition(
        Long authorId,
        String keyword,
        String status,
        String sort,
        int limit,
        int offset
) {
}
