package com.runmate.server.post.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TagMapper {
    List<String> findNamesByPostId(Long postId);

    Long findIdByNormalizedName(String normalizedName);

    int upsert(
            @Param("displayName") String displayName,
            @Param("normalizedName") String normalizedName
    );

    int connect(
            @Param("postId") Long postId,
            @Param("tagId") Long tagId
    );

    int deleteConnectionsByPostId(Long postId);
}
