package com.runmate.server.user.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.runmate.server.user.domain.SocialAccount;
import com.runmate.server.user.domain.SocialProvider;
import com.runmate.server.user.domain.User;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {
    Optional<SocialAccount> findByProviderAndProviderUserId(
            SocialProvider provider,
            String providerUserId
    );

    boolean existsByUserAndProvider(User user, SocialProvider provider);

    Optional<SocialAccount> findByUserAndProvider(User user, SocialProvider provider);

    List<SocialAccount> findByUser(User user);

    @Query("select account.provider from SocialAccount account where account.user.userId = :userId")
    List<SocialProvider> findProvidersByUserId(@Param("userId") Long userId);
}
