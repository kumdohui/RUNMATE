package com.runmate.server.notification.service;

import com.runmate.server.notification.dto.NotificationFeedResponse;
import com.runmate.server.notification.dto.UnreadCountResponse;
import com.runmate.server.notification.mapper.NotificationMapper;
import com.runmate.server.post.domain.Comment;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private static final int PAGE_SIZE = 9;

    private final NotificationMapper notificationMapper;

    public void addForComment(Long actorId, Long postAuthorId, Comment parent, Long postId, Long commentId) {
        if (parent == null) {
            insert(postAuthorId, actorId, "POST_COMMENT", postId, commentId);
            return;
        }
        insert(parent.getAuthorId(), actorId, "COMMENT_REPLY", postId, commentId);
        if (!parent.getAuthorId().equals(postAuthorId)) {
            insert(postAuthorId, actorId, "POST_COMMENT", postId, commentId);
        }
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(Long userId) {
        return new UnreadCountResponse(notificationMapper.countUnread(userId));
    }

    @Transactional(readOnly = true)
    public NotificationFeedResponse list(Long userId, Integer requestedPage) {
        int page = Math.max(requestedPage == null ? 0 : requestedPage, 0);
        long total = notificationMapper.countByRecipient(userId);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / PAGE_SIZE);
        if (totalPages > 0 && page >= totalPages) {
            page = totalPages - 1;
        }
        List<NotificationFeedResponse.Item> notifications = notificationMapper
                .searchByRecipient(userId, PAGE_SIZE, page * PAGE_SIZE)
                .stream()
                .map(row -> new NotificationFeedResponse.Item(
                        row.notificationId(),
                        row.type(),
                        row.actorId(),
                        row.actorNickname(),
                        row.postId(),
                        row.postTitle(),
                        row.commentId(),
                        row.read(),
                        row.createdAt()))
                .toList();
        return new NotificationFeedResponse(
                notifications, page, totalPages, total, notificationMapper.countUnread(userId));
    }

    @Transactional
    public void markRead(Long userId, Long notificationId) {
        notificationMapper.markRead(userId, notificationId);
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationMapper.markAllRead(userId);
    }

    private void insert(Long recipientId, Long actorId, String type, Long postId, Long commentId) {
        if (recipientId == null || recipientId.equals(actorId)) {
            return;
        }
        notificationMapper.insert(recipientId, actorId, type, postId, commentId);
    }
}
