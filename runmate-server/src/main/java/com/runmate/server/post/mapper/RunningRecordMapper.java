package com.runmate.server.post.mapper;

import java.util.Optional;
import com.runmate.server.post.domain.RunningRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RunningRecordMapper {
    Optional<RunningRecord> findByPostId(Long postId);

    Optional<RunningRecord> findLatestByUserId(@Param("userId") Long userId);
    int insert(RunningRecord record);
    int deleteByPostId(Long postId);

    default RunningRecord save(RunningRecord record) {
        insert(record);
        return record;
    }
}


