package com.runmate.server.post.mapper;

import com.runmate.server.post.domain.Post;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PostMapper {
    Optional<Post> findById(Long postId);
    int insert(Post post);
    int update(Post post);

    default Post saveAndFlush(Post post) {
        if (post.getPostId() == null) insert(post);
        else update(post);
        return post;
    }
}


