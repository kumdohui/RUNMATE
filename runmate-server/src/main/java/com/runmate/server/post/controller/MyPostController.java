package com.runmate.server.post.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.auth.security.RunmatePrincipal;
import com.runmate.server.post.dto.MyPostFeedResponse;
import com.runmate.server.post.service.PostService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users/me/posts")
@RequiredArgsConstructor
public class MyPostController {
    private final PostService postService;

    @GetMapping
    public MyPostFeedResponse list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            Authentication authentication
    ) {
        return postService.myFeed(principal(authentication).userId(), page, sort, keyword, status);
    }

    private RunmatePrincipal principal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof RunmatePrincipal principal)) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return principal;
    }
}
