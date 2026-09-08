package com.runmate.server.auth.dto;

public record KakaoPendingProfileResponse(
        String nickname,
        String profileImageUrl
) {
}
