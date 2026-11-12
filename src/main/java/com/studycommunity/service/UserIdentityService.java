package com.studycommunity.service;

import com.studycommunity.entity.UserIdentity;

public interface UserIdentityService {

    UserIdentity findOrCreateDiscordIdentity(String discordUserId);
}
