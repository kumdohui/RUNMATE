package com.runmate.server.chat.service;

import com.runmate.server.chat.domain.ChatConversation;
import com.runmate.server.chat.domain.ChatMessage;
import com.runmate.server.chat.dto.ChatDtos.ConversationMessagesResponse;
import com.runmate.server.chat.dto.ChatDtos.ConversationResponse;
import com.runmate.server.chat.dto.ChatDtos.MessageResponse;
import com.runmate.server.chat.dto.ChatDtos.SendMessageResponse;
import com.runmate.server.chat.exception.ChatException;
import com.runmate.server.chat.mapper.ChatConversationMapper;
import com.runmate.server.chat.mapper.ChatMessageMapper;
import com.runmate.server.post.domain.RunningRecord;
import com.runmate.server.post.mapper.RunningRecordMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class ChatbotService {
    private static final String DEFAULT_TITLE = "새 러닝 상담";
    private static final int TITLE_LENGTH = 30;

    private final ChatConversationMapper conversationMapper;
    private final ChatMessageMapper messageMapper;
    private final GeminiService geminiService;
    private final RunningRecordMapper runningRecordMapper;
    private final TransactionTemplate transactionTemplate;

    public List<ConversationResponse> conversations(Long userId) {
        return conversationMapper.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ConversationResponse> deletedConversations(Long userId) {
        return conversationMapper.findDeletedByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ConversationResponse createConversation(Long userId, String requestedTitle) {
        String title = normalizeTitle(requestedTitle);
        ChatConversation conversation = ChatConversation.create(userId, title);
        conversationMapper.insert(conversation);
        return toResponse(conversation);
    }

    public ConversationMessagesResponse messages(Long userId, Long conversationId) {
        requireOwned(userId, conversationId);
        List<MessageResponse> messages = messageMapper.findAll(conversationId)
                .stream()
                .map(this::toResponse)
                .toList();
        return new ConversationMessagesResponse(conversationId, messages);
    }

    public SendMessageResponse send(
            Long userId,
            Long conversationId,
            String rawContent,
            boolean referenceRecentRecord
    ) {
        requireOwned(userId, conversationId);
        String content = normalizeMessage(rawContent);
        requireNotDuplicate(conversationId, content);

        List<ChatMessage> history = messageMapper.findRecent(conversationId);
        String runningRecordContext = referenceRecentRecord
                ? runningRecordMapper.findLatestByUserId(userId)
                        .map(this::runningRecordContext)
                        .orElse(null)
                : null;
        String answer = geminiService.answer(
                history,
                content,
                runningRecordContext
        );

        return transactionTemplate.execute(status -> saveExchange(
                userId,
                conversationId,
                content,
                answer
        ));
    }

    public void delete(Long userId, Long conversationId) {
        if (conversationMapper.softDelete(conversationId, userId) == 0) {
            throw notFound();
        }
    }

    public void restore(Long userId, Long conversationId) {
        if (conversationMapper.restore(conversationId, userId) == 0) {
            throw notFound();
        }
    }

    public void deletePermanently(Long userId, Long conversationId) {
        if (conversationMapper.deletePermanently(conversationId, userId) == 0) {
            throw notFound();
        }
    }

    private String runningRecordContext(RunningRecord record) {
        int seconds = record.getDurationSeconds();
        int minutes = seconds / 60;
        int remainder = seconds % 60;
        return """
                다음은 사용자가 참고하도록 허용한 가장 최근 러닝 기록입니다.
                날짜: %s
                거리: %s km
                시간: %d분 %d초
                장소: %s
                강도: %s
                이 기록을 참고하되 기록에 없는 정보는 추측하지 마세요.
                """.formatted(
                record.getRunningDate(),
                record.getDistanceKm(),
                minutes,
                remainder,
                record.getLocationName() == null ? "미입력" : record.getLocationName(),
                record.getIntensity()
        );
    }

    private SendMessageResponse saveExchange(
            Long userId,
            Long conversationId,
            String content,
            String answer
    ) {
        ChatConversation conversation = conversationMapper
                .lockOwned(conversationId, userId)
                .orElseThrow(this::notFound);

        requireNotDuplicate(conversationId, content);
        int sequence = messageMapper.nextSequence(conversationId);

        ChatMessage userMessage = ChatMessage.create(
                conversationId,
                sequence,
                "USER",
                content,
                null
        );
        ChatMessage assistantMessage = ChatMessage.create(
                conversationId,
                sequence + 1,
                "ASSISTANT",
                answer,
                geminiService.modelName()
        );
        messageMapper.insert(userMessage);
        messageMapper.insert(assistantMessage);

        String title = DEFAULT_TITLE.equals(conversation.getTitle())
                ? titleFrom(content)
                : null;
        conversationMapper.touch(conversationId, title);

        return new SendMessageResponse(
                toResponse(userMessage),
                toResponse(assistantMessage)
        );
    }

    private void requireOwned(Long userId, Long conversationId) {
        conversationMapper
                .findOwned(conversationId, userId)
                .orElseThrow(this::notFound);
    }

    private void requireNotDuplicate(Long conversationId, String content) {
        messageMapper.findLatestUserContent(conversationId)
                .map(this::normalizeMessage)
                .filter(content::equals)
                .ifPresent(ignored -> {
                    throw new ChatException(
                            HttpStatus.CONFLICT,
                            "같은 질문을 연속으로 보낼 수 없습니다."
                    );
                });
    }

    private String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            return DEFAULT_TITLE;
        }
        return title.trim().replaceAll("\\s+", " ");
    }

    private String normalizeMessage(String content) {
        return content.trim().replaceAll("\\s+", " ");
    }

    private String titleFrom(String content) {
        if (content.length() <= TITLE_LENGTH) {
            return content;
        }
        return content.substring(0, TITLE_LENGTH) + "…";
    }

    private ChatException notFound() {
        return new ChatException(
                HttpStatus.NOT_FOUND,
                "대화방을 찾을 수 없습니다."
        );
    }

    private ConversationResponse toResponse(ChatConversation conversation) {
        return new ConversationResponse(
                conversation.getConversationId(),
                conversation.getTitle(),
                conversation.getLastMessageAt(),
                conversation.getCreatedAt()
        );
    }

    private MessageResponse toResponse(ChatMessage message) {
        return new MessageResponse(
                message.getMessageId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
