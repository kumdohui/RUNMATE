package com.runmate.server.post.mapper;

import java.util.Optional;
import com.runmate.server.post.domain.RunningRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RunningRecordMapper {
    Optional<RunningRecord> findByPostId(Long postId);
    int insert(RunningRecord record);

    default RunningRecord save(RunningRecord record) {
        insert(record);
        return record;
    }
}


