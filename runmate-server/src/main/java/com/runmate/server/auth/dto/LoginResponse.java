package com.runmate.server.auth.dto;

public record LoginResponse(
        Long userId,
        String loginId,
        String nickname,
        String role,
        String profileImageUrl
) {
}
