package com.runmate.server.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.common.storage.S3ImageStorageService;
import com.runmate.server.post.domain.Category;
import com.runmate.server.post.domain.Post;
import com.runmate.server.post.domain.PostStatus;
import com.runmate.server.post.domain.RunningIntensity;
import com.runmate.server.post.domain.RunningRecord;
import com.runmate.server.post.dto.CreatePostRequest;
import com.runmate.server.post.dto.CreatePostResponse;
import com.runmate.server.post.mapper.CategoryMapper;
import com.runmate.server.post.mapper.PostImageMapper;
import com.runmate.server.post.mapper.PostMapper;
import com.runmate.server.post.mapper.RunningRecordMapper;
import com.runmate.server.user.mapper.UserMapper;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {
    @Mock private PostMapper postRepository;
    @Mock private CategoryMapper categoryRepository;
    @Mock private RunningRecordMapper runningRecordRepository;
    @Mock private PostImageMapper postImageRepository;
    @Mock private UserMapper userRepository;
    @Mock private S3ImageStorageService storageService;
    @Mock private JdbcTemplate jdbcTemplate;

    private PostService postService;

    @BeforeEach
    void setUp() {
        postService = new PostService(postRepository, categoryRepository, runningRecordRepository,
                postImageRepository, userRepository, storageService, jdbcTemplate);
    }

    @Test
    void createsPublishedPostWithRunningRecord() {
        Category category = category(1L, "러닝 인증", true);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(postRepository.saveAndFlush(any(Post.class))).thenAnswer(invocation -> {
            Post saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "postId", 10L);
            return saved;
        });

        CreatePostRequest.RunningRecordRequest running = new CreatePostRequest.RunningRecordRequest(
                LocalDate.now(), new BigDecimal("5.000"), 1800, "한강공원", "서울",
                RunningIntensity.NORMAL, "맑음");
        CreatePostRequest request = new CreatePostRequest(1L, "저녁 러닝", null,
                "오늘은 5km를 달렸습니다.", running, List.of(), PostStatus.PUBLISHED);

        CreatePostResponse response = postService.create(3L, request);

        assertThat(response.postId()).isEqualTo(10L);
        verify(runningRecordRepository).save(any(RunningRecord.class));
    }

    @Test
    void rejectsRunningVerificationWithoutRunningRecord() {
        Category category = category(1L, "러닝 인증", true);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        CreatePostRequest request = new CreatePostRequest(1L, "제목", null, "본문",
                null, List.of(), PostStatus.PUBLISHED);

        assertThatThrownBy(() -> postService.create(3L, request))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("러닝 기록");
        verify(postRepository, never()).saveAndFlush(any(Post.class));
    }

    @Test
    void rejectsInactiveCategory() {
        Category category = category(1L, "자유", false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        CreatePostRequest request = new CreatePostRequest(1L, "제목", null, "본문",
                null, List.of(), PostStatus.DRAFT);

        assertThatThrownBy(() -> postService.create(3L, request))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("사용할 수 없는 카테고리");
        verify(postRepository, never()).saveAndFlush(any(Post.class));
    }

    private Category category(Long id, String name, boolean active) {
        Category category = org.mockito.Mockito.mock(Category.class);
        lenient().when(category.getCategoryId()).thenReturn(id);
        lenient().when(category.getName()).thenReturn(name);
        lenient().when(category.getActive()).thenReturn(active);
        return category;
    }
}

