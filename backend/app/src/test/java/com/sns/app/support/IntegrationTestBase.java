package com.sns.app.support;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared Testcontainers fixtures for @SpringBootTest-based integration tests. Spins up
 * Postgres+PostGIS and Redis containers via {@code @ServiceConnection} so tests don't have
 * to duplicate boilerplate.
 *
 * <p>Subclasses should add their own {@code @DynamicPropertySource} block for behaviour flags
 * (dev-mode verification, seed events, sweep intervals, etc.) and must annotate the class with
 * {@code @SpringBootTest @AutoConfigureMockMvc @Tag("integration")}.
 */
@Testcontainers
public abstract class IntegrationTestBase {

    @Container
    @ServiceConnection
    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
        DockerImageName.parse("postgis/postgis:15-3.4").asCompatibleSubstituteFor("postgres")
    );

    @Container
    @ServiceConnection
    protected static final RedisContainer REDIS = new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    @DynamicPropertySource
    static void baseProps(DynamicPropertyRegistry r) {
        r.add("spring.mail.host", () -> "localhost");
        r.add("spring.mail.port", () -> 1025);
        r.add("sns.verification.dev-mode", () -> true);
        r.add("sns.dev.seed-events", () -> true);
        r.add("sns.matching.sweep-interval-ms", () -> "3600000");
        r.add("sns.push.drain-interval-ms", () -> "86400000");
    }
}
