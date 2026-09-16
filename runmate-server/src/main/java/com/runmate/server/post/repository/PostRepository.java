package com.runmate.server.post.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.runmate.server.post.domain.Post;

public interface PostRepository extends JpaRepository<Post, Long> {
}
