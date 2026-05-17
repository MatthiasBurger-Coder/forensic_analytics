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
            new AnalysisStoreServiceProperties.Health(true, "127.0.0.1", 0)
        ));
        assertThrows(NullPointerException.class, () -> new AnalysisStoreServiceProperties(
            new AnalysisStoreServiceProperties.Grpc(true, "127.0.0.1", 0),
            null
        ));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisStoreServiceProperties.Grpc(true, " ", 0));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisStoreServiceProperties.Grpc(true, null, 0));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisStoreServiceProperties.Health(true, "127.0.0.1", -1));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisStoreServiceProperties.Health(true, "127.0.0.1", 65_536));

        var properties = new AnalysisStoreServiceProperties(
            new AnalysisStoreServiceProperties.Grpc(false, "127.0.0.1", 0),
            new AnalysisStoreServiceProperties.Health(false, "127.0.0.1", 0)
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
            new AnalysisStoreServiceProperties.Health(true, "127.0.0.1", 0)
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
}
