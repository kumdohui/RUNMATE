package com.runmate.server.post.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PostLikeMapper {
    int insert(@Param("postId") Long postId, @Param("userId") Long userId);

    int delete(@Param("postId") Long postId, @Param("userId") Long userId);

    int exists(@Param("postId") Long postId, @Param("userId") Long userId);

    long countByPostId(Long postId);
}
