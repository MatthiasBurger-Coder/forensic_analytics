package de.burger.forensics.analytics.services.ingestion.bootstrap;

import de.burger.forensics.analytics.ingestion.v1.ForensicIngestionServiceGrpc;
import de.burger.forensics.analytics.ingestion.v1.IngestionStatus;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForensicIngestionServiceApplicationTest {
    @Test
    void startsGrpcAndHealthEndpointsWithEphemeralPorts() throws Exception {
        try (var context = new SpringApplicationBuilder(ForensicIngestionServiceApplication.class)
            .web(WebApplicationType.NONE)
            .profiles("test")
            .run("--spring.main.banner-mode=off")) {

            var grpc = context.getBean(GrpcServerLifecycle.class);
            var health = context.getBean(HealthHttpServerLifecycle.class);

            assertTrue(grpc.isRunning());
            assertTrue(grpc.port() > 0);
            assertTrue(health.isRunning());
            assertTrue(health.port() > 0);
            assertGrpcAcceptsSessionStart(grpc.port());
            assertEquals(200, healthResponseCode(health.port()));
        }
    }

    @Test
    void healthProbeReportsRunningService() {
        try (var context = new SpringApplicationBuilder(ForensicIngestionServiceApplication.class)
            .web(WebApplicationType.NONE)
            .profiles("test")
            .run("--spring.main.banner-mode=off")) {

            var health = context.getBean(HealthHttpServerLifecycle.class);

            assertEquals(0, HealthProbe.run(new String[] {
                "--forensics.ingestion.service.health.port=" + health.port()
            }));
        }
    }

    @Test
    void healthProbeRecognizesExplicitHealthcheckModeAndReportsUnavailableEndpoint() {
        assertTrue(HealthProbe.isHealthCheck(new String[] { "--healthcheck" }));
        assertFalse(HealthProbe.isHealthCheck(new String[] { "--spring.profiles.active=test" }));
        assertEquals(1, HealthProbe.run(new String[] {
            "--forensics.ingestion.service.health.host=127.0.0.1",
            "--forensics.ingestion.service.health.port=1"
        }));
    }

    @Test
    void rejectsInvalidServiceProperties() {
        assertThrows(NullPointerException.class, () -> new ForensicIngestionServiceProperties(
            null,
            new ForensicIngestionServiceProperties.Health(true, "127.0.0.1", 0)
        ));
        assertThrows(NullPointerException.class, () -> new ForensicIngestionServiceProperties(
            new ForensicIngestionServiceProperties.Grpc(true, "127.0.0.1", 0),
            null
        ));
        assertThrows(IllegalArgumentException.class, () -> new ForensicIngestionServiceProperties.Grpc(
            true,
            null,
            0
        ));
        assertThrows(IllegalArgumentException.class, () -> new ForensicIngestionServiceProperties.Grpc(
            true,
            " ",
            0
        ));
        assertThrows(IllegalArgumentException.class, () -> new ForensicIngestionServiceProperties.Health(
            true,
            "127.0.0.1",
            -1
        ));
        assertThrows(IllegalArgumentException.class, () -> new ForensicIngestionServiceProperties.Health(
            true,
            "127.0.0.1",
            65_536
        ));
    }

    @Test
    void disabledLifecyclesRemainStoppedAndExposeNoPort() {
        var properties = new ForensicIngestionServiceProperties(
            new ForensicIngestionServiceProperties.Grpc(false, "127.0.0.1", 0),
            new ForensicIngestionServiceProperties.Health(false, "127.0.0.1", 0)
        );
        var grpc = new GrpcServerLifecycle(properties, null);
        var health = new HealthHttpServerLifecycle(properties, grpc);

        grpc.start();
        health.start();
        grpc.stop();
        health.stop();

        assertFalse(grpc.isRunning());
        assertFalse(grpc.isAutoStartup());
        assertEquals(-1, grpc.port());
        assertFalse(health.isRunning());
        assertFalse(health.isAutoStartup());
        assertEquals(-1, health.port());
    }

    @Test
    void healthEndpointReportsDownWhenGrpcIsExpectedButStopped() throws Exception {
        var properties = new ForensicIngestionServiceProperties(
            new ForensicIngestionServiceProperties.Grpc(true, "127.0.0.1", 0),
            new ForensicIngestionServiceProperties.Health(true, "127.0.0.1", 0)
        );
        var grpc = new GrpcServerLifecycle(properties, null);
        var health = new HealthHttpServerLifecycle(properties, grpc);

        try {
            health.start();

            assertTrue(health.isRunning());
            assertTrue(health.isAutoStartup());
            assertEquals(503, healthResponseCode(health.port()));
        } finally {
            health.stop();
        }
    }

    private static void assertGrpcAcceptsSessionStart(int port) throws Exception {
        var channel = ManagedChannelBuilder.forAddress("127.0.0.1", port)
            .usePlaintext()
            .build();
        try {
            var response = ForensicIngestionServiceGrpc.newBlockingStub(channel)
                .startAnalysisSession(de.burger.forensics.analytics.ingestion.v1.StartAnalysisSessionRequest.newBuilder()
                    .setBuildIdentity(buildIdentity())
                    .setPluginIdentity(pluginIdentity())
                    .setSchemaVersion("schema-v1")
                    .build());

            assertEquals(IngestionStatus.INGESTION_STATUS_ACCEPTED, response.getStatus());
        } finally {
            channel.shutdownNow();
            assertTrue(channel.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private static int healthResponseCode(int port) throws Exception {
        var connection = URI.create("http://127.0.0.1:" + port + "/health").toURL().openConnection();
        return ((java.net.HttpURLConnection) connection).getResponseCode();
    }

    private static de.burger.forensics.analytics.ingestion.v1.BuildIdentity buildIdentity() {
        return de.burger.forensics.analytics.ingestion.v1.BuildIdentity.newBuilder()
            .setProjectId("project-a")
            .setRepositoryUrl("https://example.invalid/repo.git")
            .setBranchName("main")
            .setCommitHash("abcdef")
            .setBuildId("build-1")
            .setScanTimestamp("2026-05-16T00:00:00Z")
            .build();
    }

    private static de.burger.forensics.analytics.ingestion.v1.PluginIdentity pluginIdentity() {
        return de.burger.forensics.analytics.ingestion.v1.PluginIdentity.newBuilder()
            .setPluginName("forensic-plugin")
            .setPluginVersion("0.1.0")
            .build();
    }
}
