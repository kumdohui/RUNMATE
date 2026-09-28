package com.runmate.server.post.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.common.storage.S3ImageStorageService;
import com.runmate.server.notification.service.NotificationService;
import com.runmate.server.post.domain.Post;
import com.runmate.server.post.domain.PostStatus;
import com.runmate.server.post.mapper.BookmarkMapper;
import com.runmate.server.post.mapper.CommentMapper;
import com.runmate.server.post.mapper.PostLikeMapper;
import com.runmate.server.post.mapper.PostMapper;
import com.runmate.server.user.domain.User;
import com.runmate.server.user.domain.UserStatus;
import com.runmate.server.user.mapper.UserMapper;
import com.runmate.server.user.service.ProfileImageUrlResolver;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ContentVisibilityTest {

    @Test
    void blocksCommentsOnSuspendedAuthorsPost() {
        PostMapper postMapper = mock(PostMapper.class);
        CommentMapper commentMapper = mock(CommentMapper.class);
        UserMapper userMapper = suspendedAuthor(postMapper);
        CommentService service = new CommentService(
                commentMapper,
                postMapper,
                mock(ProfileImageUrlResolver.class),
                mock(NotificationService.class),
                userMapper
        );

        assertThatThrownBy(() -> service.list(null, 10L))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("게시글을 찾을 수 없습니다");
        verify(commentMapper, never()).findByPostId(10L);
    }

    @Test
    void blocksReactionsOnSuspendedAuthorsPost() {
        PostMapper postMapper = mock(PostMapper.class);
        PostLikeMapper likeMapper = mock(PostLikeMapper.class);
        UserMapper userMapper = suspendedAuthor(postMapper);
        ReactionService service = new ReactionService(
                postMapper,
                likeMapper,
                mock(BookmarkMapper.class),
                mock(S3ImageStorageService.class),
                userMapper
        );

        assertThatThrownBy(() -> service.state(3L, 10L))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("게시글을 찾을 수 없습니다");
        verify(likeMapper, never()).countByPostId(10L);
    }

    private UserMapper suspendedAuthor(PostMapper postMapper) {
        Post post = Post.create(8L, 1L, "제목", "요약", "본문", PostStatus.PUBLISHED);
        User author = mock(User.class);
        when(author.getStatus()).thenReturn(UserStatus.SUSPENDED);
        UserMapper userMapper = mock(UserMapper.class);
        when(postMapper.findById(10L)).thenReturn(Optional.of(post));
        when(userMapper.findById(8L)).thenReturn(Optional.of(author));
        return userMapper;
    }
}
