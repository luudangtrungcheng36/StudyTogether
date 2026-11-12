package com.studycommunity.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.studycommunity.entity.StudyRoom;
import com.studycommunity.entity.StudySession;
import com.studycommunity.entity.StudySessionSegment;
import com.studycommunity.entity.User;
import com.studycommunity.entity.UserIdentity;
import com.studycommunity.enums.StudySource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

// TEST_DB_* must identify a fresh disposable PostgreSQL database, never retained data.
// This test does not start Spring or Discord and never uses the default datasource.
@EnabledIfEnvironmentVariable(named = "TEST_DB_URL", matches = ".+")
class SqlSchemaTest {

    @Test
    @DisplayName("Should apply draft SQL to isolated PostgreSQL and verify mappings and constraints")
    void applyDraftSql_freshPostgreSql_validatesMappingsAndEnforcesSqlConstraints() throws SQLException {
        String url = System.getenv("TEST_DB_URL");
        String username = System.getenv("TEST_DB_USERNAME");
        String password = System.getenv("TEST_DB_PASSWORD");
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            ScriptUtils.executeSqlScript(connection, new FileSystemResource("docs/sql/schema.sql"));
        }

        User user = new User();
        user.setDisplayName("Migration test");
        UserIdentity identity = new UserIdentity();
        identity.setUser(user);
        identity.setProviderSubject("migration-test-discord");
        StudyRoom room = new StudyRoom();
        room.setDiscordGuildId("100");
        room.setDiscordChannelId("200");
        StudySession studySession = new StudySession();
        studySession.setSource(StudySource.DISCORD);
        studySession.setUser(user);
        studySession.setIdentity(identity);
        studySession.setStudyRoom(room);
        studySession.setStartedAt(Instant.parse("2026-10-31T00:00:00Z"));
        studySession.setLastObservedAt(studySession.getStartedAt());
        StudySessionSegment segment = new StudySessionSegment();
        segment.setStudySession(studySession);
        segment.setStartedAt(studySession.getStartedAt());

        Configuration configuration = new Configuration()
                .addAnnotatedClass(User.class)
                .addAnnotatedClass(UserIdentity.class)
                .addAnnotatedClass(StudyRoom.class)
                .addAnnotatedClass(StudySession.class)
                .addAnnotatedClass(StudySessionSegment.class);
        configuration.setProperty("hibernate.connection.url", url);
        configuration.setProperty("hibernate.connection.username", username);
        configuration.setProperty("hibernate.connection.password", password);
        configuration.setProperty("hibernate.hbm2ddl.auto", "validate");
        try (var sessionFactory = configuration.buildSessionFactory()) {
            try (var session = sessionFactory.openSession()) {
                var transaction = session.beginTransaction();
                session.persist(user);
                session.persist(identity);
                session.persist(room);
                session.persist(studySession);
                session.persist(segment);
                transaction.commit();
            }
            try (var session = sessionFactory.openSession()) {
                StudySession loaded = session.find(StudySession.class, studySession.getId());
                assertEquals(user.getId(), loaded.getUser().getId());
                assertEquals(identity.getId(), loaded.getIdentity().getId());
                assertEquals(room.getId(), loaded.getStudyRoom().getId());
                assertEquals(studySession.getStartedAt(), loaded.getStartedAt());
                assertEquals(StudySource.DISCORD, loaded.getSource());
                assertNotNull(loaded.getCreatedAt());
            }
        }

        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            connection.setAutoCommit(false);
            try (var statement = connection.createStatement()) {
                try (var result = statement.executeQuery("""
                        SELECT count(*) FROM pg_constraint
                        WHERE connamespace = 'public'::regnamespace AND contype = 'c'
                        """)) {
                    result.next();
                    assertEquals(0, result.getInt(1), "SQL migrations must keep CHECK constraints deferred");
                }
            }
            assertSqlState(connection, "23505", """
                    INSERT INTO user_identities (user_id, provider, provider_subject)
                    VALUES (?, 'DISCORD', 'migration-test-discord')
                    """, user.getId());
            assertSqlState(connection, "23001",
                    "DELETE FROM user_identities WHERE id = ?", identity.getId());
            assertSqlState(connection, "23503", """
                    INSERT INTO study_sessions (user_id, source, identity_id, study_room_id,
                                                started_at, last_observed_at)
                    VALUES (?, 'DISCORD', ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """, UUID.randomUUID(), identity.getId(), room.getId());
            assertSqlState(connection, "23505", """
                    INSERT INTO study_sessions (user_id, source, identity_id, study_room_id,
                                                started_at, last_observed_at)
                    VALUES (?, 'DISCORD', ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """, user.getId(), identity.getId(), room.getId());
            assertSqlState(connection, "23502", """
                    INSERT INTO study_sessions (user_id, identity_id, study_room_id,
                                                started_at, last_observed_at)
                    VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """, user.getId(), identity.getId(), room.getId());
            assertSqlState(connection, "23505", """
                    INSERT INTO study_session_segments (study_session_id, started_at)
                    VALUES (?, CURRENT_TIMESTAMP)
                    """, studySession.getId());

            try (var statement = connection.prepareStatement("""
                    INSERT INTO study_sessions (user_id, source, identity_id, study_room_id,
                                                started_at, last_observed_at, ended_at)
                    VALUES (?, 'DISCORD', ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """)) {
                statement.setObject(1, user.getId());
                statement.setObject(2, identity.getId());
                statement.setObject(3, room.getId());
                assertEquals(1, statement.executeUpdate());
                assertEquals(1, statement.executeUpdate(),
                        "Completed sessions must coexist with the open session and each other");
            }
            connection.rollback();
        }
    }

    private void assertSqlState(Connection connection, String sqlState, String sql, UUID... ids)
            throws SQLException {
        var savepoint = connection.setSavepoint();
        try (var statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < ids.length; index++) {
                statement.setObject(index + 1, ids[index]);
            }
            SQLException exception = assertThrows(SQLException.class, statement::executeUpdate);
            assertEquals(sqlState, exception.getSQLState());
        } finally {
            connection.rollback(savepoint);
            connection.releaseSavepoint(savepoint);
        }
    }
}
