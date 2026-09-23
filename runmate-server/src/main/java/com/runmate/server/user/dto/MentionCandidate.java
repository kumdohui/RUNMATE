package com.runmate.server.user.dto;

public record MentionCandidate(
        long userId,
        String nickname,
        String profileImageUrl
) {
}
