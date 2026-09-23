package com.runmate.server.user.dto;

public record MentionRow(
        long userId,
        String nickname,
        String profileImageUrl,
        String profileImageStorageKey
) {
}
