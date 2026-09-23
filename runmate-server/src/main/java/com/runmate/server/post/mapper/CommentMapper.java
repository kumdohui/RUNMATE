package com.runmate.server.post.mapper;

import com.runmate.server.post.domain.Comment;
import com.runmate.server.post.dto.CommentRow;
import com.runmate.server.post.dto.MyCommentRow;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommentMapper {
    int insert(Comment comment);

    int update(Comment comment);

    int markDeleted(Long commentId);

    Optional<Comment> findById(Long commentId);

    Optional<CommentRow> findRow(Long commentId);

    List<CommentRow> findByPostId(Long postId);

    long countByPostId(Long postId);

    long countByAuthor(Long authorId);

    long countMine(Long authorId);

    List<MyCommentRow> searchMine(@Param("authorId") Long authorId, @Param("limit") int limit,
            @Param("offset") int offset);
}
