package com.runmate.server.notification.mapper;

import com.runmate.server.notification.dto.NotificationRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NotificationMapper {
    int insert(@Param("recipientId") Long recipientId, @Param("actorId") Long actorId,
            @Param("type") String type, @Param("postId") Long postId, @Param("commentId") Long commentId);

    long countByRecipient(@Param("recipientId") Long recipientId);

    long countUnread(@Param("recipientId") Long recipientId);

    List<NotificationRow> searchByRecipient(@Param("recipientId") Long recipientId,
            @Param("limit") int limit, @Param("offset") int offset);

    int markRead(@Param("recipientId") Long recipientId, @Param("notificationId") Long notificationId);

    int markAllRead(@Param("recipientId") Long recipientId);
}
