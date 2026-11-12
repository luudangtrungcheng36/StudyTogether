package com.studycommunity.repository;

import com.studycommunity.entity.StudySession;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudySessionRepository extends JpaRepository<StudySession, UUID> {

    @Query(value = """
            SELECT s.* FROM study_sessions s
            WHERE s.user_id = :userId AND s.ended_at IS NULL
            """, nativeQuery = true)
    Optional<StudySession> findOpenByUserId(@Param("userId") UUID userId);

    @Query(value = """
            SELECT s.* FROM study_sessions s
            JOIN user_identities i ON i.id = s.identity_id
            JOIN study_rooms r ON r.id = s.study_room_id
            WHERE s.source = 'DISCORD' AND s.ended_at IS NULL
              AND i.provider = 'DISCORD' AND i.provider_subject = :discordUserId
              AND r.discord_guild_id = :guildId AND r.discord_channel_id = :channelId
            """, nativeQuery = true)
    Optional<StudySession> findOpenDiscordSession(@Param("discordUserId") String discordUserId,
                                                 @Param("guildId") String guildId,
                                                 @Param("channelId") String channelId);
}
