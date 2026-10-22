package com.studycommunity.feature.discord.config;

import com.studycommunity.feature.discord.listener.DiscordVoiceListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DiscordBotConfig {

    @Bean(destroyMethod = "shutdown")
    public JDA discordJda(
            @Value("${discord.bot.token}") String token,
            DiscordVoiceListener listener) {

        return JDABuilder.createDefault(
                        token,
                        GatewayIntent.GUILD_VOICE_STATES
                )
                .enableCache(CacheFlag.VOICE_STATE)
                .addEventListeners(listener)
                .build();
    }
}
