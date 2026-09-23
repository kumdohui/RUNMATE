package com.runmate.server.user.mapper;

import java.util.List;
import java.util.Optional;
import com.runmate.server.user.domain.User;
import com.runmate.server.user.dto.MentionRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    Optional<User> findById(Long userId);
    Optional<User> findByLoginId(String loginId);
    Optional<User> findByPhoneNumber(String phoneNumber);
    Optional<User> findByNickname(String nickname);
    Optional<User> findByEmail(String email);
    boolean existsByLoginId(String loginId);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByNickname(String nickname);
    List<MentionRow> searchForMention(@Param("keyword") String keyword, @Param("limit") int limit);

    int insert(User user);
    int update(User user);

    default User save(User user) {
        if (user.getUserId() == null) insert(user);
        else update(user);
        return user;
    }

    default User saveAndFlush(User user) {
        return save(user);
    }
}


