package com.runmate.server.user.service;

import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.common.storage.S3ImageStorageService;
import com.runmate.server.post.mapper.PostMapper;
import com.runmate.server.user.domain.User;
import com.runmate.server.user.domain.UserRole;
import com.runmate.server.user.domain.UserStatus;
import com.runmate.server.user.dto.PublicProfileDtos.PublicPostFeedResponse;
import com.runmate.server.user.dto.PublicProfileDtos.PublicPostItem;
import com.runmate.server.user.dto.PublicProfileDtos.PublicProfileResponse;
import com.runmate.server.user.mapper.UserMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PublicProfileService {
    private static final int PAGE_SIZE = 9;

    private final UserMapper userMapper;
    private final PostMapper postMapper;
    private final ProfileImageUrlResolver profileImageUrlResolver;
    private final S3ImageStorageService imageStorageService;

    @Transactional(readOnly = true)
    public PublicProfileResponse profile(Long userId) {
        User user = requireUser(userId);
        boolean withdrawn = user.getStatus() == UserStatus.DELETED || user.getDeletedAt() != null;
        if (withdrawn) {
            return new PublicProfileResponse(user.getUserId(), "탈퇴한 사용자", null, null, true, false, false);
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            return new PublicProfileResponse(user.getUserId(), user.getNickname(), null, null, false, false, true);
        }
        return new PublicProfileResponse(
                user.getUserId(),
                user.getNickname(),
                user.getBio(),
                profileImageUrlResolver.resolve(user),
                false,
                user.getRole() == UserRole.ADMIN,
                false
        );
    }

    @Transactional(readOnly = true)
    public PublicPostFeedResponse posts(Long userId, Integer requestedPage) {
        User user = requireUser(userId);
        if (user.getStatus() == UserStatus.SUSPENDED) {
            return new PublicPostFeedResponse(List.of(), 0, 0, 0);
        }
        int page = Math.max(requestedPage == null ? 0 : requestedPage, 0);
        long total = postMapper.countPublishedByAuthor(userId);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / PAGE_SIZE);
        if (totalPages > 0 && page >= totalPages) {
            page = totalPages - 1;
        }
        List<PublicPostItem> posts = total == 0
                ? List.of()
                : postMapper.searchPublishedByAuthor(userId, PAGE_SIZE, page * PAGE_SIZE).stream()
                        .map(row -> new PublicPostItem(
                                row.postId(),
                                row.categoryName(),
                                row.title(),
                                row.summary(),
                                row.storageKey() == null ? null : imageStorageService.createViewUrl(row.storageKey()),
                                row.distanceKm(),
                                row.durationSeconds(),
                                row.likeCount(),
                                row.commentCount()
                        ))
                        .toList();
        return new PublicPostFeedResponse(posts, page, totalPages, total);
    }

    private User requireUser(Long userId) {
        return userMapper.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."));
    }
}
