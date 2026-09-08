package com.runmate.server.auth.dto;

public record CsrfTokenResponse(
        String headerName,
        String token
) {
}
