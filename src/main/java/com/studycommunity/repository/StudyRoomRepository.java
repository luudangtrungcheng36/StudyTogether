package com.studycommunity.repository;

import com.studycommunity.entity.StudyRoom;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudyRoomRepository extends JpaRepository<StudyRoom, UUID> {

    @Query(value = """
            SELECT r.* FROM study_rooms r
            WHERE r.discord_guild_id = :guildId
              AND r.discord_channel_id = :channelId
              AND r.tracking_enabled = true
            """, nativeQuery = true)
    Optional<StudyRoom> findTrackedRoom(@Param("guildId") String guildId,
                                       @Param("channelId") String channelId);
}
