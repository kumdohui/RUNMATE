package com.runmate.server.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public final class ChatDtos {
    private ChatDtos() {
    }

    public record CreateConversationRequest(
            @Size(max = 100, message = "대화방 제목은 100자 이하로 입력해 주세요.")
            String title
    ) {
    }

    public record ConversationResponse(
            Long conversationId,
            String title,
            LocalDateTime lastMessageAt,
            LocalDateTime createdAt
    ) {
    }

    public record SendMessageRequest(
            @NotBlank(message = "질문을 입력해 주세요.")
            @Size(max = 2000, message = "질문은 2,000자 이하로 입력해 주세요.")
            String content,
            Boolean referenceRecentRecord
    ) {
    }

    public record MessageResponse(
            Long messageId,
            String role,
            String content,
            LocalDateTime createdAt
    ) {
    }

    public record ConversationMessagesResponse(
            Long conversationId,
            List<MessageResponse> messages
    ) {
    }

    public record SendMessageResponse(
            MessageResponse userMessage,
            MessageResponse assistantMessage
    ) {
    }
}
