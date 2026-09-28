package com.runmate.server.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.argThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import com.runmate.server.post.dto.MyPostFeedResponse;
import com.runmate.server.post.dto.MyPostRow;
import com.runmate.server.post.dto.MyPostSearchCondition;
import com.runmate.server.post.dto.MyPostSummary;
import com.runmate.server.post.mapper.BookmarkMapper;
import com.runmate.server.post.mapper.CategoryMapper;
import com.runmate.server.post.mapper.CommentMapper;
import com.runmate.server.post.mapper.PostImageMapper;
import com.runmate.server.post.mapper.PostMapper;
import com.runmate.server.post.mapper.RunningRecordMapper;
import com.runmate.server.post.mapper.TagMapper;
import com.runmate.server.user.domain.User;
import com.runmate.server.user.mapper.UserMapper;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {
    @Mock private PostMapper postRepository;
    @Mock private CategoryMapper categoryRepository;
    @Mock private RunningRecordMapper runningRecordRepository;
    @Mock private PostImageMapper postImageRepository;
    @Mock private UserMapper userRepository;
    @Mock private S3ImageStorageService storageService;
    @Mock private TagMapper tagMapper;
    @Mock private BookmarkMapper bookmarkMapper;
    @Mock private CommentMapper commentMapper;

    private PostService postService;

    @BeforeEach
    void setUp() {
        postService = new PostService(postRepository, categoryRepository, runningRecordRepository,
                postImageRepository, userRepository, storageService, tagMapper, bookmarkMapper, commentMapper);
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
    void savesIncompleteDraft() {
        Category category = category(1L, "러닝 인증", true);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(postRepository.saveAndFlush(any(Post.class))).thenAnswer(invocation -> {
            Post saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "postId", 11L);
            return saved;
        });
        CreatePostRequest request = new CreatePostRequest(
                1L,
                "",
                null,
                "",
                null,
                List.of(),
                PostStatus.DRAFT
        );

        CreatePostResponse response = postService.create(3L, request);

        assertThat(response.postId()).isEqualTo(11L);
        verify(runningRecordRepository, never()).save(any(RunningRecord.class));
    }

    @Test
    void showsPublishedPostToGuest() {
        Post post = Post.create(3L, 1L, "한강 코스", "요약", "본문", PostStatus.PUBLISHED);
        ReflectionTestUtils.setField(post, "postId", 10L);
        Category courseCategory = category(1L, "코스 추천", true);
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(courseCategory));
        User author = org.mockito.Mockito.mock(User.class);
        when(author.getNickname()).thenReturn("러너");
        when(userRepository.findById(3L)).thenReturn(Optional.of(author));
        when(runningRecordRepository.findByPostId(10L)).thenReturn(Optional.empty());
        when(tagMapper.findNamesByPostId(10L)).thenReturn(List.of());
        when(postImageRepository.findByPostIdOrderByDisplayOrderAscImageIdAsc(10L)).thenReturn(List.of());

        var detail = postService.detail(null, 10L);

        assertThat(detail.owner()).isFalse();
        assertThat(detail.title()).isEqualTo("한강 코스");
    }

    @Test
    void hidesDraftFromGuest() {
        Post post = Post.create(3L, 1L, "임시글", "요약", "본문", PostStatus.DRAFT);
        ReflectionTestUtils.setField(post, "postId", 11L);
        when(postRepository.findById(11L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.detail(null, 11L))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("게시글을 찾을 수 없습니다");
    }

    @Test
    void listsOnlyOwnVisiblePosts() {
        when(postRepository.summarizeMine(3L)).thenReturn(new MyPostSummary(1L, 0L, 1L, 5.0, 1800L));
        when(postRepository.countMine(any(MyPostSearchCondition.class))).thenReturn(1L);
        when(postRepository.searchMine(any(MyPostSearchCondition.class))).thenReturn(List.of(
                new MyPostRow(10L, "러닝 인증", "저녁 러닝", "요약", "DRAFT",
                        null, java.time.LocalDateTime.now(), null, 5.0, 1800, "서울", 0, 0)));

        MyPostFeedResponse response = postService.myFeed(3L, 0, "latest", " 한강 ", "draft");

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.posts()).hasSize(1);
        assertThat(response.posts().get(0).status()).isEqualTo("DRAFT");
        verify(postRepository).countMine(argThat(condition ->
                condition.authorId().equals(3L)
                        && "한강".equals(condition.keyword())
                        && "DRAFT".equals(condition.status())));
    }

    @Test
    void rejectsDeletedStatusInMyFeed() {
        assertThatThrownBy(() -> postService.myFeed(3L, 0, "latest", null, "DELETED"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("게시글 상태");
        verify(postRepository, never()).countMine(any());
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
