package com.studycommunity.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.studycommunity.entity.StudyRoom;
import com.studycommunity.entity.StudySession;
import com.studycommunity.entity.StudySessionSegment;
import com.studycommunity.entity.User;
import com.studycommunity.entity.UserIdentity;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EntityMappingTest {

    @Test
    @DisplayName("Should bootstrap the five entity mappings without generating schema")
    void bootstrap_coreEntities_mapsEntitiesWithoutSchemaGeneration() {
        Configuration configuration = new Configuration()
                .addAnnotatedClass(User.class)
                .addAnnotatedClass(UserIdentity.class)
                .addAnnotatedClass(StudyRoom.class)
                .addAnnotatedClass(StudySession.class)
                .addAnnotatedClass(StudySessionSegment.class);
        configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        configuration.setProperty("hibernate.boot.allow_jdbc_metadata_access", "false");
        configuration.setProperty("jakarta.persistence.schema-generation.database.action", "none");
        configuration.setProperty("jakarta.persistence.schema-generation.scripts.action", "none");

        // Bootstrap metadata only; Flyway SQL owns all DDL and constraints.
        try (var sessionFactory = configuration.buildSessionFactory()) {
            assertEquals(5, sessionFactory.getMetamodel().getEntities().size());
        }
    }
}
