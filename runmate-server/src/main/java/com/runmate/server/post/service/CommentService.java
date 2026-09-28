package com.runmate.server.post.service;

import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.post.domain.Comment;
import com.runmate.server.post.domain.Post;
import com.runmate.server.post.domain.PostStatus;
import com.runmate.server.post.dto.CommentRequest;
import com.runmate.server.post.dto.CommentResponse;
import com.runmate.server.post.dto.CommentRow;
import com.runmate.server.post.dto.MyCommentResponse;
import com.runmate.server.notification.service.NotificationService;
import com.runmate.server.post.mapper.CommentMapper;
import com.runmate.server.post.mapper.PostMapper;
import com.runmate.server.user.service.ProfileImageUrlResolver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {
    private static final int PAGE_SIZE = 9;

    private final CommentMapper commentMapper;
    private final PostMapper postMapper;
    private final ProfileImageUrlResolver profileImageUrlResolver;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<CommentResponse> list(Long viewerId, Long postId) {
        requirePublished(postId);
        return commentMapper.findByPostId(postId).stream()
                .map(row -> toResponse(row, viewerId))
                .toList();
    }

    @Transactional
    public CommentResponse add(Long userId, Long postId, CommentRequest request) {
        Post post = requirePublished(postId);
        Long parentId = request.parentCommentId();
        Long replyToUserId = null;
        Comment parent = null;
        if (parentId != null) {
            parent = requireLive(parentId);
            if (!parent.getPostId().equals(postId)) {
                throw new AuthException(HttpStatus.BAD_REQUEST, "이 게시글의 댓글에만 답글을 달 수 있습니다.");
            }
            if (parent.getParentCommentId() != null) {
                throw new AuthException(HttpStatus.BAD_REQUEST, "답글에는 다시 답글을 달 수 없습니다.");
            }
            replyToUserId = parent.getAuthorId();
        }
        Comment comment = Comment.create(postId, userId, parentId, replyToUserId, request.content().trim());
        commentMapper.insert(comment);
        notificationService.addForComment(userId, post.getAuthorId(), parent, postId, comment.getCommentId());
        return toResponse(requireRow(comment.getCommentId()), userId);
    }

    @Transactional
    public CommentResponse edit(Long userId, Long postId, Long commentId, CommentRequest request) {
        Comment comment = requireOwner(userId, postId, commentId);
        comment.edit(request.content().trim());
        commentMapper.update(comment);
        return toResponse(requireRow(commentId), userId);
    }

    @Transactional
    public void remove(Long userId, Long postId, Long commentId) {
        requireOwner(userId, postId, commentId);
        commentMapper.markDeleted(commentId);
    }

    @Transactional(readOnly = true)
    public MyCommentResponse mine(Long userId, Integer requestedPage) {
        int page = Math.max(requestedPage == null ? 0 : requestedPage, 0);
        long total = commentMapper.countMine(userId);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / PAGE_SIZE);
        if (totalPages > 0 && page >= totalPages) {
            page = totalPages - 1;
        }
        List<MyCommentResponse.Item> comments = commentMapper.searchMine(userId, PAGE_SIZE, page * PAGE_SIZE)
                .stream()
                .map(row -> new MyCommentResponse.Item(
                        row.commentId(), row.postId(), row.postTitle(), row.content(), row.createdAt()))
                .toList();
        return new MyCommentResponse(comments, page, totalPages, total);
    }

    private Comment requireOwner(Long userId, Long postId, Long commentId) {
        Comment comment = requireLive(commentId);
        if (!comment.getPostId().equals(postId)) {
            throw new AuthException(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다.");
        }
        if (!comment.getAuthorId().equals(userId)) {
            throw new AuthException(HttpStatus.FORBIDDEN, "댓글 작성자만 수정하거나 삭제할 수 있습니다.");
        }
        return comment;
    }

    private Comment requireLive(Long commentId) {
        Comment comment = commentMapper.findById(commentId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."));
        if (comment.getDeletedAt() != null) {
            throw new AuthException(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다.");
        }
        return comment;
    }

    private CommentRow requireRow(Long commentId) {
        return commentMapper.findRow(commentId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."));
    }

    private Post requirePublished(Long postId) {
        Post post = postMapper.findById(postId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));
        if (post.getStatus() != PostStatus.PUBLISHED || post.getDeletedAt() != null) {
            throw new AuthException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다.");
        }
        return post;
    }

    private CommentResponse toResponse(CommentRow row, Long viewerId) {
        return new CommentResponse(
                row.commentId(),
                row.postId(),
                row.authorId(),
                row.authorNickname(),
                profileImageUrlResolver.resolve(row.authorProfileImageStorageKey(), row.authorProfileImageUrl()),
                row.parentCommentId(),
                row.replyToNickname(),
                row.content(),
                viewerId != null && viewerId.equals(row.authorId()),
                row.createdAt(),
                row.updatedAt(),
                row.authorAdmin()
        );
    }
}
