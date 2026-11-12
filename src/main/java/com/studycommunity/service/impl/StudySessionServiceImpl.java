package com.studycommunity.service.impl;

import com.studycommunity.dto.study.DiscordMediaUpdateCommand;
import com.studycommunity.dto.study.DiscordVoiceUpdateCommand;
import com.studycommunity.entity.StudySession;
import com.studycommunity.entity.StudySessionSegment;
import com.studycommunity.enums.StudySource;
import com.studycommunity.repository.StudyRoomRepository;
import com.studycommunity.repository.StudySessionRepository;
import com.studycommunity.repository.StudySessionSegmentRepository;
import com.studycommunity.service.StudySessionService;
import com.studycommunity.service.UserIdentityService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudySessionServiceImpl implements StudySessionService {

    private final StudyRoomRepository studyRoomRepository;
    private final StudySessionRepository studySessionRepository;
    private final StudySessionSegmentRepository segmentRepository;
    private final UserIdentityService userIdentityService;

    @Override
    @Transactional
    public Optional<StudySession> handleDiscordVoiceUpdate(DiscordVoiceUpdateCommand command) {
        if (Objects.equals(command.leftChannelId(), command.joinedChannelId())) {
            return Optional.empty();
        }
        Instant time = command.observedAt().truncatedTo(ChronoUnit.MICROS);
        if (command.leftChannelId() != null) {
            var previous = studySessionRepository
                    .findOpenDiscordSession(command.discordUserId(), command.guildId(), command.leftChannelId());
            if (previous.isPresent()) {
                StudySession session = previous.get();
                if (time.isBefore(session.getLastObservedAt())) {
                    return Optional.empty();
                }
                closeSession(session, time, command.joinedChannelId() == null ? "LEFT_ROOM" : "MOVED_ROOM");
                // Release the open-session unique key before inserting the next room's session.
                studySessionRepository.flush();
            }
        }
        if (command.joinedChannelId() == null) {
            return Optional.empty();
        }
        return startSession(command.discordUserId(), command.guildId(), command.joinedChannelId(),
                command.cameraOn(), command.screenShareOn(), time);
    }

    @Override
    @Transactional
    public void handleDiscordMediaUpdate(DiscordMediaUpdateCommand command) {
        var current = studySessionRepository.findOpenDiscordSession(
                command.discordUserId(), command.guildId(), command.channelId());
        if (current.isEmpty()) {
            return;
        }
        StudySession session = current.get();
        Instant time = command.observedAt().truncatedTo(ChronoUnit.MICROS);
        if (time.isBefore(session.getLastObservedAt())) {
            return;
        }
        session.setLastObservedAt(time);
        var openSegment = segmentRepository.findOpenBySessionId(session.getId());
        boolean hasActiveMedia = command.cameraOn() || command.screenShareOn();
        if (openSegment.isPresent()) {
            StudySessionSegment segment = openSegment.get();
            if (segment.isCameraOn() == command.cameraOn() && segment.isScreenShareOn() == command.screenShareOn()) {
                return;
            }
            if (time.equals(segment.getStartedAt())) {
                if (hasActiveMedia) {
                    segment.setCameraOn(command.cameraOn());
                    segment.setScreenShareOn(command.screenShareOn());
                } else {
                    segment.setEndedAt(time);
                }
                return;
            }
            segment.setEndedAt(time);
            if (hasActiveMedia) {
                // Hibernate may insert before updating; close the old segment's unique key first.
                segmentRepository.flush();
                createSegment(session, time, command.cameraOn(), command.screenShareOn());
            }
        } else if (hasActiveMedia) {
            createSegment(session, time, command.cameraOn(), command.screenShareOn());
        }
    }

    private Optional<StudySession> startSession(String discordUserId, String guildId, String channelId,
            boolean cameraOn, boolean screenShareOn, Instant time) {
        var room = studyRoomRepository.findTrackedRoom(guildId, channelId);
        if (room.isEmpty()) {
            return Optional.empty();
        }
        var identity = userIdentityService.findOrCreateDiscordIdentity(discordUserId);
        if (studySessionRepository.findOpenByUserId(identity.getUser().getId()).isPresent()) {
            return Optional.empty();
        }
        StudySession session = StudySession.builder()
                .user(identity.getUser())
                .identity(identity)
                .studyRoom(room.get())
                .source(StudySource.DISCORD)
                .startedAt(time)
                .lastObservedAt(time)
                .build();
        studySessionRepository.save(session);
        if (cameraOn || screenShareOn) {
            createSegment(session, time, cameraOn, screenShareOn);
        }
        return Optional.of(session);
    }

    private void closeSession(StudySession session, Instant time, String reason) {
        segmentRepository.findOpenBySessionId(session.getId())
                .ifPresent(segment -> segment.setEndedAt(time));
        session.setEndedAt(time);
        session.setLastObservedAt(time);
        session.setEndReason(reason);
    }

    private void createSegment(StudySession session, Instant time, boolean cameraOn, boolean screenShareOn) {
        StudySessionSegment segment = StudySessionSegment.builder()
                .studySession(session)
                .startedAt(time)
                .cameraOn(cameraOn)
                .screenShareOn(screenShareOn)
                .build();
        segmentRepository.save(segment);
    }
}
