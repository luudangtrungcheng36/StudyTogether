package com.studycommunity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.studycommunity.discord.listener.DiscordVoiceListener;
import com.studycommunity.dto.study.DiscordMediaUpdateCommand;
import com.studycommunity.dto.study.DiscordVoiceUpdateCommand;
import com.studycommunity.repository.UserRepository;
import com.studycommunity.service.impl.UserIdentityServiceImpl;
import com.studycommunity.service.impl.StudySessionServiceImpl;
import jakarta.persistence.EntityManagerFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceStreamEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceVideoEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

// TEST_DB_* must target disposable PostgreSQL; only the persistence/listener context starts, without Discord.
@EnabledIfEnvironmentVariable(named = "TEST_DB_URL", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StudySessionServiceIntegrationTest {

    private static final Instant START = Instant.parse("2026-10-31T08:00:00Z");
    private final String schema = "study_service_test_" + UUID.randomUUID().toString().replace("-", "");
    private AnnotationConfigApplicationContext context;
    private JdbcTemplate jdbc;
    private StudySessionService service;
    private DiscordVoiceListener listener;

    @BeforeAll
    void setUp() throws Exception {
        var dataSource = new DriverManagerDataSource();
        dataSource.setUrl(System.getenv("TEST_DB_URL"));
        dataSource.setUsername(System.getenv("TEST_DB_USERNAME"));
        dataSource.setPassword(System.getenv("TEST_DB_PASSWORD"));
        dataSource.setSchema(schema);
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE SCHEMA " + schema);
        String sql = Files.readString(Path.of("docs/sql/schema.sql")).replace("public.", schema + ".");
        try (var connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection,
                    new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)));
        }
        context = new AnnotationConfigApplicationContext();
        context.getEnvironment().setActiveProfiles("test");
        context.registerBean(DataSource.class, () -> dataSource);
        context.register(TestConfiguration.class);
        context.refresh();
        service = context.getBean(StudySessionService.class);
        listener = context.getBean(DiscordVoiceListener.class);
    }

    @BeforeEach
    void resetData() {
        jdbc.update("DELETE FROM study_session_segments");
        jdbc.update("DELETE FROM study_sessions");
        jdbc.update("DELETE FROM user_identities");
        jdbc.update("DELETE FROM users");
        jdbc.update("DELETE FROM study_rooms");
        jdbc.update("""
                INSERT INTO study_rooms (discord_guild_id, discord_channel_id, tracking_enabled)
                VALUES ('guild', 'room-a', true), ('guild', 'room-b', true), ('guild', 'disabled', false)
                """);
    }

    @AfterAll
    void tearDown() {
        if (context != null) {
            context.close();
        }
        if (jdbc != null) {
            jdbc.execute("DROP SCHEMA " + schema + " CASCADE");
        }
    }

    @Test
    void joinThenLeave_persistsOwnedSessionAndClosesItsInitialSegment() {
        var session = service.handleDiscordVoiceUpdate(new DiscordVoiceUpdateCommand("learner", "guild", null, "room-a",
                true, false, START)).orElseThrow();
        assertEquals(1, count("users"));
        assertEquals(1, count("user_identities"));
        assertEquals(1, count("study_sessions"));
        assertEquals(1, count("study_session_segments"));
        assertEquals("DISCORD", jdbc.queryForObject("SELECT source FROM study_sessions", String.class));
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM study_sessions s JOIN user_identities i ON i.id = s.identity_id
                WHERE s.user_id = i.user_id AND s.id = ?
                """, Integer.class, session.getId()));
        service.handleDiscordVoiceUpdate(new DiscordVoiceUpdateCommand("learner", "guild", "room-a", null,
                false, false, START.plusSeconds(120)));
        assertEquals(120, jdbc.queryForObject(
                "SELECT extract(epoch FROM ended_at - started_at)::int FROM study_sessions", Integer.class));
        assertEquals("LEFT_ROOM", jdbc.queryForObject("SELECT end_reason FROM study_sessions", String.class));
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM study_sessions s JOIN study_session_segments g ON g.study_session_id = s.id
                WHERE g.started_at = s.started_at AND g.ended_at = s.ended_at
                  AND s.last_observed_at = s.ended_at AND g.camera_on AND NOT g.screen_share_on
                """, Integer.class));
    }

    @Test
    void unknownOrDisabledRoomAndUnknownLeave_doNotCreateAccountsOrHistory() {
        assertTrue(service.handleDiscordVoiceUpdate(new DiscordVoiceUpdateCommand("learner", "guild", null, "unknown",
                false, false, START)).isEmpty());
        assertTrue(service.handleDiscordVoiceUpdate(new DiscordVoiceUpdateCommand("learner", "guild", null, "disabled",
                false, false, START)).isEmpty());
        service.handleDiscordVoiceUpdate(new DiscordVoiceUpdateCommand("learner", "guild", "room-a", null, false, false, START));
        service.handleDiscordMediaUpdate(new DiscordMediaUpdateCommand("learner", "guild", "room-a", true, true, START));
        assertEquals(0, count("users"));
        assertEquals(0, count("study_sessions"));
    }

    @Test
    void repeatedJoinAndSecondIdentity_keepTheFirstSession() {
        var first = service.handleDiscordVoiceUpdate(new DiscordVoiceUpdateCommand("learner", "guild", null, "room-a",
                false, false, START)).orElseThrow();
        jdbc.update("""
                INSERT INTO user_identities (user_id, provider, provider_subject)
                SELECT user_id, 'DISCORD', 'linked-identity' FROM user_identities WHERE provider_subject = 'learner'
                """);
        assertTrue(service.handleDiscordVoiceUpdate(new DiscordVoiceUpdateCommand("learner", "guild", null, "room-a",
                false, false, START.plusSeconds(1))).isEmpty());
        assertTrue(service.handleDiscordVoiceUpdate(new DiscordVoiceUpdateCommand("linked-identity", "guild", null, "room-b",
                false, false, START.plusSeconds(2))).isEmpty());
        assertEquals(1, count("study_sessions"));
        assertEquals(0, count("study_session_segments"));
        assertEquals(first.getId(), jdbc.queryForObject("SELECT id FROM study_sessions", UUID.class));
    }

    @Test
    void moveBetweenTrackedRooms_closesAndStartsAtTheSameBoundary() {
        service.handleDiscordVoiceUpdate(new DiscordVoiceUpdateCommand("learner", "guild", null, "room-a", false, false, START));
        service.handleDiscordVoiceUpdate(new DiscordVoiceUpdateCommand("learner", "guild", "room-a", "room-b",
                true, true, START.plusSeconds(60))).orElseThrow();
        assertEquals(2, count("study_sessions"));
        assertEquals(1, count("study_session_segments"));
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM study_sessions old JOIN study_sessions current
                  ON old.user_id = current.user_id AND old.ended_at = current.started_at
                WHERE old.end_reason = 'MOVED_ROOM' AND current.ended_at IS NULL
                """, Integer.class));
        service.handleDiscordVoiceUpdate(new DiscordVoiceUpdateCommand("learner", "guild", "room-a", null,
                false, false, START.plusSeconds(70)));
        assertEquals(1, jdbc.queryForObject(
                "SELECT count(*) FROM study_sessions WHERE ended_at IS NULL", Integer.class));
    }

    @Test
    void moveToUntrackedRoom_closesWithoutStartingAnotherSession() {
        service.handleDiscordVoiceUpdate(new DiscordVoiceUpdateCommand("learner", "guild", null, "room-a", false, false, START));
        service.handleDiscordVoiceUpdate(new DiscordVoiceUpdateCommand("learner", "guild", "room-a", "disabled",
                false, false, START.plusSeconds(60)));
        assertEquals(1, count("study_sessions"));
        assertEquals(0, jdbc.queryForObject(
                "SELECT count(*) FROM study_session_segments WHERE ended_at IS NULL", Integer.class));
    }

    @Test
    void cameraAndShareChanges_preserveContinuousIntervalsWithoutDuplicateSegments() {
        service.handleDiscordVoiceUpdate(new DiscordVoiceUpdateCommand("learner", "guild", null, "room-a", false, false, START));
        service.handleDiscordMediaUpdate(new DiscordMediaUpdateCommand("learner", "guild", "room-a", true, false, START.plusSeconds(10)));
        service.handleDiscordMediaUpdate(new DiscordMediaUpdateCommand("learner", "guild", "room-a", true, true, START.plusSeconds(20)));
        service.handleDiscordMediaUpdate(new DiscordMediaUpdateCommand("learner", "guild", "room-a", true, true, START.plusSeconds(25)));
        service.handleDiscordMediaUpdate(new DiscordMediaUpdateCommand("learner", "guild", "room-b", false, false, START.plusSeconds(26)));
        service.handleDiscordVoiceUpdate(new DiscordVoiceUpdateCommand("learner", "guild", "room-a", null,
                true, true, START.plusSeconds(30)));
        assertEquals(2, count("study_session_segments"));
        assertEquals(20, jdbc.queryForObject("""
                SELECT sum(extract(epoch FROM ended_at - started_at))::int FROM study_session_segments
                """, Integer.class));
        assertEquals(20, jdbc.queryForObject("""
                SELECT sum(extract(epoch FROM ended_at - started_at))::int FROM study_session_segments WHERE camera_on
                """, Integer.class));
        assertEquals(10, jdbc.queryForObject("""
                SELECT sum(extract(epoch FROM ended_at - started_at))::int FROM study_session_segments WHERE screen_share_on
                """, Integer.class));
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM study_session_segments a JOIN study_session_segments b
                  ON a.study_session_id = b.study_session_id AND a.ended_at = b.started_at
                """, Integer.class));
    }

    @Test
    void sameMicrosecondStateChange_updatesBoundaryAndOlderEventsAreIgnored() {
        service.handleDiscordVoiceUpdate(new DiscordVoiceUpdateCommand("learner", "guild", null, "room-a",
                false, false, START.plusNanos(100)));
        service.handleDiscordMediaUpdate(new DiscordMediaUpdateCommand("learner", "guild", "room-a",
                true, true, START.plusNanos(900)));
        service.handleDiscordMediaUpdate(new DiscordMediaUpdateCommand("learner", "guild", "room-a", false, false, START.minusSeconds(1)));
        service.handleDiscordVoiceUpdate(new DiscordVoiceUpdateCommand("learner", "guild", "room-a", null,
                false, false, START.minusSeconds(1)));
        assertEquals(1, count("study_session_segments"));
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM study_session_segments
                WHERE camera_on AND screen_share_on AND ended_at IS NULL
                """, Integer.class));
    }

    @Test
    void segmentInsertFailure_rollsBackNewAccountAndSession() {
        jdbc.execute("""
                CREATE FUNCTION reject_test_segment() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN RAISE EXCEPTION 'Injected segment write failure'; END $$
                """);
        jdbc.execute("""
                CREATE TRIGGER reject_test_segment BEFORE INSERT ON study_session_segments
                FOR EACH ROW EXECUTE FUNCTION reject_test_segment()
                """);
        try {
            assertThrows(RuntimeException.class, () -> service.handleDiscordVoiceUpdate(new DiscordVoiceUpdateCommand(
                    "learner", "guild", null, "room-a", true, false, START)));
            assertEquals(0, count("users"));
            assertEquals(0, count("user_identities"));
            assertEquals(0, count("study_sessions"));
            assertEquals(0, count("study_session_segments"));
        } finally {
            jdbc.execute("DROP TRIGGER reject_test_segment ON study_session_segments");
            jdbc.execute("DROP FUNCTION reject_test_segment()");
        }
    }

    @Test
    void voiceOnlySession_doesNotCreateAnySegments() {
        service.handleDiscordVoiceUpdate(new DiscordVoiceUpdateCommand("learner", "guild", null, "room-a",
                false, false, START)).orElseThrow();
        assertEquals(1, count("study_sessions"));
        assertEquals(0, count("study_session_segments"));

        service.handleDiscordVoiceUpdate(new DiscordVoiceUpdateCommand("learner", "guild", "room-a", null,
                false, false, START.plusSeconds(300)));
        assertEquals(1, count("study_sessions"));
        assertEquals(0, count("study_session_segments"));
        assertEquals(300, jdbc.queryForObject(
                "SELECT extract(epoch FROM ended_at - started_at)::int FROM study_sessions", Integer.class));
        assertEquals("LEFT_ROOM", jdbc.queryForObject("SELECT end_reason FROM study_sessions", String.class));
    }

    @Test
    @DisplayName("JDA callbacks persist joins, camera/share changes, a room move and a leave")
    void discordListener_joinMediaMoveAndLeave_persistSessionsAndContinuousSegments() {
        var api = mock(JDA.class);
        var roomA = channel("room-a");
        var roomB = channel("room-b");
        var member = member(roomA, false, false, false);
        listener.onEvent(new GuildVoiceUpdateEvent(api, 1, member, null));
        assertEquals(1, count("study_sessions"));

        member = member(roomA, true, false, false);
        listener.onEvent(new GuildVoiceVideoEvent(api, 2, member, true));
        member = member(roomA, true, true, false);
        listener.onEvent(new GuildVoiceStreamEvent(api, 3, member, true));
        member = member(roomA, false, true, false);
        listener.onEvent(new GuildVoiceVideoEvent(api, 4, member, false));
        member = member(roomA, false, false, false);
        listener.onEvent(new GuildVoiceStreamEvent(api, 5, member, false));

        member = member(roomB, true, true, false);
        listener.onEvent(new GuildVoiceUpdateEvent(api, 6, member, roomA));
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM study_sessions old JOIN study_sessions current
                  ON old.user_id = current.user_id AND old.ended_at = current.started_at
                WHERE old.end_reason = 'MOVED_ROOM' AND current.ended_at IS NULL
                """, Integer.class));
        member = member(null, false, false, false);
        listener.onEvent(new GuildVoiceUpdateEvent(api, 7, member, roomB));

        assertEquals(1, count("users"));
        assertEquals(1, count("user_identities"));
        assertEquals(2, count("study_sessions"));
        assertEquals(4, count("study_session_segments"));
        assertEquals(0, jdbc.queryForObject(
                "SELECT count(*) FROM study_sessions WHERE ended_at IS NULL", Integer.class));
        assertEquals(0, jdbc.queryForObject(
                "SELECT count(*) FROM study_session_segments WHERE ended_at IS NULL", Integer.class));
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM study_sessions s JOIN study_rooms r ON r.id = s.study_room_id
                WHERE r.discord_channel_id = 'room-b' AND s.end_reason = 'LEFT_ROOM'
                """, Integer.class));
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM study_session_segments g
                JOIN study_sessions s ON s.id = g.study_session_id
                JOIN study_rooms r ON r.id = s.study_room_id
                WHERE r.discord_channel_id = 'room-b' AND g.camera_on AND g.screen_share_on
                """, Integer.class));
        var states = jdbc.queryForList("""
                SELECT g.camera_on, g.screen_share_on FROM study_session_segments g
                JOIN study_sessions s ON s.id = g.study_session_id
                JOIN study_rooms r ON r.id = s.study_room_id
                WHERE r.discord_channel_id = 'room-a' ORDER BY g.started_at
                """);
        assertEquals(List.of(
                Map.of("camera_on", true, "screen_share_on", false),
                Map.of("camera_on", true, "screen_share_on", true),
                Map.of("camera_on", false, "screen_share_on", true)), states);
        assertEquals(0, jdbc.queryForObject("""
                SELECT count(*) FROM study_sessions s
                JOIN study_session_segments g ON g.study_session_id = s.id
                WHERE g.started_at < s.started_at
                   OR g.ended_at > s.ended_at
                """, Integer.class));
    }

    @Test
    @DisplayName("All Discord callbacks ignore bots without creating or changing history")
    void discordListener_botCallbacks_doNotCreateOrChangeStudyHistory() {
        var api = mock(JDA.class);
        var room = channel("room-a");
        var member = member(room, false, false, true);
        listener.onEvent(new GuildVoiceUpdateEvent(api, 1, member, null));
        assertEquals(0, count("users"));
        assertEquals(0, count("study_sessions"));

        service.handleDiscordVoiceUpdate(new DiscordVoiceUpdateCommand(
                "learner", "guild", null, "room-a", true, false, START)).orElseThrow();
        member = member(room, true, true, true);
        listener.onEvent(new GuildVoiceVideoEvent(api, 2, member, true));
        listener.onEvent(new GuildVoiceStreamEvent(api, 3, member, true));
        member = member(null, false, false, true);
        listener.onEvent(new GuildVoiceUpdateEvent(api, 4, member, room));
        assertEquals(1, count("study_session_segments"));
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM study_session_segments
                WHERE camera_on AND NOT screen_share_on AND ended_at IS NULL
                """, Integer.class));
        assertEquals(1, jdbc.queryForObject(
                "SELECT count(*) FROM study_sessions WHERE ended_at IS NULL", Integer.class));
    }

    @Test
    @DisplayName("Untracked rooms and media callbacks outside voice do not create history")
    void discordListener_untrackedRoomsAndDisconnectedMedia_doNotCreateHistory() {
        var api = mock(JDA.class);
        for (String roomId : List.of("unknown", "disabled")) {
            var member = member(channel(roomId), true, true, false);
            listener.onEvent(new GuildVoiceUpdateEvent(api, 1, member, null));
            listener.onEvent(new GuildVoiceVideoEvent(api, 2, member, true));
            listener.onEvent(new GuildVoiceStreamEvent(api, 3, member, true));
        }
        var member = member(null, false, false, false);
        listener.onEvent(new GuildVoiceVideoEvent(api, 4, member, false));
        listener.onEvent(new GuildVoiceStreamEvent(api, 5, member, false));
        assertEquals(0, count("users"));
        assertEquals(0, count("study_sessions"));
        assertEquals(0, count("study_session_segments"));
    }

    private AudioChannelUnion channel(String id) {
        var channel = mock(AudioChannelUnion.class);
        when(channel.getId()).thenReturn(id);
        return channel;
    }

    private Member member(AudioChannelUnion channel, boolean cameraOn, boolean screenShareOn, boolean bot) {
        var member = mock(Member.class);
        var user = mock(User.class);
        var guild = mock(Guild.class);
        var state = mock(GuildVoiceState.class);
        when(member.getId()).thenReturn("learner");
        when(member.getUser()).thenReturn(user);
        when(member.getGuild()).thenReturn(guild);
        when(member.getVoiceState()).thenReturn(state);
        when(user.isBot()).thenReturn(bot);
        when(guild.getId()).thenReturn("guild");
        when(state.getChannel()).thenReturn(channel);
        when(state.isSendingVideo()).thenReturn(cameraOn);
        when(state.isStream()).thenReturn(screenShareOn);
        return member;
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableJpaRepositories(basePackageClasses = UserRepository.class)
    @EnableTransactionManagement
    @Import({UserIdentityServiceImpl.class, StudySessionServiceImpl.class, DiscordVoiceListener.class})
    static class TestConfiguration {

        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setPackagesToScan("com.studycommunity.entity");
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "validate"));
            return factory;
        }

        @Bean
        PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
            return new JpaTransactionManager(entityManagerFactory);
        }
    }
}
