package com.runmate.server.user.mapper;

import java.util.Optional;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.runmate.server.user.domain.SocialAccount;
import com.runmate.server.user.domain.SocialProvider;
import com.runmate.server.user.domain.User;

@Mapper
public interface SocialAccountMapper {
    Optional<SocialAccount> findByProviderAndProviderUserId(
            @Param("provider") SocialProvider provider,
            @Param("providerUserId") String providerUserId
    );

    boolean existsByUserAndProvider(
            @Param("user") User user,
            @Param("provider") SocialProvider provider
    );

    Optional<SocialAccount> findByUserAndProvider(
            @Param("user") User user,
            @Param("provider") SocialProvider provider
    );

    List<SocialAccount> findByUser(User user);

    List<SocialProvider> findProvidersByUserId(@Param("userId") Long userId);

    int insert(SocialAccount account);
    int update(SocialAccount account);
    int deleteById(Long socialAccountId);
    int deleteByUserId(Long userId);

    default SocialAccount save(SocialAccount account) {
        if (account.getSocialAccountId() == null) insert(account);
        else update(account);
        return account;
    }

    default void delete(SocialAccount account) {
        deleteById(account.getSocialAccountId());
    }

    default void deleteAll(List<SocialAccount> accounts) {
        if (!accounts.isEmpty()) deleteByUserId(accounts.get(0).getUser().getUserId());
    }

    default void flush() {
    }
}


