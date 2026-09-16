package com.runmate.server.post.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.runmate.server.post.domain.RunningRecord;

public interface RunningRecordRepository extends JpaRepository<RunningRecord, Long> {
    Optional<RunningRecord> findByPostId(Long postId);
}
