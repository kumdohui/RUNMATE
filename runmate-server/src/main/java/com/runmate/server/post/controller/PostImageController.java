package com.runmate.server.post.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.auth.security.RunmatePrincipal;
import com.runmate.server.post.dto.PostImageResponse;
import com.runmate.server.post.service.PostImageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/posts/{postId}/images")
@RequiredArgsConstructor
public class PostImageController {
    private final PostImageService postImageService;

    @PostMapping
    public ResponseEntity<List<PostImageResponse>> upload(
            @PathVariable Long postId,
            @RequestPart("images") List<MultipartFile> images,
            @RequestParam(required = false) List<Integer> displayOrder,
            Authentication authentication
    ) {
        List<PostImageResponse> result = postImageService.upload(
                principal(authentication).userId(), postId, images, displayOrder);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> delete(@PathVariable Long postId, @PathVariable Long imageId,
            Authentication authentication) {
        postImageService.delete(principal(authentication).userId(), postId, imageId);
        return ResponseEntity.noContent().build();
    }

    private RunmatePrincipal principal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof RunmatePrincipal principal)) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return principal;
    }
}
