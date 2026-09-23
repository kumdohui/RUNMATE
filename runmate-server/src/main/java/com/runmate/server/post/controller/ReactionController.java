package com.runmate.server.post.controller;

import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.auth.security.RunmatePrincipal;
import com.runmate.server.post.dto.PostReactionResponse;
import com.runmate.server.post.dto.SavedPostResponse;
import com.runmate.server.post.service.ReactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReactionController {
    private final ReactionService reactionService;

    @GetMapping("/api/posts/{postId}/reaction")
    public PostReactionResponse state(@PathVariable Long postId, Authentication authentication) {
        return reactionService.state(viewerId(authentication), postId);
    }

    @PostMapping("/api/posts/{postId}/likes")
    public PostReactionResponse like(@PathVariable Long postId, Authentication authentication) {
        return reactionService.like(principal(authentication).userId(), postId);
    }

    @DeleteMapping("/api/posts/{postId}/likes")
    public PostReactionResponse unlike(@PathVariable Long postId, Authentication authentication) {
        return reactionService.unlike(principal(authentication).userId(), postId);
    }

    @PostMapping("/api/posts/{postId}/bookmarks")
    public PostReactionResponse mark(@PathVariable Long postId, Authentication authentication) {
        return reactionService.mark(principal(authentication).userId(), postId);
    }

    @DeleteMapping("/api/posts/{postId}/bookmarks")
    public PostReactionResponse unmark(@PathVariable Long postId, Authentication authentication) {
        return reactionService.unmark(principal(authentication).userId(), postId);
    }

    @GetMapping("/api/users/me/bookmarks")
    public SavedPostResponse saved(
            @RequestParam(required = false) Integer page,
            Authentication authentication
    ) {
        return reactionService.saved(principal(authentication).userId(), page);
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
