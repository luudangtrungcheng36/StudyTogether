package com.studycommunity.service;

import com.studycommunity.dto.study.DiscordMediaUpdateCommand;
import com.studycommunity.dto.study.DiscordVoiceUpdateCommand;
import com.studycommunity.entity.StudySession;
import java.util.Optional;

public interface StudySessionService {

    Optional<StudySession> handleDiscordVoiceUpdate(DiscordVoiceUpdateCommand command);

    void handleDiscordMediaUpdate(DiscordMediaUpdateCommand command);
}
