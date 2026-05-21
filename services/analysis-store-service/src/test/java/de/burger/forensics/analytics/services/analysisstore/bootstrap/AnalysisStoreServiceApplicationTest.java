package de.burger.forensics.analytics.services.analysisstore.bootstrap;

import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobServiceGrpc;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind;
import de.burger.forensics.analytics.analysisjob.v1.SubmitAnalysisJobRequest;
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

class AnalysisStoreServiceApplicationTest {
    @Test
    void startsGrpcAndHealthEndpointsWithEphemeralPorts() throws Exception {
        try (var context = new SpringApplicationBuilder(AnalysisStoreServiceApplication.class)
            .web(WebApplicationType.NONE)
            .profiles("test")
            .run("--spring.main.banner-mode=off")) {

            var grpc = context.getBean(GrpcServerLifecycle.class);
            var health = context.getBean(HealthHttpServerLifecycle.class);

            assertTrue(grpc.isRunning());
            assertTrue(grpc.port() > 0);
            assertTrue(health.isRunning());
            assertTrue(health.port() > 0);
            assertGrpcAcceptsJobSubmit(grpc.port());
            assertEquals(200, healthResponseCode(health.port()));

            grpc.start();
            health.start();
            assertTrue(grpc.isRunning());
            assertTrue(health.isRunning());
        }
    }

    @Test
    void healthProbeReportsRunningServiceAndUnavailableEndpoint() {
        try (var context = new SpringApplicationBuilder(AnalysisStoreServiceApplication.class)
            .web(WebApplicationType.NONE)
            .profiles("test")
            .run("--spring.main.banner-mode=off")) {

            var health = context.getBean(HealthHttpServerLifecycle.class);

            assertTrue(HealthProbe.isHealthCheck(new String[] { "--healthcheck" }));
            assertFalse(HealthProbe.isHealthCheck(new String[] { "--spring.profiles.active=test" }));
            assertEquals(0, HealthProbe.run(new String[] {
                "--forensics.analysis-store.service.health.port=" + health.port()
            }));
            assertEquals(1, HealthProbe.run(new String[] {
                "--forensics.analysis-store.service.health.host=127.0.0.1",
                "--forensics.analysis-store.service.health.port=1"
            }));
        }
    }

