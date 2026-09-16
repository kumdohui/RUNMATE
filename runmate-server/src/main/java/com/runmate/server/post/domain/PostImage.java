package com.runmate.server.post.domain;

import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "post_images")
public class PostImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long imageId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "storage_key", nullable = false, length = 255, unique = true)
    private String storageKey;

    @Column(name = "image_url", nullable = false, length = 1000)
    private String imageUrl;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
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
