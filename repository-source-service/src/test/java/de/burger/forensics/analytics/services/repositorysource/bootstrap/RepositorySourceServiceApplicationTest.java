package de.burger.forensics.analytics.services.repositorysource.bootstrap;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.net.URI;
import java.nio.file.Path;
import java.util.UUID;
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
    void startsGrpcAndHealthEndpointsWithH2Persistence() throws Exception {
        var storageId = "context-" + UUID.randomUUID();

        try (var context = new SpringApplicationBuilder(RepositorySourceServiceApplication.class)
            .web(WebApplicationType.NONE)
            .run(
                "--spring.main.banner-mode=off",
                "--forensics.repository-source.service.grpc.port=0",
                "--forensics.repository-source.service.health.port=0",
                "--forensics.repository-source.service.workspace.root=build/repository-source-test-workspaces/" + storageId,
                "--forensics.repository-source.service.persistence.type=h2",
                "--forensics.repository-source.service.persistence.h2.jdbc-url=jdbc:h2:file:./build/repository-source-test-data/" + storageId + "/repository-source;AUTO_SERVER=FALSE;DB_CLOSE_DELAY=-1"
            )) {

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
    void healthProbeAndDisabledLifecyclesBehaveDeterministically() {
        var properties = new RepositorySourceServiceProperties(
            new RepositorySourceServiceProperties.Grpc(false, "127.0.0.1", 0),
            new RepositorySourceServiceProperties.Health(false, "127.0.0.1", 0),
            new RepositorySourceServiceProperties.Workspace(Path.of("build/test-workspaces")),
            memoryPersistence()
        );
        var grpc = new GrpcServerLifecycle(properties, null);
        var health = new HealthHttpServerLifecycle(properties, grpc);

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
        assertThrows(IllegalArgumentException.class, () -> new RepositorySourceServiceProperties.Persistence("postgres", h2()));
        assertThrows(IllegalArgumentException.class, () -> new RepositorySourceServiceProperties.H2(" ", "sa", ""));
        assertThrows(IllegalArgumentException.class, () -> new RepositorySourceServiceProperties.H2(
            "jdbc:h2:tcp://127.0.0.1/~/repository-source",
            "sa",
            ""
        ));
        assertThrows(IllegalArgumentException.class, () -> new RepositorySourceServiceProperties.H2(
            "jdbc:h2:file:../repository-source;AUTO_SERVER=FALSE",
            "sa",
            ""
        ));
        assertThrows(IllegalArgumentException.class, () -> new RepositorySourceServiceProperties.H2(
            "jdbc:h2:file:build/repository-source-data/repository-source;INIT=RUNSCRIPT FROM 'classpath:init.sql'",
            "sa",
            ""
        ));

        var properties = new RepositorySourceServiceProperties(
            new RepositorySourceServiceProperties.Grpc(true, "127.0.0.1", 0),
            new RepositorySourceServiceProperties.Health(true, "127.0.0.1", 0),
            new RepositorySourceServiceProperties.Workspace(Path.of("build/test-workspaces")),
            memoryPersistence()
        );
        var grpc = new GrpcServerLifecycle(properties, null);
        var health = new HealthHttpServerLifecycle(properties, grpc);

        try {
            health.start();
            assertEquals(503, healthResponseCode(health.port()));
        } finally {
            health.stop();
        }
    }

    @Test
    void selectsH2PersistenceAndNormalizesSafeH2Defaults() {
        var h2 = new RepositorySourceServiceProperties.H2(
            "jdbc:h2:file:./build/repository-source-test-data/repository-source;AUTO_SERVER=FALSE;DB_CLOSE_DELAY=-1",
            null,
            null
        );
        var properties = new RepositorySourceServiceProperties(
            new RepositorySourceServiceProperties.Grpc(false, "127.0.0.1", 0),
            new RepositorySourceServiceProperties.Health(false, "127.0.0.1", 0),
            new RepositorySourceServiceProperties.Workspace(Path.of("build/test-workspaces")),
            new RepositorySourceServiceProperties.Persistence("H2", h2)
        );

        var components = new RepositorySourceServiceConfiguration().repositorySourcePersistenceComponents(properties);

        assertTrue(properties.persistence().useH2());
        assertEquals("", properties.persistence().h2().username());
        assertEquals("", properties.persistence().h2().password());
        assertNotNull(components.preparationRepository());
        assertNotNull(components.workspaceRepository());
        assertNotNull(components.idempotencyRepository());
        assertDoesNotThrowH2Path("jdbc:h2:file:/var/lib/forensic-analytics/repository-source-data/repository-source");
        assertThrows(IllegalArgumentException.class, () -> new RepositorySourceServiceProperties.H2(
            "jdbc:h2:file:~/repository-source",
            "sa",
            ""
        ));
        assertThrows(IllegalArgumentException.class, () -> new RepositorySourceServiceProperties.H2(
            "jdbc:h2:file:/tmp/repository-source",
            "sa",
            ""
        ));
    }

    private static int healthResponseCode(int port) throws Exception {
        var connection = URI.create("http://127.0.0.1:" + port + "/health").toURL().openConnection();
        return ((java.net.HttpURLConnection) connection).getResponseCode();
    }

    private static void assertDoesNotThrowH2Path(String jdbcUrl) {
        new RepositorySourceServiceProperties.H2(jdbcUrl, "sa", "");
    }

    private static RepositorySourceServiceProperties.Persistence memoryPersistence() {
        return new RepositorySourceServiceProperties.Persistence("memory", h2());
    }

    private static RepositorySourceServiceProperties.H2 h2() {
        return new RepositorySourceServiceProperties.H2(
            "jdbc:h2:file:./build/repository-source-test-data/repository-source",
            "sa",
            ""
        );
    }

}
