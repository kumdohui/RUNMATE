package com.runmate.server.post.domain;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {
    private Long postId;

    private Long authorId;

    private Long categoryId;

    private String title;

    private String summary;

    private String content;

    private String thumbnailUrl;

    private Long viewCount = 0L;

    private Boolean notice = false;

    private PostStatus status = PostStatus.DRAFT;

    private LocalDateTime publishedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

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

    public void update(Long categoryId, String title, String summary,
            String content, PostStatus status) {
        this.categoryId = categoryId;
        this.title = title;
        this.summary = summary;
        this.content = content;
        if (this.status != PostStatus.PUBLISHED && status == PostStatus.PUBLISHED) {
            this.publishedAt = LocalDateTime.now();
        }
        this.status = status;
    }

    public void delete() {
        this.status = PostStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }

    public void hide() {
        this.status = PostStatus.HIDDEN;
    }
}
