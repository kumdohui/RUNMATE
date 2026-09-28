package com.runmate.server.admin.mapper;

import com.runmate.server.admin.dto.AdminUserRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminUserMapper {
    long count(@Param("keyword") String keyword);

    List<AdminUserRow> search(@Param("keyword") String keyword, @Param("limit") int limit, @Param("offset") int offset);

    int updateStatus(@Param("userId") Long userId, @Param("status") String status);
}
