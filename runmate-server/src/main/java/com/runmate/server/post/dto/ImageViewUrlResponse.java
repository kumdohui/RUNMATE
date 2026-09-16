package com.runmate.server.post.dto;

import java.time.Instant;

public record ImageViewUrlResponse(String url, Instant expiresAt) {
}
