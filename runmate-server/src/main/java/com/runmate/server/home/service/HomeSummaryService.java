package com.runmate.server.home.service;

import com.runmate.server.home.dto.HomeSummaryResponse;
import com.runmate.server.home.dto.HomeSummaryResponse.PopularCourseResponse;
import com.runmate.server.home.dto.HomeSummaryResponse.RecentPostResponse;
import com.runmate.server.common.storage.S3ImageStorageService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HomeSummaryService {
    private static final int FEED_PAGE_SIZE = 9;
    private final JdbcTemplate jdbcTemplate;
    private final S3ImageStorageService imageStorageService;

    @Transactional(readOnly = true)
    public HomeSummaryResponse getSummary(int requestedPage, String requestedSort) {
        int page = Math.max(requestedPage, 0);
        String orderBy = switch (requestedSort == null ? "latest" : requestedSort) {
            case "oldest" -> "p.published_at ASC, p.post_id ASC";
            case "views" -> "p.view_count DESC, p.published_at DESC, p.post_id DESC";
            case "likes" -> "like_count DESC, p.published_at DESC, p.post_id DESC";
            default -> "p.published_at DESC, p.post_id DESC";
        };
        Long activeMemberCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE status = 'ACTIVE' AND role = 'USER' AND deleted_at IS NULL",
                Long.class
        );

        List<PopularCourseResponse> popularCourses = jdbcTemplate.query("""
                SELECT * FROM (
                    SELECT p.post_id,
                           p.title,
                           COALESCE(rr.location_name, '') AS location_name,
                           COALESCE(rr.region, '') AS region,
                           COALESCE(rr.distance_km, 0) AS distance_km,
                           COALESCE(rr.duration_seconds, 0) AS duration_seconds,
                           (SELECT COUNT(*) FROM post_likes pl WHERE pl.post_id = p.post_id) AS like_count,
                           (SELECT COUNT(*) FROM comments c
                              WHERE c.post_id = p.post_id AND c.deleted_at IS NULL) AS comment_count,
                           p.published_at
                      FROM posts p
                      JOIN categories category ON category.category_id = p.category_id
                      LEFT JOIN running_records rr ON rr.post_id = p.post_id
                     WHERE p.status = 'PUBLISHED'
                       AND p.deleted_at IS NULL
                       AND category.category_id = 2
                ) ranked
                WHERE (like_count + comment_count) > 0
                ORDER BY (like_count + comment_count) DESC, published_at DESC, post_id DESC
                LIMIT 3
                """, (rs, rowNum) -> new PopularCourseResponse(
                rs.getLong("post_id"),
                rs.getString("title"),
                rs.getString("location_name"),
                rs.getString("region"),
                rs.getDouble("distance_km"),
                rs.getInt("duration_seconds"),
                rs.getLong("like_count"),
                rs.getLong("comment_count")
        ));

        Long totalPostsValue = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM posts WHERE status='PUBLISHED' AND deleted_at IS NULL",
                Long.class
        );
        long totalPosts = totalPostsValue == null ? 0 : totalPostsValue;
        int totalPages = totalPosts == 0 ? 0 : (int) Math.ceil((double) totalPosts / FEED_PAGE_SIZE);
        if (totalPages > 0 && page >= totalPages) page = totalPages - 1;

        String recentPostsSql = """
                SELECT p.post_id, category.name AS category_name, p.title, p.summary,
                       u.nickname AS author_nickname,
                       (SELECT pi.storage_key FROM post_images pi
                         WHERE pi.post_id=p.post_id ORDER BY pi.display_order, pi.image_id LIMIT 1) AS storage_key,
                       rr.distance_km, rr.duration_seconds,
                       (SELECT COUNT(*) FROM post_likes pl WHERE pl.post_id=p.post_id) AS like_count,
                       (SELECT COUNT(*) FROM comments c
                         WHERE c.post_id=p.post_id AND c.deleted_at IS NULL) AS comment_count
                  FROM posts p
                  JOIN users u ON u.user_id=p.author_id
                  LEFT JOIN categories category ON category.category_id=p.category_id
                  LEFT JOIN running_records rr ON rr.post_id=p.post_id
                 WHERE p.status='PUBLISHED' AND p.deleted_at IS NULL
                 ORDER BY %s
                 LIMIT ? OFFSET ?
                """.formatted(orderBy);
        List<RecentPostResponse> recentPosts = jdbcTemplate.query(recentPostsSql, (rs, rowNum) -> {
            String storageKey = rs.getString("storage_key");
            Number distance = (Number) rs.getObject("distance_km");
            Number duration = (Number) rs.getObject("duration_seconds");
            return new RecentPostResponse(
                    rs.getLong("post_id"), rs.getString("category_name"), rs.getString("title"),
                    rs.getString("summary"), rs.getString("author_nickname"),
                    storageKey == null ? null : imageStorageService.createViewUrl(storageKey),
                    distance == null ? null : distance.doubleValue(),
                    duration == null ? null : duration.intValue(),
                    rs.getLong("like_count"), rs.getLong("comment_count")
            );
        }, FEED_PAGE_SIZE, page * FEED_PAGE_SIZE);

        return new HomeSummaryResponse(
                activeMemberCount == null ? 0 : activeMemberCount,
                popularCourses,
                recentPosts,
                page,
                totalPages,
                totalPosts
        );
    }
}
