package com.runmate.server.post.dto;

public record PostImageResponse(Long imageId, String originalName, String contentType,
        long fileSize, int displayOrder, String imageUrl) {
}
