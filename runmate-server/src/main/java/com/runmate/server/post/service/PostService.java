package com.runmate.server.post.service;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.post.domain.Category;
import com.runmate.server.post.domain.Post;
import com.runmate.server.post.domain.PostStatus;
import com.runmate.server.post.domain.RunningRecord;
import com.runmate.server.post.dto.CreatePostRequest;
import com.runmate.server.post.dto.CreatePostResponse;
import com.runmate.server.post.dto.MyPostFeedResponse;
import com.runmate.server.post.dto.MyPostFeedResponse.Item;
import com.runmate.server.post.dto.MyPostSearchCondition;
import com.runmate.server.post.dto.MyPostSummary;
import com.runmate.server.post.dto.PostDetailResponse;
import com.runmate.server.post.dto.PostImageResponse;
import com.runmate.server.post.mapper.BookmarkMapper;
import com.runmate.server.post.mapper.CategoryMapper;
import com.runmate.server.post.mapper.CommentMapper;
import com.runmate.server.post.mapper.PostImageMapper;
import com.runmate.server.post.mapper.PostMapper;
import com.runmate.server.post.mapper.RunningRecordMapper;
import com.runmate.server.post.mapper.TagMapper;
import com.runmate.server.user.domain.User;
import com.runmate.server.user.domain.UserRole;
import com.runmate.server.user.domain.UserStatus;
import com.runmate.server.user.mapper.UserMapper;
import com.runmate.server.common.storage.S3ImageStorageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostService {
    private static final String RUNNING_VERIFICATION_CATEGORY = "러닝 인증";
    private static final int MY_FEED_PAGE_SIZE = 9;

    private final PostMapper postMapper;
    private final CategoryMapper categoryMapper;
    private final RunningRecordMapper runningRecordMapper;
    private final PostImageMapper postImageMapper;
    private final UserMapper userMapper;
    private final S3ImageStorageService storageService;
    private final TagMapper tagMapper;
    private final BookmarkMapper bookmarkMapper;
    private final CommentMapper commentMapper;

    @Transactional
    public CreatePostResponse create(Long authorId, CreatePostRequest request) {
        Category category = categoryMapper.findById(request.categoryId())
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
        Post post = postMapper.saveAndFlush(
                Post.create(authorId, category.getCategoryId(), title, summary, content, status));

        if (request.runningRecord() != null) {
            CreatePostRequest.RunningRecordRequest record = request.runningRecord();
            runningRecordMapper.save(RunningRecord.create(post.getPostId(), record.runningDate(),
                    record.distanceKm(), record.durationSeconds(), trimToNull(record.locationName()),
                    trimToNull(record.region()), record.intensity(), trimToNull(record.weather())));
        }

        saveTags(post.getPostId(), request.tags());
        return new CreatePostResponse(post.getPostId());
    }

    @Transactional
    public CreatePostResponse update(
            Long authorId,
            Long postId,
            CreatePostRequest request
    ) {
        Post post = requireOwner(authorId, postId);
        Category category = requireCategory(request.categoryId());
        validateRequest(category, request);

        PostStatus status = resolveStatus(request.status());
        String content = request.content().trim();
        post.update(
                category.getCategoryId(),
                request.title().trim(),
                createSummary(request.summary(), content),
                content,
                status
        );
        postMapper.update(post);

        runningRecordMapper.deleteByPostId(postId);
        saveRunningRecord(postId, request.runningRecord());
        tagMapper.deleteConnectionsByPostId(postId);
        saveTags(postId, request.tags());
        return new CreatePostResponse(postId);
    }

    @Transactional
    public void delete(Long authorId, Long postId) {
        Post post = requireOwner(authorId, postId);
        post.delete();
        postMapper.update(post);
    }

    @Transactional(readOnly = true)
    public MyPostFeedResponse myFeed(
            Long authorId,
            Integer requestedPage,
            String requestedSort,
            String requestedKeyword,
            String requestedStatus
    ) {
        String keyword = trimToNull(requestedKeyword);
        if (keyword != null && keyword.length() < 2) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "검색어는 두 글자 이상 입력해 주세요.");
        }
        String status = normalizeMyFeedStatus(requestedStatus);
        String sort = "oldest".equals(requestedSort) ? "oldest" : "latest";
        int page = Math.max(requestedPage == null ? 0 : requestedPage, 0);
        MyPostSearchCondition countCondition = new MyPostSearchCondition(
                authorId, keyword, status, sort, MY_FEED_PAGE_SIZE, 0);
        long totalElements = postMapper.countMine(countCondition);
        int totalPages = totalElements == 0
                ? 0
                : (int) Math.ceil((double) totalElements / MY_FEED_PAGE_SIZE);
        if (totalPages > 0 && page >= totalPages) {
            page = totalPages - 1;
        }
        List<Item> posts = postMapper.searchMine(new MyPostSearchCondition(
                authorId, keyword, status, sort, MY_FEED_PAGE_SIZE, page * MY_FEED_PAGE_SIZE))
                .stream()
                .map(row -> new Item(
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
                        row.commentCount()))
                .toList();
        MyPostSummary summary = postMapper.summarizeMine(authorId);
        double totalDistanceKm = summary == null || summary.totalDistanceKm() == null
                ? 0
                : summary.totalDistanceKm();
        long totalDurationSeconds = summary == null || summary.totalDurationSeconds() == null
                ? 0
                : summary.totalDurationSeconds();
        long publishedCount = summary == null ? 0 : summary.publishedCount();
        long draftCount = summary == null ? 0 : summary.draftCount();
        return new MyPostFeedResponse(
                posts,
                page,
                totalPages,
                totalElements,
                totalDistanceKm,
                totalDurationSeconds,
                publishedCount,
                draftCount,
                bookmarkMapper.countByUser(authorId),
                commentMapper.countByAuthor(authorId)
        );
    }

    @Transactional(readOnly = true)
    public PostDetailResponse detail(Long viewerId, Long postId) {
        Post post = postMapper.findById(postId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));
        if (post.getStatus() == PostStatus.DELETED) {
            throw new AuthException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다.");
        }
        boolean owner = viewerId != null && post.getAuthorId().equals(viewerId);
        if (!owner && post.getStatus() != PostStatus.PUBLISHED) {
            throw new AuthException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다.");
        }
        Category category = categoryMapper.findById(post.getCategoryId())
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."));
        User author = userMapper.findById(post.getAuthorId()).orElse(null);
        if (author != null && author.getStatus() == UserStatus.SUSPENDED && !owner) {
            User viewer = viewerId == null ? null : userMapper.findById(viewerId).orElse(null);
            if (viewer == null || viewer.getRole() != UserRole.ADMIN) {
                throw new AuthException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다.");
            }
        }
        String nickname = author == null ? "탈퇴한 사용자" : displayNickname(author);
        boolean authorAdmin = author != null && author.getRole() == UserRole.ADMIN;
        PostDetailResponse.RunningRecordDetail running = runningRecordMapper.findByPostId(postId)
                .map(record -> new PostDetailResponse.RunningRecordDetail(record.getRunningDate(),
                        record.getDistanceKm(), record.getDurationSeconds(), record.getLocationName(),
                        record.getRegion(), record.getIntensity().name(), record.getWeather()))
                .orElse(null);
        List<String> tags = tagMapper.findNamesByPostId(postId);
        List<PostImageResponse> images = postImageMapper.findByPostIdOrderByDisplayOrderAscImageIdAsc(postId)
                .stream().map(image -> new PostImageResponse(image.getImageId(), image.getOriginalName(),
                        image.getContentType(), image.getFileSize(), image.getDisplayOrder(),
                        storageService.createViewUrl(image.getStorageKey()))).toList();
        return new PostDetailResponse(post.getPostId(), post.getAuthorId(), nickname,
                category.getCategoryId(), category.getName(), post.getTitle(), post.getContent(),
                post.getStatus().name(), owner, post.getPublishedAt(), post.getCreatedAt(),
                running, tags, images, authorAdmin);
    }

    private String displayNickname(User user) {
        if (user.getStatus() == UserStatus.DELETED || user.getDeletedAt() != null) {
            return "탈퇴한 사용자";
        }
        return user.getNickname();
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
            tagMapper.upsert(displayName, normalizedName);
            Long tagId = tagMapper.findIdByNormalizedName(normalizedName);
            tagMapper.connect(postId, tagId);
        });
    }

    private Post requireOwner(Long authorId, Long postId) {
        Post post = postMapper.findById(postId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));
        if (!post.getAuthorId().equals(authorId)) {
            throw new AuthException(HttpStatus.FORBIDDEN, "게시글 작성자만 수정하거나 삭제할 수 있습니다.");
        }
        if (post.getStatus() == PostStatus.DELETED) {
            throw new AuthException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다.");
        }
        return post;
    }

    private Category requireCategory(Long categoryId) {
        Category category = categoryMapper.findById(categoryId)
                .orElseThrow(() -> new AuthException(HttpStatus.BAD_REQUEST, "존재하지 않는 카테고리입니다."));
        if (!Boolean.TRUE.equals(category.getActive())) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "현재 사용할 수 없는 카테고리입니다.");
        }
        return category;
    }

    private void validateRequest(Category category, CreatePostRequest request) {
        if (RUNNING_VERIFICATION_CATEGORY.equals(category.getName())
                && request.runningRecord() == null) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "러닝 인증 게시글에는 러닝 기록이 반드시 필요합니다.");
        }
    }

    private PostStatus resolveStatus(PostStatus status) {
        PostStatus resolved = status == null ? PostStatus.PUBLISHED : status;
        if (resolved != PostStatus.DRAFT && resolved != PostStatus.PUBLISHED) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "게시글은 임시저장 또는 공개 상태로만 저장할 수 있습니다.");
        }
        return resolved;
    }

    private void saveRunningRecord(
            Long postId,
            CreatePostRequest.RunningRecordRequest record
    ) {
        if (record == null) return;
        runningRecordMapper.save(RunningRecord.create(
                postId,
                record.runningDate(),
                record.distanceKm(),
                record.durationSeconds(),
                trimToNull(record.locationName()),
                trimToNull(record.region()),
                record.intensity(),
                trimToNull(record.weather())
        ));
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

    private String normalizeMyFeedStatus(String status) {
        String value = trimToNull(status);
        if (value == null || "ALL".equalsIgnoreCase(value)) return null;
        if ("PUBLISHED".equalsIgnoreCase(value) || "DRAFT".equalsIgnoreCase(value)) {
            return value.toUpperCase(Locale.ROOT);
        }
        throw new AuthException(HttpStatus.BAD_REQUEST, "게시글 상태는 전체, 공개, 임시저장 중에서 선택해 주세요.");
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}


