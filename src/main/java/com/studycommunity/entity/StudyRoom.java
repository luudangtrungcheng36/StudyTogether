package com.studycommunity.entity;

import com.studycommunity.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "study_rooms")
public class StudyRoom extends BaseEntity {

    @Column(name = "discord_guild_id")
    private String discordGuildId;

    @Column(name = "discord_channel_id")
    private String discordChannelId;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "tracking_enabled")
    private boolean trackingEnabled = true;
}
