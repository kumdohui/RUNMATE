package com.runmate.server.auth.dto;

import java.io.Serializable;

public record KakaoPendingProfile(
        String providerUserId,
        String nickname,
        String profileImageUrl
) implements Serializable {
}
