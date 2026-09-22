package com.runmate.server.post.domain;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostImage {
    private Long imageId;

    private Long postId;

    private String originalName;

    private String storageKey;

    private String imageUrl;

    private String contentType;

    private Long fileSize;

    private Integer displayOrder;

    private LocalDateTime createdAt;

    public static PostImage create(Long postId, String originalName, String storageKey,
            String imageUrl, String contentType, long fileSize, int displayOrder) {
        PostImage image = new PostImage();
        image.postId = postId;
        image.originalName = originalName;
        image.storageKey = storageKey;
        image.imageUrl = imageUrl;
        image.contentType = contentType;
        image.fileSize = fileSize;
        image.displayOrder = displayOrder;
        return image;
    }
}
