package com.runmate.server.chat.mapper;

import com.runmate.server.chat.domain.ChatMessage;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageMapper {
    int insert(ChatMessage message);

    List<ChatMessage> findAll(Long conversationId);

    List<ChatMessage> findRecent(Long conversationId);

    Optional<String> findLatestUserContent(Long conversationId);

    int nextSequence(Long conversationId);
}
