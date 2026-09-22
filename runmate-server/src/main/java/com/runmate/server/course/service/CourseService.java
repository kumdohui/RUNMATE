package com.runmate.server.course.service;

import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.common.storage.S3ImageStorageService;
import com.runmate.server.course.dto.CourseListResponse;
import com.runmate.server.course.dto.CourseListResponse.CourseItemResponse;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseService {
    private static final int PAGE_SIZE = 9;
    private final JdbcTemplate jdbcTemplate;
    private final S3ImageStorageService imageStorageService;

    @Transactional(readOnly = true)
    public CourseListResponse list(int requestedPage, String requestedSort, String rawKeyword,
            Long categoryId, String rawRegion, String rawTag, Double minDistance, Double maxDistance) {
        validateDistances(minDistance, maxDistance);
        int page = Math.max(requestedPage, 0);
        String keyword = trimToNull(rawKeyword);
        String region = trimToNull(rawRegion);
        String tag = trimToNull(rawTag);
        String orderBy = switch (requestedSort == null ? "latest" : requestedSort) {
            case "oldest" -> "p.published_at ASC, p.post_id ASC";
            case "distanceAsc" -> "(rr.distance_km IS NULL) ASC, rr.distance_km ASC, p.published_at DESC, p.post_id DESC";
            case "distanceDesc" -> "(rr.distance_km IS NULL) ASC, rr.distance_km DESC, p.published_at DESC, p.post_id DESC";
            case "reactions" -> "(like_count + comment_count) DESC, p.published_at DESC, p.post_id DESC";
            default -> "p.published_at DESC, p.post_id DESC";
        };

        StringBuilder where = new StringBuilder("""
                WHERE p.status = 'PUBLISHED'
                  AND p.deleted_at IS NULL
                """);
        List<Object> parameters = new ArrayList<>();
        if (keyword != null) {
            where.append("""
                      AND (p.title LIKE ? OR p.summary LIKE ? OR p.content LIKE ?)
                    """);
            String pattern = "%" + keyword + "%";
            for (int index = 0; index < 3; index++) parameters.add(pattern);
        }
        if (categoryId != null) {
            where.append(" AND category.category_id = ?\n");
            parameters.add(categoryId);
        }
        if (region != null) {
            where.append(" AND rr.region = ?\n");
            parameters.add(region);
        }
        if (tag != null) {
            where.append("""
                     AND EXISTS (SELECT 1 FROM post_tags pt JOIN tags t ON t.tag_id=pt.tag_id
                                  WHERE pt.post_id=p.post_id
                                    AND REPLACE(t.normalized_name, ' ', '') LIKE ?)
                    """);
            parameters.add("%" + normalizeTagKeyword(tag) + "%");
        }
        if (minDistance != null) {
            where.append(" AND rr.distance_km >= ?\n");
            parameters.add(minDistance);
        }
        if (maxDistance != null) {
            where.append(" AND rr.distance_km <= ?\n");
            parameters.add(maxDistance);
        }

        Long totalValue = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM posts p
                  JOIN categories category ON category.category_id=p.category_id
                  LEFT JOIN running_records rr ON rr.post_id=p.post_id
                """ + where, Long.class, parameters.toArray());
        long totalElements = totalValue == null ? 0 : totalValue;
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / PAGE_SIZE);
        if (totalPages > 0 && page >= totalPages) page = totalPages - 1;

        String sql = """
                SELECT p.post_id, category.name AS category_name, p.title, p.summary, u.nickname AS author_nickname,
                       (SELECT pi.storage_key FROM post_images pi WHERE pi.post_id=p.post_id
                        ORDER BY pi.display_order, pi.image_id LIMIT 1) AS storage_key,
                       rr.distance_km, rr.duration_seconds, rr.location_name, rr.region,
                       (SELECT COUNT(*) FROM post_likes pl WHERE pl.post_id=p.post_id) AS like_count,
                       (SELECT COUNT(*) FROM comments c WHERE c.post_id=p.post_id AND c.deleted_at IS NULL) AS comment_count
                  FROM posts p
                  JOIN users u ON u.user_id=p.author_id
                  JOIN categories category ON category.category_id=p.category_id
                  LEFT JOIN running_records rr ON rr.post_id=p.post_id
                """ + where + " ORDER BY " + orderBy + " LIMIT ? OFFSET ?";
        List<Object> listParameters = new ArrayList<>(parameters);
        listParameters.add(PAGE_SIZE);
        listParameters.add(page * PAGE_SIZE);
        List<CourseItemResponse> courses = jdbcTemplate.query(sql, (rs, rowNum) -> {
            String storageKey = rs.getString("storage_key");
            Number distance = (Number) rs.getObject("distance_km");
            Number duration = (Number) rs.getObject("duration_seconds");
            return new CourseItemResponse(
                    rs.getLong("post_id"), rs.getString("category_name"),
                    rs.getString("title"), rs.getString("summary"),
                    rs.getString("author_nickname"),
                    storageKey == null ? null : imageStorageService.createViewUrl(storageKey),
                    distance == null ? null : distance.doubleValue(),
                    duration == null ? null : duration.intValue(),
                    rs.getString("location_name"), rs.getString("region"),
                    rs.getLong("like_count"), rs.getLong("comment_count")
            );
        }, listParameters.toArray());
        return new CourseListResponse(courses, page, totalPages, totalElements);
    }

    private void validateDistances(Double minDistance, Double maxDistance) {
        if ((minDistance != null && minDistance < 0) || (maxDistance != null && maxDistance < 0)) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "거리는 0 이상이어야 합니다.");
        }
        if (minDistance != null && maxDistance != null && minDistance > maxDistance) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "최소 거리는 최대 거리보다 클 수 없습니다.");
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private String normalizeTagKeyword(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceFirst("^#+", "")
                .replaceAll("\\s+", "");
    }
}
