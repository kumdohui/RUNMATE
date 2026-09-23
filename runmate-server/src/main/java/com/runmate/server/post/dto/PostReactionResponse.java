package com.runmate.server.post.dto;

public record PostReactionResponse(long likeCount, boolean liked, boolean bookmarked) {
}
