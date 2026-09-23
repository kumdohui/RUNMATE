package com.runmate.server.post.domain;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment {
    private Long commentId;
    private Long postId;
    private Long authorId;
    private Long parentCommentId;
    private Long replyToUserId;
    private String content;
    private LocalDateTime deletedAt;

    public static Comment create(Long postId, Long authorId, Long parentCommentId,
            Long replyToUserId, String content) {
        Comment comment = new Comment();
        comment.postId = postId;
        comment.authorId = authorId;
        comment.parentCommentId = parentCommentId;
        comment.replyToUserId = replyToUserId;
        comment.content = content;
        return comment;
    }

    public void edit(String content) {
        this.content = content;
    }

    public void remove() {
        this.deletedAt = LocalDateTime.now();
    }
}
