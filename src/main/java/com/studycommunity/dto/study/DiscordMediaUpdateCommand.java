package com.studycommunity.dto.study;

import java.time.Instant;

public record DiscordMediaUpdateCommand(
        String discordUserId,
        String guildId,
        String channelId,
        boolean cameraOn,
        boolean screenShareOn,
        Instant observedAt
) {}
