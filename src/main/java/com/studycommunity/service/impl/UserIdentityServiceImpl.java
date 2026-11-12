package com.studycommunity.service.impl;

import com.studycommunity.entity.User;
import com.studycommunity.entity.UserIdentity;
import com.studycommunity.enums.IdentityProvider;
import com.studycommunity.repository.UserIdentityRepository;
import com.studycommunity.repository.UserRepository;
import com.studycommunity.service.UserIdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserIdentityServiceImpl implements UserIdentityService {

    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;

    @Override
    @Transactional
    public UserIdentity findOrCreateDiscordIdentity(String discordUserId) {
        return userIdentityRepository
                .findByProviderAndProviderSubject(IdentityProvider.DISCORD, discordUserId)
                .orElseGet(() -> createDiscordIdentity(discordUserId));
    }

    private UserIdentity createDiscordIdentity(String discordUserId) {
        User user = userRepository.save(new User());
        UserIdentity identity = new UserIdentity();
        identity.setUser(user);
        identity.setProvider(IdentityProvider.DISCORD);
        identity.setProviderSubject(discordUserId);
        return userIdentityRepository.saveAndFlush(identity);
    }
}
