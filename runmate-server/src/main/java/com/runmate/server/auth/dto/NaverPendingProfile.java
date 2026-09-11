package com.runmate.server.auth.dto;

import java.io.Serializable;

/** 네이버 회원가입이 완료될 때까지 HTTP 세션에 보관하는 정보입니다. */
public record NaverPendingProfile(
        String providerUserId,
        String nickname,
        String profileImageUrl
) implements Serializable {
}
