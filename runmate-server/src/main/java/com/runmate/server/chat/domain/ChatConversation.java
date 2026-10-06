package com.runmate.server.chat.domain;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatConversation {
    private Long conversationId;
    private Long userId;
    private String title;
    private String status;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;

    public static ChatConversation create(Long userId, String title) {
        ChatConversation conversation = new ChatConversation();
        conversation.userId = userId;
        conversation.title = title;
        conversation.status = "ACTIVE";
        return conversation;
    }
}
