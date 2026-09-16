package com.runmate.server.post.repository;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.runmate.server.post.domain.PostImage;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {
    long countByPostId(Long postId);
    Optional<PostImage> findByImageIdAndPostId(Long imageId, Long postId);
    List<PostImage> findByPostIdOrderByDisplayOrderAscImageIdAsc(Long postId);
}
