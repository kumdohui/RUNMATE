package com.runmate.server.post.service;

import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.common.storage.S3ImageStorageService;
import com.runmate.server.post.domain.Post;
import com.runmate.server.post.domain.PostStatus;
import com.runmate.server.post.dto.MyPostFeedResponse.Item;
import com.runmate.server.post.dto.MyPostRow;
import com.runmate.server.post.dto.PostReactionResponse;
import com.runmate.server.post.dto.SavedPostResponse;
import com.runmate.server.post.mapper.BookmarkMapper;
import com.runmate.server.post.mapper.PostLikeMapper;
import com.runmate.server.post.mapper.PostMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReactionService {
    private static final int PAGE_SIZE = 9;

    private final PostMapper postMapper;
    private final PostLikeMapper postLikeMapper;
    private final BookmarkMapper bookmarkMapper;
    private final S3ImageStorageService storageService;

    @Transactional(readOnly = true)
    public PostReactionResponse state(Long viewerId, Long postId) {
        requirePublished(postId);
        return new PostReactionResponse(
                postLikeMapper.countByPostId(postId),
                viewerId != null && postLikeMapper.exists(postId, viewerId) > 0,
                viewerId != null && bookmarkMapper.exists(postId, viewerId) > 0
        );
    }

    @Transactional
    public PostReactionResponse like(Long userId, Long postId) {
        requirePublished(postId);
        if (postLikeMapper.exists(postId, userId) == 0) {
            postLikeMapper.insert(postId, userId);
        }
        return state(userId, postId);
    }

    @Transactional
    public PostReactionResponse unlike(Long userId, Long postId) {
        requirePublished(postId);
        postLikeMapper.delete(postId, userId);
        return state(userId, postId);
    }

    @Transactional
    public PostReactionResponse mark(Long userId, Long postId) {
        requirePublished(postId);
        if (bookmarkMapper.exists(postId, userId) == 0) {
            bookmarkMapper.insert(postId, userId);
        }
        return state(userId, postId);
    }

    @Transactional
    public PostReactionResponse unmark(Long userId, Long postId) {
        requirePublished(postId);
        bookmarkMapper.delete(postId, userId);
        return state(userId, postId);
    }

    @Transactional(readOnly = true)
    public SavedPostResponse saved(Long userId, Integer requestedPage) {
        int page = Math.max(requestedPage == null ? 0 : requestedPage, 0);
        long total = bookmarkMapper.countMine(userId);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / PAGE_SIZE);
        if (totalPages > 0 && page >= totalPages) {
            page = totalPages - 1;
        }
        List<Item> posts = bookmarkMapper.searchMine(userId, PAGE_SIZE, page * PAGE_SIZE).stream()
                .map(this::toItem)
                .toList();
        return new SavedPostResponse(posts, page, totalPages, total);
    }

    private Item toItem(MyPostRow row) {
        return new Item(
                row.postId(),
                row.categoryName(),
                row.title(),
                row.summary(),
                row.status(),
                row.publishedAt(),
                row.createdAt(),
                row.storageKey() == null ? null : storageService.createViewUrl(row.storageKey()),
                row.distanceKm(),
                row.durationSeconds(),
                row.region(),
                row.likeCount(),
                row.commentCount()
        );
    }

    private void requirePublished(Long postId) {
        Post post = postMapper.findById(postId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));
        if (post.getStatus() != PostStatus.PUBLISHED || post.getDeletedAt() != null) {
            throw new AuthException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다.");
        }
    }
}
