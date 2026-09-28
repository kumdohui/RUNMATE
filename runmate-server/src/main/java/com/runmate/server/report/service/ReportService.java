package com.runmate.server.report.service;

import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.post.domain.Comment;
import com.runmate.server.post.domain.Post;
import com.runmate.server.post.domain.PostStatus;
import com.runmate.server.post.mapper.CommentMapper;
import com.runmate.server.post.mapper.PostMapper;
import com.runmate.server.report.dto.ReportRequest;
import com.runmate.server.report.mapper.ReportMapper;
import com.runmate.server.user.domain.User;
import com.runmate.server.user.domain.UserRole;
import com.runmate.server.user.domain.UserStatus;
import com.runmate.server.user.mapper.UserMapper;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {
    private static final Set<String> TARGETS = Set.of("POST", "COMMENT", "USER");
    private static final Set<String> REASONS = Set.of("SPAM", "ABUSE", "INAPPROPRIATE", "PRIVACY", "OTHER");

    private final ReportMapper reportMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final UserMapper userMapper;

    @Transactional
    public void submit(Long reporterId, ReportRequest request) {
        String targetType = request.targetType().trim().toUpperCase();
        String reason = request.reason().trim().toUpperCase();
        if (!TARGETS.contains(targetType)) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "신고 대상을 선택해 주세요.");
        }
        if (!REASONS.contains(reason)) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "신고 사유를 선택해 주세요.");
        }
        String detail = request.detail() == null ? "" : request.detail().trim();
        if ("OTHER".equals(reason) && detail.isEmpty()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "기타 사유는 내용을 입력해 주세요.");
        }
        requireTarget(reporterId, targetType, request.targetId());
        if (reportMapper.existsOpen(reporterId, targetType, request.targetId())) {
            throw new AuthException(HttpStatus.CONFLICT, "이미 접수된 신고가 있습니다.");
        }
        reportMapper.insert(reporterId, targetType, request.targetId(), reason, detail.isEmpty() ? null : detail);
    }

    private void requireTarget(Long reporterId, String targetType, Long targetId) {
        if ("POST".equals(targetType)) {
            Post post = requireVisiblePost(targetId);
            if (post.getAuthorId().equals(reporterId)) {
                throw new AuthException(HttpStatus.BAD_REQUEST, "본인 게시글은 신고할 수 없습니다.");
            }
            rejectAdminTarget(post.getAuthorId());
            return;
        }
        if ("COMMENT".equals(targetType)) {
            Comment comment = commentMapper.findById(targetId)
                    .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "신고 대상을 찾을 수 없습니다."));
            if (comment.getDeletedAt() != null) {
                throw new AuthException(HttpStatus.NOT_FOUND, "신고 대상을 찾을 수 없습니다.");
            }
            requireVisiblePost(comment.getPostId());
            requireActiveAuthor(comment.getAuthorId());
            if (comment.getAuthorId().equals(reporterId)) {
                throw new AuthException(HttpStatus.BAD_REQUEST, "본인 댓글은 신고할 수 없습니다.");
            }
            rejectAdminTarget(comment.getAuthorId());
            return;
        }
        User user = userMapper.findById(targetId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "신고 대상을 찾을 수 없습니다."));
        if (user.getStatus() == UserStatus.DELETED || user.getDeletedAt() != null) {
            throw new AuthException(HttpStatus.NOT_FOUND, "신고 대상을 찾을 수 없습니다.");
        }
        if (user.getUserId().equals(reporterId)) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "본인은 신고할 수 없습니다.");
        }
        if (user.getRole() == UserRole.ADMIN) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "관리자 계정은 신고할 수 없습니다.");
        }
    }

    private void rejectAdminTarget(Long authorId) {
        User author = userMapper.findById(authorId).orElse(null);
        if (author != null && author.getRole() == UserRole.ADMIN) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "관리자 계정은 신고할 수 없습니다.");
        }
    }

    private Post requireVisiblePost(Long postId) {
        Post post = postMapper.findById(postId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "신고 대상을 찾을 수 없습니다."));
        if (post.getStatus() != PostStatus.PUBLISHED || post.getDeletedAt() != null) {
            throw new AuthException(HttpStatus.NOT_FOUND, "신고 대상을 찾을 수 없습니다.");
        }
        requireActiveAuthor(post.getAuthorId());
        return post;
    }

    private void requireActiveAuthor(Long authorId) {
        User author = userMapper.findById(authorId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "신고 대상을 찾을 수 없습니다."));
        if (author.getStatus() == UserStatus.SUSPENDED) {
            throw new AuthException(HttpStatus.NOT_FOUND, "신고 대상을 찾을 수 없습니다.");
        }
    }
}
