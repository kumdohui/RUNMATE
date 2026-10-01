package com.runmate.server.auth.mapper;

import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.runmate.server.auth.domain.RejoinBlock;

@Mapper
public interface RejoinBlockMapper {
    int upsert(RejoinBlock block);

    boolean existsActive(
            @Param("phoneIdentifierHash") String phoneIdentifierHash,
            @Param("now") LocalDateTime now
    );
}
