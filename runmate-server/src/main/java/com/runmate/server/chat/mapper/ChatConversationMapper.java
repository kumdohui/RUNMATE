package com.runmate.server.chat.mapper;

import com.runmate.server.chat.domain.ChatConversation;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ChatConversationMapper {
    int insert(ChatConversation conversation);

    List<ChatConversation> findByUserId(Long userId);

    List<ChatConversation> findDeletedByUserId(Long userId);

    Optional<ChatConversation> findOwned(
            @Param("conversationId") Long conversationId,
            @Param("userId") Long userId
    );

    Optional<ChatConversation> lockOwned(
            @Param("conversationId") Long conversationId,
            @Param("userId") Long userId
    );

    int touch(
            @Param("conversationId") Long conversationId,
            @Param("title") String title
    );

    int softDelete(@Param("conversationId") Long conversationId,
            @Param("userId") Long userId);

    int restore(@Param("conversationId") Long conversationId,
            @Param("userId") Long userId);

    int deletePermanently(@Param("conversationId") Long conversationId,
            @Param("userId") Long userId);
}