    @Test
    void rejectsInvalidServicePropertiesAndKeepsDisabledLifecyclesStopped() {
        assertThrows(NullPointerException.class, () -> new AnalysisStoreServiceProperties(
            null,
            new AnalysisStoreServiceProperties.Health(true, "127.0.0.1", 0),
            javaAstAnalysis(),
            repositoryAnalysis(),
            joernCpgAnalysis(),
            btmGeneration()
        ));
        assertThrows(NullPointerException.class, () -> new AnalysisStoreServiceProperties(
            new AnalysisStoreServiceProperties.Grpc(true, "127.0.0.1", 0),
            null,
            javaAstAnalysis(),
            repositoryAnalysis(),
            joernCpgAnalysis(),
            btmGeneration()
        ));
        assertThrows(NullPointerException.class, () -> new AnalysisStoreServiceProperties(
            new AnalysisStoreServiceProperties.Grpc(true, "127.0.0.1", 0),
            new AnalysisStoreServiceProperties.Health(true, "127.0.0.1", 0),
            null,
            repositoryAnalysis(),
            joernCpgAnalysis(),
            btmGeneration()
        ));
        assertThrows(NullPointerException.class, () -> new AnalysisStoreServiceProperties(
            new AnalysisStoreServiceProperties.Grpc(true, "127.0.0.1", 0),
            new AnalysisStoreServiceProperties.Health(true, "127.0.0.1", 0),
            javaAstAnalysis(),
            null,
            joernCpgAnalysis(),
            btmGeneration()
        ));
        assertThrows(NullPointerException.class, () -> new AnalysisStoreServiceProperties(
            new AnalysisStoreServiceProperties.Grpc(true, "127.0.0.1", 0),
            new AnalysisStoreServiceProperties.Health(true, "127.0.0.1", 0),
            javaAstAnalysis(),
            repositoryAnalysis(),
            null,
            btmGeneration()
        ));
        assertThrows(NullPointerException.class, () -> new AnalysisStoreServiceProperties(
            new AnalysisStoreServiceProperties.Grpc(true, "127.0.0.1", 0),
            new AnalysisStoreServiceProperties.Health(true, "127.0.0.1", 0),
            javaAstAnalysis(),
            repositoryAnalysis(),
            joernCpgAnalysis(),
            null
        ));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisStoreServiceProperties.Grpc(true, " ", 0));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisStoreServiceProperties.Grpc(true, null, 0));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisStoreServiceProperties.Health(true, "127.0.0.1", -1));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisStoreServiceProperties.Health(true, "127.0.0.1", 65_536));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisStoreServiceProperties.ClientGrpc(" ", 9093, 30, 1));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisStoreServiceProperties.ClientGrpc("example.com", 9093, 30, 1));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisStoreServiceProperties.ClientGrpc("127.0.0.1", -1, 30, 1));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisStoreServiceProperties.ClientGrpc("127.0.0.1", 9093, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisStoreServiceProperties.ClientGrpc("127.0.0.1", 9093, 30, 0));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisStoreServiceProperties.OwnerGrpc(" ", 9092, 30));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisStoreServiceProperties.OwnerGrpc("repo.example.test", 9092, 30));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisStoreServiceProperties.OwnerGrpc("127.0.0.1", -1, 30));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisStoreServiceProperties.OwnerGrpc("127.0.0.1", 9092, 0));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisStoreServiceProperties.JoernGrpc("127.0.0.1", 9094, 30, " ", "bundle-v1"));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisStoreServiceProperties.JoernGrpc("127.0.0.1", 9094, 30, "joern:latest", "bundle-v1"));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisStoreServiceProperties.JoernGrpc("127.0.0.1", 9094, 30, "https://registry.example/joern@sha256:" + "a".repeat(64), "bundle-v1"));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisStoreServiceProperties.JoernGrpc("127.0.0.1", 9094, 30, pinnedJoernImage(), " "));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisStoreServiceProperties.JoernGrpc("127.0.0.1", 9094, 30, pinnedJoernImage(), "bundle/v1"));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisStoreServiceProperties.JoernGrpc("127.0.0.1", 9094, 30, pinnedJoernImage(), "secret-bundle"));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisStoreServiceProperties.JoernGrpc("127.0.0.1", 9094, 30, pinnedJoernImage(), "token-bundle"));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisStoreServiceProperties.JoernGrpc("127.0.0.1", 9094, 30, pinnedJoernImage(), "password-bundle"));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisStoreServiceProperties.JoernGrpc("127.0.0.1", 9094, 30, pinnedJoernImage(), "ghp_bundle"));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisStoreServiceProperties.JoernGrpc("127.0.0.1", 9094, 30, pinnedJoernImage(), "sk-bundle"));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisStoreServiceProperties.BtmGrpc("btm.example.test", 9095, 30, 1));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisStoreServiceProperties.BtmGrpc("127.0.0.1", 9095, 30, 0));

        var properties = new AnalysisStoreServiceProperties(
            new AnalysisStoreServiceProperties.Grpc(false, "127.0.0.1", 0),
            new AnalysisStoreServiceProperties.Health(false, "127.0.0.1", 0),
            javaAstAnalysis(),
            repositoryAnalysis(),
            joernCpgAnalysis(),
            btmGeneration()
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
        var properties = new AnalysisStoreServiceProperties(
            new AnalysisStoreServiceProperties.Grpc(true, "127.0.0.1", 0),
            new AnalysisStoreServiceProperties.Health(true, "127.0.0.1", 0),
            javaAstAnalysis(),
            repositoryAnalysis(),
            joernCpgAnalysis(),
            btmGeneration()
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

    private static void assertGrpcAcceptsJobSubmit(int port) throws Exception {
        var channel = ManagedChannelBuilder.forAddress("127.0.0.1", port)
            .usePlaintext()
            .build();
        try {
            var response = AnalysisJobServiceGrpc.newBlockingStub(channel)
                .submitAnalysisJob(submitRequest(
                    "submit-bootstrap",
                    "job-bootstrap",
                    AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS
                ));

            assertEquals("ACCEPTED", response.getStatus().getCode());
        } finally {
            channel.shutdownNow();
            assertTrue(channel.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private static SubmitAnalysisJobRequest submitRequest(String idempotencyKey, String jobId, AnalysisWorkerKind workerKind) {
        return SubmitAnalysisJobRequest.newBuilder()
            .setRequestId("request-" + jobId)
            .setIdempotencyKey(idempotencyKey)
            .setSchemaVersion("schema-v1")
            .setCorrelationId("correlation-1")
            .setAnalysisRunId(de.burger.forensics.analytics.analysisjob.v1.AnalysisRunId.newBuilder()
                .setValue("run-1"))
            .setJobId(de.burger.forensics.analytics.analysisjob.v1.AnalysisJobId.newBuilder()
                .setValue(jobId))
            .setWorkerKind(workerKind)
            .setSourceSnapshotId(de.burger.forensics.analytics.analysisjob.v1.SourceSnapshotId.newBuilder()
                .setValue("snapshot-1"))
            .setInputCompleteness(de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.ANALYSIS_COMPLETENESS_UNKNOWN)
            .build();
    }

    private static int healthResponseCode(int port) throws Exception {
        var connection = URI.create("http://127.0.0.1:" + port + "/health").toURL().openConnection();
        return ((java.net.HttpURLConnection) connection).getResponseCode();
    }

    private static AnalysisStoreServiceProperties.JavaAstAnalysis javaAstAnalysis() {
        return new AnalysisStoreServiceProperties.JavaAstAnalysis(
            new AnalysisStoreServiceProperties.ClientGrpc("127.0.0.1", 9093, 30, 104_857_600)
        );
    }

    private static AnalysisStoreServiceProperties.RepositoryAnalysis repositoryAnalysis() {
        return new AnalysisStoreServiceProperties.RepositoryAnalysis(
            new AnalysisStoreServiceProperties.OwnerGrpc("127.0.0.1", 9092, 30)
        );
    }

    private static AnalysisStoreServiceProperties.JoernCpgAnalysis joernCpgAnalysis() {
        return new AnalysisStoreServiceProperties.JoernCpgAnalysis(
            new AnalysisStoreServiceProperties.JoernGrpc("127.0.0.1", 9094, 30, pinnedJoernImage(), "bundle-v1")
        );
    }

    private static AnalysisStoreServiceProperties.BtmGeneration btmGeneration() {
        return new AnalysisStoreServiceProperties.BtmGeneration(
            new AnalysisStoreServiceProperties.BtmGrpc("127.0.0.1", 9095, 30, 104_857_600)
        );
    }

    private static String pinnedJoernImage() {
        return "ghcr.io/joernio/joern@sha256:" + "a".repeat(64);
    }
}
