package com.runmate.server.terms.dto;

public record CurrentTermResponse(
        Long termId,
        String termCode,
        String version,
        String title,
        String contentUrl,
        String contentHash,
        boolean required
) {
}
