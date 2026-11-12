package com.studycommunity.dto.study;

import java.time.Instant;

// A null channel means the member joined from, or left to, outside voice.
public record DiscordVoiceUpdateCommand(
        String discordUserId,
        String guildId,
        String leftChannelId,
        String joinedChannelId,
        boolean cameraOn,
        boolean screenShareOn,
        Instant observedAt
) {}
