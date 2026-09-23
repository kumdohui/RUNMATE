package com.runmate.server.post.dto;

import java.util.List;

public record SavedPostResponse(
        List<MyPostFeedResponse.Item> posts,
        int currentPage,
        int totalPages,
        long totalElements
) {
}
