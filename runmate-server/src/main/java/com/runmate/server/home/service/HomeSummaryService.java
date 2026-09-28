package com.runmate.server.home.service;

import com.runmate.server.common.storage.S3ImageStorageService;
import com.runmate.server.home.dto.HomeSummaryResponse;
import com.runmate.server.home.dto.HomeSummaryResponse.PopularCourseResponse;
import com.runmate.server.home.dto.HomeSummaryResponse.RecentPostResponse;
import com.runmate.server.home.mapper.HomeMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HomeSummaryService {
    private static final int FEED_PAGE_SIZE = 9;
    private final HomeMapper homeMapper;
    private final S3ImageStorageService imageStorageService;

    @Transactional(readOnly = true)
    public HomeSummaryResponse getSummary(int requestedPage, String requestedSort) {
        int page = Math.max(requestedPage, 0);
        String sort = switch (requestedSort == null ? "latest" : requestedSort) {
            case "oldest", "views", "likes" -> requestedSort;
            default -> "latest";
        };
        long activeMemberCount = homeMapper.countActiveMembers();
        List<PopularCourseResponse> popularCourses = homeMapper.findPopularCourses().stream()
                .map(row -> new PopularCourseResponse(
                        row.postId(),
                        row.title(),
                        row.location(),
                        row.region(),
                        row.distanceKm(),
                        row.durationSeconds(),
                        row.likeCount(),
                        row.commentCount()
                ))
                .toList();
        long totalPosts = homeMapper.countPublishedPosts();
        int totalPages = totalPosts == 0 ? 0 : (int) Math.ceil((double) totalPosts / FEED_PAGE_SIZE);
        if (totalPages > 0 && page >= totalPages) {
            page = totalPages - 1;
        }
        List<RecentPostResponse> recentPosts = homeMapper
                .findRecentPosts(sort, FEED_PAGE_SIZE, page * FEED_PAGE_SIZE)
                .stream()
                .map(row -> new RecentPostResponse(
                        row.postId(),
                        row.categoryName(),
                        row.title(),
                        row.summary(),
                        row.authorNickname(),
                        row.storageKey() == null ? null : imageStorageService.createViewUrl(row.storageKey()),
                        row.distanceKm(),
                        row.durationSeconds(),
                        row.likeCount(),
                        row.commentCount(),
                        row.authorId()
                ))
                .toList();
        return new HomeSummaryResponse(
                activeMemberCount,
                popularCourses,
                recentPosts,
                page,
                totalPages,
                totalPosts
        );
    }
}
