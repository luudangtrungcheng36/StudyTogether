package com.studycommunity.discord.listener;

import com.studycommunity.dto.study.DiscordMediaUpdateCommand;
import com.studycommunity.dto.study.DiscordVoiceUpdateCommand;
import com.studycommunity.entity.StudySession;
import com.studycommunity.service.StudySessionService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceStreamEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceVideoEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiscordVoiceListener extends ListenerAdapter {

    private final StudySessionService studySessionService;

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        if (event.getMember().getUser().isBot()) {
            return;
        }
        var left = event.getChannelLeft();
        var joined = event.getChannelJoined();
        var state = event.getVoiceState();
        var command = new DiscordVoiceUpdateCommand(
                event.getMember().getId(), event.getGuild().getId(),
                left == null ? null : left.getId(), joined == null ? null : joined.getId(),
                state.isSendingVideo(), state.isStream(), Instant.now());
        var started = studySessionService.handleDiscordVoiceUpdate(command);
        log.info("Voice update processed: userId={}, leftChannelId={}, joinedChannelId={}, startedSessionId={}",
                command.discordUserId(), command.leftChannelId(), command.joinedChannelId(),
                started.map(StudySession::getId).orElse(null));
    }

    @Override
    public void onGuildVoiceVideo(GuildVoiceVideoEvent event) {
        if (event.getMember().getUser().isBot()) {
            return;
        }
        var state = event.getVoiceState();
        var channel = state.getChannel();
        if (channel == null) {
            return;
        }
        studySessionService.handleDiscordMediaUpdate(new DiscordMediaUpdateCommand(
                event.getMember().getId(), event.getGuild().getId(), channel.getId(),
                event.isSendingVideo(), state.isStream(), Instant.now()));
        log.debug("Camera update processed: userId={}, channelId={}, cameraOn={}",
                event.getMember().getId(), channel.getId(), event.isSendingVideo());
    }

    @Override
    public void onGuildVoiceStream(GuildVoiceStreamEvent event) {
        if (event.getMember().getUser().isBot()) {
            return;
        }
        var state = event.getVoiceState();
        var channel = state.getChannel();
        if (channel == null) {
            return;
        }
        studySessionService.handleDiscordMediaUpdate(new DiscordMediaUpdateCommand(
                event.getMember().getId(), event.getGuild().getId(), channel.getId(),
                state.isSendingVideo(), event.isStream(), Instant.now()));
        log.debug("Screen share update processed: userId={}, channelId={}, screenShareOn={}",
                event.getMember().getId(), channel.getId(), event.isStream());
    }
}
