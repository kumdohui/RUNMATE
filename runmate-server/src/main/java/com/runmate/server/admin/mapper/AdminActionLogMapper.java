package com.runmate.server.admin.mapper;

import com.runmate.server.admin.dto.AdminLogRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminActionLogMapper {
    int insert(
            @Param("adminId") Long adminId,
            @Param("actionType") String actionType,
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId,
            @Param("reason") String reason,
            @Param("ipAddress") String ipAddress
    );

    long countAll();

    List<AdminLogRow> searchAll(
            @Param("limit") int limit,
            @Param("offset") int offset
    );
}
