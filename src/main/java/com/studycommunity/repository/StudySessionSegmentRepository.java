package com.studycommunity.repository;

import com.studycommunity.entity.StudySessionSegment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudySessionSegmentRepository extends JpaRepository<StudySessionSegment, UUID> {

    @Query(value = """
            SELECT s.* FROM study_session_segments s
            WHERE s.study_session_id = :sessionId AND s.ended_at IS NULL
            """, nativeQuery = true)
    Optional<StudySessionSegment> findOpenBySessionId(@Param("sessionId") UUID sessionId);
}
