package com.runmate.server.post.service;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.post.domain.Category;
import com.runmate.server.post.domain.Post;
import com.runmate.server.post.domain.PostStatus;
import com.runmate.server.post.domain.RunningRecord;
import com.runmate.server.post.dto.CreatePostRequest;
import com.runmate.server.post.dto.CreatePostResponse;
import com.runmate.server.post.dto.PostDetailResponse;
import com.runmate.server.post.dto.PostImageResponse;
import com.runmate.server.post.repository.CategoryRepository;
import com.runmate.server.post.repository.PostImageRepository;
import com.runmate.server.post.repository.PostRepository;
import com.runmate.server.post.repository.RunningRecordRepository;
import com.runmate.server.user.repository.UserRepository;
import com.runmate.server.common.storage.S3ImageStorageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostService {
    private static final String RUNNING_VERIFICATION_CATEGORY = "러닝 인증";

    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final RunningRecordRepository runningRecordRepository;
    private final PostImageRepository postImageRepository;
    private final UserRepository userRepository;
    private final S3ImageStorageService storageService;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public CreatePostResponse create(Long authorId, CreatePostRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new AuthException(HttpStatus.BAD_REQUEST, "존재하지 않는 카테고리입니다."));
        if (!Boolean.TRUE.equals(category.getActive())) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "현재 사용할 수 없는 카테고리입니다.");
        }
        if (RUNNING_VERIFICATION_CATEGORY.equals(category.getName()) && request.runningRecord() == null) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "러닝 인증 게시글에는 러닝 기록이 반드시 필요합니다.");
        }

        PostStatus status = request.status() == null ? PostStatus.PUBLISHED : request.status();
        if (status != PostStatus.DRAFT && status != PostStatus.PUBLISHED) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "게시글은 임시저장 또는 공개 상태로만 작성할 수 있습니다.");
        }

        String title = request.title().trim();
        String content = request.content().trim();
        String summary = createSummary(request.summary(), content);
        Post post = postRepository.saveAndFlush(
                Post.create(authorId, category.getCategoryId(), title, summary, content, status));

        if (request.runningRecord() != null) {
            CreatePostRequest.RunningRecordRequest record = request.runningRecord();
            runningRecordRepository.save(RunningRecord.create(post.getPostId(), record.runningDate(),
                    record.distanceKm(), record.durationSeconds(), trimToNull(record.locationName()),
                    trimToNull(record.region()), record.intensity(), trimToNull(record.weather())));
        }

        saveTags(post.getPostId(), request.tags());
        return new CreatePostResponse(post.getPostId());
    }

    @Transactional(readOnly = true)
    public PostDetailResponse detail(Long viewerId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));
        boolean owner = post.getAuthorId().equals(viewerId);
        if (!owner && post.getStatus() != PostStatus.PUBLISHED) {
            throw new AuthException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다.");
        }
        Category category = categoryRepository.findById(post.getCategoryId())
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."));
        String nickname = userRepository.findById(post.getAuthorId())
                .map(user -> user.getNickname()).orElse("탈퇴한 회원");
        PostDetailResponse.RunningRecordDetail running = runningRecordRepository.findByPostId(postId)
                .map(record -> new PostDetailResponse.RunningRecordDetail(record.getRunningDate(),
                        record.getDistanceKm(), record.getDurationSeconds(), record.getLocationName(),
                        record.getRegion(), record.getIntensity().name(), record.getWeather()))
                .orElse(null);
        List<String> tags = jdbcTemplate.query(
                "SELECT t.display_name FROM tags t JOIN post_tags pt ON pt.tag_id=t.tag_id WHERE pt.post_id=? ORDER BY t.tag_id",
                (resultSet, rowNumber) -> resultSet.getString(1), postId);
        List<PostImageResponse> images = postImageRepository.findByPostIdOrderByDisplayOrderAscImageIdAsc(postId)
                .stream().map(image -> new PostImageResponse(image.getImageId(), image.getOriginalName(),
                        image.getContentType(), image.getFileSize(), image.getDisplayOrder(),
                        storageService.createViewUrl(image.getStorageKey()))).toList();
        return new PostDetailResponse(post.getPostId(), post.getAuthorId(), nickname,
                category.getCategoryId(), category.getName(), post.getTitle(), post.getContent(),
                post.getStatus().name(), post.getPublishedAt(), post.getCreatedAt(), running, tags, images);
    }

    private void saveTags(Long postId, List<String> requestedTags) {
        if (requestedTags == null || requestedTags.isEmpty()) return;

        LinkedHashMap<String, String> tags = new LinkedHashMap<>();
        for (String requestedTag : requestedTags) {
            String displayName = normalizeDisplayName(requestedTag);
            String normalizedName = Normalizer.normalize(displayName, Normalizer.Form.NFKC)
                    .toLowerCase(Locale.ROOT);
            tags.putIfAbsent(normalizedName, displayName);
        }
        if (tags.size() > 10) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "태그는 최대 10개까지 등록할 수 있습니다.");
        }

        tags.forEach((normalizedName, displayName) -> {
            jdbcTemplate.update("""
                    INSERT INTO tags (display_name, normalized_name)
                    VALUES (?, ?)
                    ON DUPLICATE KEY UPDATE tag_id = LAST_INSERT_ID(tag_id)
                    """, displayName, normalizedName);
            Long tagId = jdbcTemplate.queryForObject(
                    "SELECT tag_id FROM tags WHERE normalized_name = ?", Long.class, normalizedName);
            jdbcTemplate.update("INSERT IGNORE INTO post_tags (post_id, tag_id) VALUES (?, ?)", postId, tagId);
        });
    }

    private String createSummary(String requestedSummary, String content) {
        String summary = trimToNull(requestedSummary);
        if (summary == null) summary = content.replaceAll("\\s+", " ");
        return summary.length() <= 500 ? summary : summary.substring(0, 500);
    }

    private String normalizeDisplayName(String tag) {
        String value = trimToNull(tag);
        if (value == null) throw new AuthException(HttpStatus.BAD_REQUEST, "빈 태그는 등록할 수 없습니다.");
        value = value.replaceFirst("^#+", "").trim();
        if (value.isBlank()) throw new AuthException(HttpStatus.BAD_REQUEST, "빈 태그는 등록할 수 없습니다.");
        if (value.length() > 50) throw new AuthException(HttpStatus.BAD_REQUEST, "태그는 50자 이하로 입력해 주세요.");
        return value;
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
