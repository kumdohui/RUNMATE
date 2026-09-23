package com.runmate.server.post.controller;

import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.auth.security.RunmatePrincipal;
import com.runmate.server.post.dto.CommentRequest;
import com.runmate.server.post.dto.CommentResponse;
import com.runmate.server.post.dto.MyCommentResponse;
import com.runmate.server.post.service.CommentService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @GetMapping("/api/posts/{postId}/comments")
    public List<CommentResponse> list(@PathVariable Long postId, Authentication authentication) {
        return commentService.list(viewerId(authentication), postId);
    }

    @PostMapping("/api/posts/{postId}/comments")
    public ResponseEntity<CommentResponse> add(
            @PathVariable Long postId,
            @Valid @RequestBody CommentRequest request,
            Authentication authentication
    ) {
        CommentResponse result = commentService.add(principal(authentication).userId(), postId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PutMapping("/api/posts/{postId}/comments/{commentId}")
    public CommentResponse edit(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequest request,
            Authentication authentication
    ) {
        return commentService.edit(principal(authentication).userId(), postId, commentId, request);
    }

    @DeleteMapping("/api/posts/{postId}/comments/{commentId}")
    public ResponseEntity<Void> remove(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            Authentication authentication
    ) {
        commentService.remove(principal(authentication).userId(), postId, commentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/users/me/comments")
    public MyCommentResponse mine(
            @RequestParam(required = false) Integer page,
            Authentication authentication
    ) {
        return commentService.mine(principal(authentication).userId(), page);
    }

    private Long viewerId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof RunmatePrincipal principal)) {
            return null;
        }
        return principal.userId();
    }

    private RunmatePrincipal principal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof RunmatePrincipal principal)) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return principal;
    }
}
