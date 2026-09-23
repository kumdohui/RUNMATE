package com.runmate.server.post.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.auth.security.RunmatePrincipal;
import com.runmate.server.post.dto.CreatePostRequest;
import com.runmate.server.post.dto.CreatePostResponse;
import com.runmate.server.post.dto.PostDetailResponse;
import com.runmate.server.post.service.PostService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;

    @PostMapping
    public ResponseEntity<CreatePostResponse> create(@Valid @RequestBody CreatePostRequest request,
            Authentication authentication) {
        CreatePostResponse result = postService.create(principal(authentication).userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/{postId}")
    public PostDetailResponse detail(@PathVariable Long postId, Authentication authentication) {
        Long viewerId = authentication != null
                && authentication.getPrincipal() instanceof RunmatePrincipal principal
                        ? principal.userId()
                        : null;
        return postService.detail(viewerId, postId);
    }

    @PutMapping("/{postId}")
    public CreatePostResponse update(
            @PathVariable Long postId,
            @Valid @RequestBody CreatePostRequest request,
            Authentication authentication
    ) {
        return postService.update(
                principal(authentication).userId(),
                postId,
                request
        );
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long postId,
            Authentication authentication
    ) {
        postService.delete(principal(authentication).userId(), postId);
        return ResponseEntity.noContent().build();
    }

    private RunmatePrincipal principal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof RunmatePrincipal principal)) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return principal;
    }
}
