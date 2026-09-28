package com.runmate.server.report.mapper;

import com.runmate.server.report.dto.AdminReportRow;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReportMapper {
    int insert(@Param("reporterId") Long reporterId, @Param("targetType") String targetType,
            @Param("targetId") Long targetId, @Param("reason") String reason, @Param("detail") String detail);

    boolean existsOpen(@Param("reporterId") Long reporterId, @Param("targetType") String targetType,
            @Param("targetId") Long targetId);

    long countByStatus(@Param("status") String status);

    List<AdminReportRow> searchByStatus(@Param("status") String status, @Param("limit") int limit,
            @Param("offset") int offset);

    Optional<AdminReportRow> findAdminById(Long reportId);

    int markProcessed(@Param("reportId") Long reportId, @Param("adminId") Long adminId,
            @Param("status") String status, @Param("note") String note);

    long countProcessed();

    List<com.runmate.server.admin.dto.AdminLogRow> searchProcessed(@Param("limit") int limit,
            @Param("offset") int offset);
}
