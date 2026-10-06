package com.runmate.server.chat.domain;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {
    private Long messageId;
    private Long conversationId;
    private Integer sequenceNumber;
    private String role;
    private String content;
    private String modelName;
    private LocalDateTime createdAt;

    public static ChatMessage create(
            Long conversationId,
            int sequenceNumber,
            String role,
            String content,
            String modelName
    ) {
        ChatMessage message = new ChatMessage();
        message.conversationId = conversationId;
        message.sequenceNumber = sequenceNumber;
        message.role = role;
        message.content = content;
        message.modelName = modelName;
        return message;
    }
}
