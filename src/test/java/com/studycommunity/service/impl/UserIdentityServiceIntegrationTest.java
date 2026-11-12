package com.studycommunity.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.studycommunity.entity.UserIdentity;
import com.studycommunity.enums.IdentityProvider;
import com.studycommunity.enums.UserStatus;
import com.studycommunity.repository.UserIdentityRepository;
import com.studycommunity.repository.UserRepository;
import com.studycommunity.service.UserIdentityService;
import jakarta.persistence.EntityManagerFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

// TEST_DB_* must target disposable PostgreSQL. Only this test's schema is created/dropped.
@EnabledIfEnvironmentVariable(named = "TEST_DB_URL", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserIdentityServiceIntegrationTest {

    private final String schema = "identity_service_test_" + UUID.randomUUID().toString().replace("-", "");
    private final DriverManagerDataSource dataSource = new DriverManagerDataSource();
    private AnnotationConfigApplicationContext context;
    private JdbcTemplate jdbcTemplate;
    private UserIdentityService userIdentityService;

    @BeforeAll
    void setUp() throws Exception {
        dataSource.setUrl(System.getenv("TEST_DB_URL"));
        dataSource.setUsername(System.getenv("TEST_DB_USERNAME"));
        dataSource.setPassword(System.getenv("TEST_DB_PASSWORD"));
        dataSource.setSchema(schema);
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("CREATE SCHEMA " + schema);
        String sql = Files.readString(Path.of("docs/sql/schema.sql"))
                .replace("public.", schema + ".");
        try (var connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection,
                    new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)));
        }

        context = new AnnotationConfigApplicationContext();
        context.getEnvironment().setActiveProfiles("test");
        context.registerBean(DataSource.class, () -> dataSource);
        context.register(TestConfiguration.class);
        context.refresh();
        userIdentityService = context.getBean(UserIdentityService.class);
    }

    @BeforeEach
    void clearUsers() {
        jdbcTemplate.update("DELETE FROM user_identities");
        jdbcTemplate.update("DELETE FROM users");
    }

    @AfterAll
    void tearDown() {
        if (context != null) {
            context.close();
        }
        if (jdbcTemplate != null) {
            jdbcTemplate.execute("DROP SCHEMA " + schema + " CASCADE");
        }
    }

    @Test
    @DisplayName("Should create a Discord identity and active user without recording a website login")
    void findOrCreateDiscordIdentity_newDiscordId_createsUserAndIdentity() {
        UserIdentity identity = userIdentityService.findOrCreateDiscordIdentity("123456");

        assertNotNull(identity.getId());
        assertNotNull(identity.getUser().getId());
        assertEquals(IdentityProvider.DISCORD, identity.getProvider());
        assertEquals("123456", identity.getProviderSubject());
        assertEquals(UserStatus.ACTIVE, identity.getUser().getStatus());
        assertNull(identity.getUser().getFirstWebLoginAt());
        assertNull(identity.getLastAuthenticatedAt());
        assertEquals(1, rowCount("users"));
        assertEquals(1, rowCount("user_identities"));
    }

    @Test
    @DisplayName("Should reuse the same owner and preserve profile and authentication history")
    void findOrCreateDiscordIdentity_existingDiscordId_reusesUserAndIdentity() {
        UserIdentity first = userIdentityService.findOrCreateDiscordIdentity("123456");
        Instant authenticatedAt = Instant.parse("2026-10-31T00:00:00Z");
        var transaction = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        transaction.executeWithoutResult(status -> {
            UserIdentity stored = context.getBean(UserIdentityRepository.class)
                    .findByProviderAndProviderSubject(IdentityProvider.DISCORD, "123456").orElseThrow();
            stored.getUser().setDisplayName("Existing learner");
            stored.getUser().setFirstWebLoginAt(authenticatedAt);
            stored.setLastAuthenticatedAt(authenticatedAt);
        });

        UserIdentity second = userIdentityService.findOrCreateDiscordIdentity("123456");

        assertEquals(first.getId(), second.getId());
        assertEquals(first.getUser().getId(), second.getUser().getId());
        assertEquals("Existing learner", second.getUser().getDisplayName());
        assertEquals(authenticatedAt, second.getUser().getFirstWebLoginAt());
        assertEquals(authenticatedAt, second.getLastAuthenticatedAt());
        assertEquals(1, rowCount("users"));
        assertEquals(1, rowCount("user_identities"));
    }

    @Test
    @DisplayName("Should propagate other database failures and roll back the new user")
    void findOrCreateDiscordIdentity_invalidStoredValue_rollsBackUserAndPropagatesFailure() {
        assertThrows(DataIntegrityViolationException.class,
                () -> userIdentityService.findOrCreateDiscordIdentity("1".repeat(256)));

        assertEquals(0, rowCount("users"));
        assertEquals(0, rowCount("user_identities"));
    }

    private int rowCount(String table) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableJpaRepositories(basePackageClasses = UserRepository.class)
    @EnableTransactionManagement
    @Import(UserIdentityServiceImpl.class)
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
