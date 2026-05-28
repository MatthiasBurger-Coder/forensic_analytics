package de.burger.forensics.analytics.services.analysisorchestrator.bootstrap;

import de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobId;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobServiceGrpc;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisRunId;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind;
import de.burger.forensics.analytics.analysisjob.v1.SourceSnapshotId;
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

class AnalysisOrchestratorServiceApplicationTest {
    @Test
    void startsGrpcAndHealthEndpointsWithEphemeralPorts() throws Exception {
        try (var context = new SpringApplicationBuilder(AnalysisOrchestratorServiceApplication.class)
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
        try (var context = new SpringApplicationBuilder(AnalysisOrchestratorServiceApplication.class)
            .web(WebApplicationType.NONE)
            .profiles("test")
            .run("--spring.main.banner-mode=off")) {

            var health = context.getBean(HealthHttpServerLifecycle.class);

            assertTrue(HealthProbe.isHealthCheck(new String[] { "--healthcheck" }));
            assertFalse(HealthProbe.isHealthCheck(new String[] { "--spring.profiles.active=test" }));
            assertEquals(0, HealthProbe.run(new String[] {
                "--forensics.analysis-orchestrator.service.health.port=" + health.port()
            }));
            assertEquals(1, HealthProbe.run(new String[] {
                "--forensics.analysis-orchestrator.service.health.host=127.0.0.1",
                "--forensics.analysis-orchestrator.service.health.port=1"
            }));
        }
    }

    @Test
    void rejectsInvalidServicePropertiesAndKeepsDisabledLifecyclesStopped() {
        assertThrows(NullPointerException.class, () -> new AnalysisOrchestratorServiceProperties(
            null,
            new AnalysisOrchestratorServiceProperties.Health(true, "127.0.0.1", 0)
        ));
        assertThrows(NullPointerException.class, () -> new AnalysisOrchestratorServiceProperties(
            new AnalysisOrchestratorServiceProperties.Grpc(true, "127.0.0.1", 0),
            null
        ));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisOrchestratorServiceProperties.Grpc(true, " ", 0));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisOrchestratorServiceProperties.Grpc(true, null, 0));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisOrchestratorServiceProperties.Health(true, "127.0.0.1", -1));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisOrchestratorServiceProperties.Health(true, "127.0.0.1", 65_536));

        var properties = new AnalysisOrchestratorServiceProperties(
            new AnalysisOrchestratorServiceProperties.Grpc(false, "127.0.0.1", 0),
            new AnalysisOrchestratorServiceProperties.Health(false, "127.0.0.1", 0)
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
        var properties = new AnalysisOrchestratorServiceProperties(
            new AnalysisOrchestratorServiceProperties.Grpc(true, "127.0.0.1", 0),
            new AnalysisOrchestratorServiceProperties.Health(true, "127.0.0.1", 0)
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
                .submitAnalysisJob(SubmitAnalysisJobRequest.newBuilder()
                    .setRequestId("request-bootstrap")
                    .setIdempotencyKey("submit-bootstrap")
                    .setSchemaVersion("schema-v1")
                    .setCorrelationId("correlation-1")
                    .setAnalysisRunId(AnalysisRunId.newBuilder().setValue("run-1"))
                    .setJobId(AnalysisJobId.newBuilder().setValue("job-bootstrap"))
                    .setWorkerKind(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS)
                    .setSourceSnapshotId(SourceSnapshotId.newBuilder().setValue("snapshot-1"))
                    .setInputCompleteness(AnalysisCompleteness.ANALYSIS_COMPLETENESS_UNKNOWN)
                    .build());

            assertEquals("ACCEPTED", response.getStatus().getCode());
        } finally {
            channel.shutdownNow();
            assertTrue(channel.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private static int healthResponseCode(int port) throws Exception {
        var connection = URI.create("http://127.0.0.1:" + port + "/health").toURL().openConnection();
        return ((java.net.HttpURLConnection) connection).getResponseCode();
    }
}
