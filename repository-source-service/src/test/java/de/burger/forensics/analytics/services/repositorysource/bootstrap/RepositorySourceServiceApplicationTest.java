package de.burger.forensics.analytics.services.repositorysource.bootstrap;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.mock.env.MockEnvironment;

import java.net.URI;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositorySourceServiceApplicationTest {
    @Test
    void dispatchesMainBranchesWithoutStartingSpringInHealthProbeMode() {
        var started = new AtomicBoolean();
        var exitCode = new AtomicInteger(-1);

        RepositorySourceServiceApplication.run(
            new String[] { "--healthcheck", "--forensics.repository-source.service.health.port=1" },
            args -> started.set(true),
            exitCode::set
        );

        assertFalse(started.get());
        assertEquals(1, exitCode.get());

        RepositorySourceServiceApplication.run(
            new String[] { "--spring.main.banner-mode=off" },
            args -> started.set(true),
            exitCode::set
        );

        assertTrue(started.get());
    }

    @Test
    void startsGrpcAndHealthEndpointsWithEphemeralPorts() throws Exception {
        try (var context = new SpringApplicationBuilder(RepositorySourceServiceApplication.class)
            .web(WebApplicationType.NONE)
            .profiles("test")
            .run("--spring.main.banner-mode=off")) {

            var grpc = context.getBean(GrpcServerLifecycle.class);
            var health = context.getBean(HealthHttpServerLifecycle.class);

            assertTrue(grpc.isRunning());
            assertTrue(grpc.port() > 0);
            assertTrue(health.isRunning());
            assertTrue(health.port() > 0);
            assertEquals(200, healthResponseCode(health.port()));
        }
    }

    @Test
    void rejectsH2AsRuntimePersistenceType() {
        var error = assertThrows(
            IllegalArgumentException.class,
            () -> new RepositorySourceServiceProperties.Persistence("h2", postgres())
        );

        assertEquals("persistence type must be memory or postgres", error.getMessage());
    }

    @Test
    void healthProbeAndDisabledLifecyclesBehaveDeterministically() {
        var properties = new RepositorySourceServiceProperties(
            new RepositorySourceServiceProperties.Grpc(false, "127.0.0.1", 0),
            new RepositorySourceServiceProperties.Health(false, "127.0.0.1", 0),
            new RepositorySourceServiceProperties.Workspace(Path.of("build/test-workspaces")),
            memoryPersistence()
        );
        var grpc = new GrpcServerLifecycle(properties, null);
        var health = new HealthHttpServerLifecycle(properties, grpc, RepositorySourceStorageReadiness.ready());

        grpc.start();
        health.start();
        grpc.stop();
        health.stop();

        assertTrue(HealthProbe.isHealthCheck(new String[] { "--healthcheck" }));
        assertFalse(HealthProbe.isHealthCheck(new String[] { "--spring.profiles.active=test" }));
        assertEquals(1, HealthProbe.run(new String[] {
            "--forensics.repository-source.service.health.host=127.0.0.1",
            "--forensics.repository-source.service.health.port=1"
        }));
        assertFalse(grpc.isRunning());
        assertFalse(grpc.isAutoStartup());
        assertEquals(-1, grpc.port());
        assertFalse(health.isRunning());
        assertFalse(health.isAutoStartup());
        assertEquals(-1, health.port());
    }

    @Test
    void rejectsInvalidServicePropertiesAndReportsDownWhenGrpcStopped() throws Exception {
        assertThrows(NullPointerException.class, () -> new RepositorySourceServiceProperties(
            null,
            new RepositorySourceServiceProperties.Health(true, "127.0.0.1", 0),
            new RepositorySourceServiceProperties.Workspace(Path.of("build/test-workspaces")),
            memoryPersistence()
        ));
        assertThrows(NullPointerException.class, () -> new RepositorySourceServiceProperties(
            new RepositorySourceServiceProperties.Grpc(true, "127.0.0.1", 0),
            null,
            new RepositorySourceServiceProperties.Workspace(Path.of("build/test-workspaces")),
            memoryPersistence()
        ));
        assertThrows(NullPointerException.class, () -> new RepositorySourceServiceProperties(
            new RepositorySourceServiceProperties.Grpc(true, "127.0.0.1", 0),
            new RepositorySourceServiceProperties.Health(true, "127.0.0.1", 0),
            null,
            memoryPersistence()
        ));
        assertThrows(NullPointerException.class, () -> new RepositorySourceServiceProperties(
            new RepositorySourceServiceProperties.Grpc(true, "127.0.0.1", 0),
            new RepositorySourceServiceProperties.Health(true, "127.0.0.1", 0),
            new RepositorySourceServiceProperties.Workspace(Path.of("build/test-workspaces")),
            null
        ));
        assertThrows(IllegalArgumentException.class, () -> new RepositorySourceServiceProperties.Grpc(true, " ", 0));
        assertThrows(IllegalArgumentException.class, () -> new RepositorySourceServiceProperties.Grpc(true, null, 0));
        assertThrows(IllegalArgumentException.class, () -> new RepositorySourceServiceProperties.Grpc(true, "127.0.0.1", -1));
        assertThrows(IllegalArgumentException.class, () -> new RepositorySourceServiceProperties.Health(true, "127.0.0.1", 65_536));
        assertThrows(IllegalArgumentException.class, () -> new RepositorySourceServiceProperties.Health(true, null, 0));
        assertThrows(NullPointerException.class, () -> new RepositorySourceServiceProperties.Workspace(null));
        assertThrows(IllegalArgumentException.class, () -> new RepositorySourceServiceProperties.Persistence("oracle", postgres()));
        assertThrows(IllegalArgumentException.class, () -> new RepositorySourceServiceProperties.Persistence("h2", postgres()));
        assertThrows(NullPointerException.class, () -> new RepositorySourceServiceProperties.Persistence("memory", null));
        assertThrows(IllegalArgumentException.class, () -> new RepositorySourceServiceProperties.Postgres(
            "jdbc:h2:file:./build/repository-source-data/repository-source",
            "forensic",
            "",
            "repository_source",
            "classpath:db/changelog/repository-source-workspace.postgresql.yaml"
        ));
        assertThrows(IllegalArgumentException.class, () -> new RepositorySourceServiceProperties.Postgres(
            "jdbc:postgresql://127.0.0.1:5432/forensic_analytics?password=secret",
            "forensic",
            "",
            "repository_source",
            "classpath:db/changelog/repository-source-workspace.postgresql.yaml"
        ));
        assertThrows(IllegalArgumentException.class, () -> new RepositorySourceServiceProperties.Postgres(
            "jdbc:postgresql://127.0.0.1:5432/forensic_analytics",
            " ",
            "",
            "repository_source",
            "classpath:db/changelog/repository-source-workspace.postgresql.yaml"
        ));
        assertThrows(IllegalArgumentException.class, () -> new RepositorySourceServiceProperties.Postgres(
            "jdbc:postgresql://127.0.0.1:5432/forensic_analytics",
            "forensic",
            "",
            "repository-source",
            "classpath:db/changelog/repository-source-workspace.postgresql.yaml"
        ));
        assertThrows(IllegalArgumentException.class, () -> new RepositorySourceServiceProperties.Postgres(
            "jdbc:postgresql://127.0.0.1:5432/forensic_analytics",
            "forensic",
            "",
            "repository_source",
            "file:db/changelog/repository-source-workspace.postgresql.yaml"
        ));

        var properties = new RepositorySourceServiceProperties(
            new RepositorySourceServiceProperties.Grpc(true, "127.0.0.1", 0),
            new RepositorySourceServiceProperties.Health(true, "127.0.0.1", 0),
            new RepositorySourceServiceProperties.Workspace(Path.of("build/test-workspaces")),
            memoryPersistence()
        );
        var grpc = new GrpcServerLifecycle(properties, null);
        var health = new HealthHttpServerLifecycle(properties, grpc, RepositorySourceStorageReadiness.ready());

        try {
            health.start();
            assertEquals(503, healthResponseCode(health.port()));
        } finally {
            health.stop();
        }
    }

    @Test
    void selectsMemoryPersistenceForTestsWithoutDatabaseRuntimeFallback() {
        var properties = new RepositorySourceServiceProperties(
            new RepositorySourceServiceProperties.Grpc(false, "127.0.0.1", 0),
            new RepositorySourceServiceProperties.Health(false, "127.0.0.1", 0),
            new RepositorySourceServiceProperties.Workspace(Path.of("build/test-workspaces")),
            new RepositorySourceServiceProperties.Persistence("memory", postgres())
        );

        var components = new RepositorySourceServiceConfiguration().repositorySourcePersistenceComponents(properties);

        assertNotNull(components.preparationRepository());
        assertNotNull(components.workspaceRepository());
        assertNotNull(components.idempotencyRepository());
    }

    @Test
    void selectsPostgreSQLPersistenceAfterRunningLiquibaseMigration() {
        var events = new ArrayList<String>();
        var configuration = new RepositorySourceServiceConfiguration(
            postgres -> events.add("migrate"),
            postgres -> {
                events.add("components");
                return memoryComponents();
            }
        );
        var properties = new RepositorySourceServiceProperties(
            new RepositorySourceServiceProperties.Grpc(false, "127.0.0.1", 0),
            new RepositorySourceServiceProperties.Health(false, "127.0.0.1", 0),
            new RepositorySourceServiceProperties.Workspace(Path.of("build/test-workspaces")),
            new RepositorySourceServiceProperties.Persistence("Postgres", postgres())
        );

        var components = configuration.repositorySourcePersistenceComponents(properties);

        assertTrue(properties.persistence().usePostgres());
        assertEquals(List.of("migrate", "components"), events);
        assertNotNull(components.preparationRepository());
        assertNotNull(components.workspaceRepository());
        assertNotNull(components.idempotencyRepository());
        assertTrue(components.storageReadiness().isReady());
    }

    @Test
    void sanitizesPostgreSQLMigrationFailureBeforeExposingBootstrapError() {
        var leakedDetails = "jdbc:postgresql://db:5432/forensic_analytics?password=secret repository_source.workspace "
            + "/var/lib/forensic-analytics/repository-workspaces";
        var configuration = new RepositorySourceServiceConfiguration(
            postgres -> {
                throw new IllegalStateException(leakedDetails);
            },
            postgres -> {
                throw new AssertionError("components must not be created after migration failure");
            }
        );
        var properties = new RepositorySourceServiceProperties(
            new RepositorySourceServiceProperties.Grpc(false, "127.0.0.1", 0),
            new RepositorySourceServiceProperties.Health(false, "127.0.0.1", 0),
            new RepositorySourceServiceProperties.Workspace(Path.of("build/test-workspaces")),
            new RepositorySourceServiceProperties.Persistence("postgres", postgres())
        );

        var error = assertThrows(
            IllegalStateException.class,
            () -> configuration.repositorySourcePersistenceComponents(properties)
        );

        assertEquals("Repository-source PostgreSQL storage is not ready", error.getMessage());
        assertDoesNotExposeStorageDetails(error.getMessage());
    }

    @Test
    void reportsStorageReadinessDownWithoutLeakingFailureDetails() throws Exception {
        var leakedDetails = "jdbc:postgresql://db:5432/forensic_analytics?password=secret repository_source.workspace "
            + "/var/lib/forensic-analytics/repository-workspaces";
        var properties = new RepositorySourceServiceProperties(
            new RepositorySourceServiceProperties.Grpc(false, "127.0.0.1", 0),
            new RepositorySourceServiceProperties.Health(true, "127.0.0.1", 0),
            new RepositorySourceServiceProperties.Workspace(Path.of("build/test-workspaces")),
            memoryPersistence()
        );
        var grpc = new GrpcServerLifecycle(properties, null);
        var health = new HealthHttpServerLifecycle(
            properties,
            grpc,
            () -> {
                throw new IllegalStateException(leakedDetails);
            }
        );

        try {
            health.start();
            var response = healthResponse(health.port());

            assertEquals(503, response.statusCode());
            assertEquals("{\"status\":\"DOWN\"}", response.body());
            assertDoesNotExposeStorageDetails(response.body());
        } finally {
            health.stop();
        }
    }

    @Test
    void bindsTypedPostgreSQLConfigurationWithoutConnectingToDatabase() {
        var environment = new MockEnvironment()
            .withProperty("forensics.repository-source.service.persistence.type", "postgres")
            .withProperty(
                "forensics.repository-source.service.persistence.postgres.jdbc-url",
                " jdbc:postgresql://127.0.0.1:5432/forensic_analytics "
            )
            .withProperty("forensics.repository-source.service.persistence.postgres.username", " forensic ")
            .withProperty("forensics.repository-source.service.persistence.postgres.password", "")
            .withProperty("forensics.repository-source.service.persistence.postgres.schema", " repository_source ")
            .withProperty(
                "forensics.repository-source.service.persistence.postgres.change-log",
                " classpath:db/changelog/repository-source-workspace.postgresql.yaml "
            );

        var properties = new RepositorySourceServicePropertiesConfiguration()
            .repositorySourceServiceProperties(environment);

        assertTrue(properties.persistence().usePostgres());
        assertEquals("jdbc:postgresql://127.0.0.1:5432/forensic_analytics", properties.persistence().postgres().jdbcUrl());
        assertEquals("forensic", properties.persistence().postgres().username());
        assertEquals("", properties.persistence().postgres().password());
        assertEquals("repository_source", properties.persistence().postgres().schema());
        assertEquals(
            "classpath:db/changelog/repository-source-workspace.postgresql.yaml",
            properties.persistence().postgres().changeLog()
        );
    }

    @Test
    void defaultsToPostgreSQLRuntimeConfigurationWithoutConnectingToDatabase() {
        var properties = new RepositorySourceServicePropertiesConfiguration()
            .repositorySourceServiceProperties(new MockEnvironment());

        assertTrue(properties.persistence().usePostgres());
        assertEquals("jdbc:postgresql://127.0.0.1:5432/forensic_analytics", properties.persistence().postgres().jdbcUrl());
        assertEquals("forensic", properties.persistence().postgres().username());
        assertEquals("", properties.persistence().postgres().password());
        assertEquals("repository_source", properties.persistence().postgres().schema());
    }

    @Test
    void rejectsUnsafeTypedPostgreSQLConfigurationBeforeRuntimeUse() {
        var environment = new MockEnvironment()
            .withProperty("forensics.repository-source.service.persistence.type", "memory")
            .withProperty(
                "forensics.repository-source.service.persistence.postgres.jdbc-url",
                "jdbc:postgresql://127.0.0.1:5432/forensic_analytics?user=forensic"
            );

        assertThrows(IllegalArgumentException.class, () -> new RepositorySourceServicePropertiesConfiguration()
            .repositorySourceServiceProperties(environment));
    }

    private static int healthResponseCode(int port) throws Exception {
        return healthResponse(port).statusCode();
    }

    private static HealthResponse healthResponse(int port) throws Exception {
        var connection = (HttpURLConnection) URI.create("http://127.0.0.1:" + port + "/health").toURL().openConnection();
        var statusCode = connection.getResponseCode();
        var responseBody = statusCode < 400 ? connection.getInputStream() : connection.getErrorStream();
        var body = responseBody == null ? "" : new String(responseBody.readAllBytes(), StandardCharsets.UTF_8);
        return new HealthResponse(statusCode, body);
    }

    private static void assertDoesNotExposeStorageDetails(String text) {
        assertFalse(text.contains("jdbc:postgresql"));
        assertFalse(text.contains("password"));
        assertFalse(text.contains("secret"));
        assertFalse(text.contains("repository_source"));
        assertFalse(text.contains("workspace"));
        assertFalse(text.contains("/var/lib"));
    }

    private static RepositorySourceServiceProperties.Persistence memoryPersistence() {
        return new RepositorySourceServiceProperties.Persistence("memory", postgres());
    }

    private static RepositorySourceServiceConfiguration.RepositorySourcePersistenceComponents memoryComponents() {
        var configuration = new RepositorySourceServiceConfiguration();
        var properties = new RepositorySourceServiceProperties(
            new RepositorySourceServiceProperties.Grpc(false, "127.0.0.1", 0),
            new RepositorySourceServiceProperties.Health(false, "127.0.0.1", 0),
            new RepositorySourceServiceProperties.Workspace(Path.of("build/test-workspaces")),
            memoryPersistence()
        );
        return configuration.repositorySourcePersistenceComponents(properties);
    }

    private static RepositorySourceServiceProperties.Postgres postgres() {
        return new RepositorySourceServiceProperties.Postgres(
            "jdbc:postgresql://127.0.0.1:5432/forensic_analytics_test",
            "forensic_test",
            "",
            "repository_source",
            "classpath:db/changelog/repository-source-workspace.postgresql.yaml"
        );
    }

    private record HealthResponse(int statusCode, String body) {
    }

}
