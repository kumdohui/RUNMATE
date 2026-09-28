package com.runmate.server.report.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReportMapper {
    int insert(@Param("reporterId") Long reporterId, @Param("targetType") String targetType,
            @Param("targetId") Long targetId, @Param("reason") String reason, @Param("detail") String detail);

    boolean existsOpen(@Param("reporterId") Long reporterId, @Param("targetType") String targetType,
            @Param("targetId") Long targetId);
}
