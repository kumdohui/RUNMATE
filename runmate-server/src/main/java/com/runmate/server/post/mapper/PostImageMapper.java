package com.runmate.server.post.mapper;

import java.util.Optional;
import java.util.List;
import com.runmate.server.post.domain.PostImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PostImageMapper {
    Optional<PostImage> findById(Long imageId);
    long countByPostId(Long postId);
    Optional<PostImage> findByImageIdAndPostId(
            @Param("imageId") Long imageId,
            @Param("postId") Long postId
    );
    List<PostImage> findByPostIdOrderByDisplayOrderAscImageIdAsc(Long postId);
    int insert(PostImage image);
    int deleteById(Long imageId);

    default List<PostImage> saveAllAndFlush(List<PostImage> images) {
        images.forEach(this::insert);
        return images;
    }

    default void delete(PostImage image) {
        deleteById(image.getImageId());
    }

    default void flush() {
    }
}


