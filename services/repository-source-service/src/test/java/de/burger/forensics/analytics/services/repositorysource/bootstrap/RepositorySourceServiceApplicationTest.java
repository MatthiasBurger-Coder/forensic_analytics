package de.burger.forensics.analytics.services.repositorysource.bootstrap;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.net.URI;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void healthProbeAndDisabledLifecyclesBehaveDeterministically() {
        var properties = new RepositorySourceServiceProperties(
            new RepositorySourceServiceProperties.Grpc(false, "127.0.0.1", 0),
            new RepositorySourceServiceProperties.Health(false, "127.0.0.1", 0),
            new RepositorySourceServiceProperties.Workspace(Path.of("build/test-workspaces"))
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
            new RepositorySourceServiceProperties.Workspace(Path.of("build/test-workspaces"))
        ));
        assertThrows(NullPointerException.class, () -> new RepositorySourceServiceProperties(
            new RepositorySourceServiceProperties.Grpc(true, "127.0.0.1", 0),
            null,
            new RepositorySourceServiceProperties.Workspace(Path.of("build/test-workspaces"))
        ));
        assertThrows(NullPointerException.class, () -> new RepositorySourceServiceProperties(
            new RepositorySourceServiceProperties.Grpc(true, "127.0.0.1", 0),
            new RepositorySourceServiceProperties.Health(true, "127.0.0.1", 0),
            null
        ));
        assertThrows(IllegalArgumentException.class, () -> new RepositorySourceServiceProperties.Grpc(true, " ", 0));
        assertThrows(IllegalArgumentException.class, () -> new RepositorySourceServiceProperties.Grpc(true, null, 0));
        assertThrows(IllegalArgumentException.class, () -> new RepositorySourceServiceProperties.Grpc(true, "127.0.0.1", -1));
        assertThrows(IllegalArgumentException.class, () -> new RepositorySourceServiceProperties.Health(true, "127.0.0.1", 65_536));
        assertThrows(IllegalArgumentException.class, () -> new RepositorySourceServiceProperties.Health(true, null, 0));
        assertThrows(NullPointerException.class, () -> new RepositorySourceServiceProperties.Workspace(null));

        var properties = new RepositorySourceServiceProperties(
            new RepositorySourceServiceProperties.Grpc(true, "127.0.0.1", 0),
            new RepositorySourceServiceProperties.Health(true, "127.0.0.1", 0),
            new RepositorySourceServiceProperties.Workspace(Path.of("build/test-workspaces"))
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

    private static int healthResponseCode(int port) throws Exception {
        var connection = URI.create("http://127.0.0.1:" + port + "/health").toURL().openConnection();
        return ((java.net.HttpURLConnection) connection).getResponseCode();
    }

}
