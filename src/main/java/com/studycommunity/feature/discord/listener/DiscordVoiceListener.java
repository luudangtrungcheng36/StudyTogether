package com.studycommunity.feature.discord.listener;

import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceVideoEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceStreamEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DiscordVoiceListener extends ListenerAdapter {

    private static final Logger log =
            LoggerFactory.getLogger(DiscordVoiceListener.class);

    // Người dùng vào, rời hoặc chuyển phòng
    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {

        if (event.getMember().getUser().isBot()) {
            return;
        }

        String userId = event.getMember().getId();

        if (event.getChannelLeft() != null) {
            log.info("LEAVE: {} - Room: {}",
                    userId,
                    event.getChannelLeft().getName());
        }

        if (event.getChannelJoined() != null) {
            log.info("JOIN: {} - Room: {}",
                    userId,
                    event.getChannelJoined().getName());
        }
    }

    // Người dùng bật hoặc tắt camera
    @Override
    public void onGuildVoiceVideo(GuildVoiceVideoEvent event) {
        log.info("CAMERA: {} - {}",
                event.getMember().getId(),
                event.isSendingVideo());
    }

    // Người dùng bật hoặc tắt chia sẻ màn hình
    @Override
    public void onGuildVoiceStream(GuildVoiceStreamEvent event) {
        log.info("STREAM: {} - {}",
                event.getMember().getId(),
                event.isStream());
    }
}
