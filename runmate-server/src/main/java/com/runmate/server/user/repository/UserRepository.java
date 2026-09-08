package com.runmate.server.user.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.runmate.server.user.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLoginId(String loginId);
    boolean existsByLoginId(String loginId);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByNickname(String nickname);
}
