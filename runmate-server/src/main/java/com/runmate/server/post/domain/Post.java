package com.runmate.server.post.domain;

import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "posts")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long postId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "summary", nullable = false, length = 500)
    private String summary;

    @Column(name = "content", nullable = false, columnDefinition = "longtext")
    private String content;

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;

    @Column(name = "is_notice", nullable = false)
    private Boolean notice = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PostStatus status = PostStatus.DRAFT;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static Post create(Long authorId, Long categoryId, String title, String summary,
            String content, PostStatus status) {
        Post post = new Post();
        post.authorId = authorId;
        post.categoryId = categoryId;
        post.title = title;
        post.summary = summary;
        post.content = content;
        post.status = status;
        if (status == PostStatus.PUBLISHED) post.publishedAt = LocalDateTime.now();
        return post;
    }
}
