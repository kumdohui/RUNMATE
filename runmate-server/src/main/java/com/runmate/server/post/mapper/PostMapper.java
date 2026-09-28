package com.runmate.server.post.mapper;

import com.runmate.server.post.domain.Post;
import com.runmate.server.post.dto.MyPostRow;
import com.runmate.server.post.dto.MyPostSearchCondition;
import com.runmate.server.post.dto.MyPostSummary;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PostMapper {
    Optional<Post> findById(Long postId);
    int insert(Post post);
    int update(Post post);
    long countMine(MyPostSearchCondition condition);
    List<MyPostRow> searchMine(MyPostSearchCondition condition);
    MyPostSummary summarizeMine(Long authorId);

    long countPublishedByAuthor(Long authorId);

    List<MyPostRow> searchPublishedByAuthor(@Param("authorId") Long authorId, @Param("limit") int limit,
            @Param("offset") int offset);

    default Post saveAndFlush(Post post) {
        if (post.getPostId() == null) insert(post);
        else update(post);
        return post;
    }
}


