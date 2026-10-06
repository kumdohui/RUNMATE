package com.runmate.server.course.service;

import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.common.storage.S3ImageStorageService;
import com.runmate.server.course.dto.CourseListResponse;
import com.runmate.server.course.dto.CourseListResponse.CourseItemResponse;
import com.runmate.server.course.dto.CourseSearchCondition;
import com.runmate.server.course.dto.CourseSearchRequest;
import com.runmate.server.course.dto.CourseSearchRow;
import com.runmate.server.course.mapper.CourseMapper;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseService {
    private static final int PAGE_SIZE = 9;
    private final CourseMapper courseMapper;
    private final S3ImageStorageService imageStorageService;

    @Transactional(readOnly = true)
    public CourseListResponse list(CourseSearchRequest request) {
        validateDistances(request.minDistance(), request.maxDistance());

        int page = Math.max(request.page() == null ? 0 : request.page(), 0);
        String sort = normalizeSort(request.sort());
        CourseSearchCondition countCondition = createCondition(request, sort, 0);
        long totalElements = courseMapper.count(countCondition);
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / PAGE_SIZE);
        if (totalPages > 0 && page >= totalPages) {
            page = totalPages - 1;
        }

        CourseSearchCondition searchCondition = createCondition(
                request,
                sort,
                page * PAGE_SIZE
        );
        List<CourseItemResponse> courses = courseMapper.search(searchCondition).stream()
                .map(this::toResponse)
                .toList();
        return new CourseListResponse(courses, page, totalPages, totalElements);
    }

    private CourseSearchCondition createCondition(
            CourseSearchRequest request,
            String sort,
            int offset
    ) {
        return new CourseSearchCondition(
                trimToNull(request.keyword()),
                request.categoryId(),
                trimToNull(request.region()),
                normalizeTags(trimToNull(request.tag())),
                request.minDistance(),
                request.maxDistance(),
                sort,
                PAGE_SIZE,
                offset
        );
    }

    private CourseItemResponse toResponse(CourseSearchRow row) {
        String imageUrl = row.storageKey() == null
                ? null
                : imageStorageService.createViewUrl(row.storageKey());

        return new CourseItemResponse(
                row.postId(),
                row.categoryName(),
                row.title(),
                row.summary(),
                row.authorNickname(),
                imageUrl,
                row.distanceKm(),
                row.durationSeconds(),
                row.locationName(),
                row.region(),
                row.likeCount(),
                row.commentCount(),
                row.authorId()
        );
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

    private List<String> normalizeTags(String value) {
        if (value == null) return List.of();
        return List.of(value.split(","))
                .stream()
                .map(tag -> Normalizer.normalize(tag, Normalizer.Form.NFKC)
                        .toLowerCase(Locale.ROOT)
                        .trim()
                        .replaceFirst("^#+", "")
                        .replaceAll("\\s+", ""))
                .filter(tag -> !tag.isBlank())
                .distinct()
                .limit(10)
                .toList();
    }

    private String normalizeSort(String sort) {
        return switch (sort == null ? "latest" : sort) {
            case "oldest", "distanceAsc", "distanceDesc", "reactions" -> sort;
            default -> "latest";
        };
    }
}
