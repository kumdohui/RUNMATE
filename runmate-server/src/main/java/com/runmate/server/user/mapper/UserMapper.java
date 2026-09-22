package com.runmate.server.user.mapper;

import java.util.Optional;
import com.runmate.server.user.domain.User;
import org.apache.ibatis.annotations.Mapper;

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


