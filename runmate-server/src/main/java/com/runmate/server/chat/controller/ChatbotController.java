package com.runmate.server.chat.controller;

import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.auth.security.RunmatePrincipal;
import com.runmate.server.chat.dto.ChatDtos.ConversationMessagesResponse;
import com.runmate.server.chat.dto.ChatDtos.ConversationResponse;
import com.runmate.server.chat.dto.ChatDtos.CreateConversationRequest;
import com.runmate.server.chat.dto.ChatDtos.SendMessageRequest;
import com.runmate.server.chat.dto.ChatDtos.SendMessageResponse;
import com.runmate.server.chat.service.ChatbotService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatbotController {
    private final ChatbotService chatbotService;

    @GetMapping("/conversations")
    public List<ConversationResponse> conversations(Authentication authentication) {
        return chatbotService.conversations(principal(authentication).userId());
    }

    @GetMapping("/conversations/trash")
    public List<ConversationResponse> deletedConversations(
            Authentication authentication
    ) {
        return chatbotService.deletedConversations(
                principal(authentication).userId()
        );
    }

    @PostMapping("/conversations")
    public ResponseEntity<ConversationResponse> createConversation(
            @Valid @RequestBody(required = false) CreateConversationRequest request,
            Authentication authentication
    ) {
        String title = request == null ? null : request.title();
        ConversationResponse response = chatbotService.createConversation(
                principal(authentication).userId(),
                title
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ConversationMessagesResponse messages(
            @PathVariable Long conversationId,
            Authentication authentication
    ) {
        return chatbotService.messages(
                principal(authentication).userId(),
                conversationId
        );
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public SendMessageResponse send(
            @PathVariable Long conversationId,
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication
    ) {
        return chatbotService.send(
                principal(authentication).userId(),
                conversationId,
                request.content(),
                Boolean.TRUE.equals(request.referenceRecentRecord())
        );
    }

    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long conversationId,
            Authentication authentication
    ) {
        chatbotService.delete(principal(authentication).userId(), conversationId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/conversations/{conversationId}/restore")
    public ResponseEntity<Void> restore(
            @PathVariable Long conversationId,
            Authentication authentication
    ) {
        chatbotService.restore(principal(authentication).userId(), conversationId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/conversations/{conversationId}/permanent")
    public ResponseEntity<Void> deletePermanently(
            @PathVariable Long conversationId,
            Authentication authentication
    ) {
        chatbotService.deletePermanently(
                principal(authentication).userId(),
                conversationId
        );
        return ResponseEntity.noContent().build();
    }

    private RunmatePrincipal principal(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof RunmatePrincipal principal)) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return principal;
    }
}
