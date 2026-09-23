package com.runmate.server.post.mapper;

import com.runmate.server.post.dto.MyPostRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BookmarkMapper {
    int insert(@Param("postId") Long postId, @Param("userId") Long userId);

    int delete(@Param("postId") Long postId, @Param("userId") Long userId);

    int exists(@Param("postId") Long postId, @Param("userId") Long userId);

    long countByUser(Long userId);

    long countMine(Long userId);

    List<MyPostRow> searchMine(@Param("userId") Long userId, @Param("limit") int limit,
            @Param("offset") int offset);
}
