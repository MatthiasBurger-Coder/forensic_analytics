package de.burger.forensics.analytics.services.queryreportapi.bootstrap;

import de.burger.forensics.analytics.services.queryreportapi.adapter.in.http.QueryReportApiHttpHandler;
import de.burger.forensics.analytics.services.queryreportapi.application.QueryReportApiRepositoryAnalysisSubmissionService;
import de.burger.forensics.analytics.services.queryreportapi.application.QueryReportApiStatusService;
import de.burger.forensics.analytics.services.queryreportapi.application.port.RepositoryAnalysisOwnerPort;
import de.burger.forensics.analytics.services.queryreportapi.domain.DownstreamServiceStatus;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.Diagnostic;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.RepositoryToBtmSubmission;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.RepositoryToBtmStatus;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.StatusRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.SubmissionRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiStatusSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.mock.env.MockEnvironment;

import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryReportApiServiceApplicationTest {
    @Test
    void dispatchesMainBranchesWithoutStartingSpringInHealthProbeMode() {
        var started = new AtomicBoolean();
        var exitCode = new AtomicInteger(-1);

        QueryReportApiServiceApplication.run(
            new String[] { "--healthcheck", "--forensics.query-report-api.service.http.port=1" },
            args -> started.set(true),
            exitCode::set
        );

        assertFalse(started.get());
        assertEquals(1, exitCode.get());

        QueryReportApiServiceApplication.run(
            new String[] { "--spring.main.banner-mode=off" },
            args -> started.set(true),
            exitCode::set
        );

        assertTrue(started.get());
    }

    @Test
    void startsHttpEndpointWithEphemeralPort() throws Exception {
        try (var context = new SpringApplicationBuilder(QueryReportApiServiceApplication.class)
            .web(WebApplicationType.NONE)
            .profiles("test")
            .run("--spring.main.banner-mode=off")) {

            var http = context.getBean(QueryReportApiHttpServerLifecycle.class);

            assertTrue(http.isRunning());
            assertTrue(http.port() > 0);
            assertEquals(200, responseCode(http.port(), "/api/health"));
            assertEquals(200, responseCode(http.port(), "/api/status"));
            assertEquals(0, HealthProbe.run(new String[] {
                "--forensics.query-report-api.service.http.host=127.0.0.1",
                "--forensics.query-report-api.service.http.port=" + http.port()
            }));

            http.start();
            assertTrue(http.isRunning());
        }
    }

    @Test
    void rejectsInvalidPropertiesAndKeepsDisabledLifecycleStopped() {
        assertThrows(NullPointerException.class, () -> new QueryReportApiServiceProperties(
            null,
            new QueryReportApiServiceProperties.AnalysisOrchestrator(
                new QueryReportApiServiceProperties.Grpc("127.0.0.1", 0, 1)
            )
        ));
        assertThrows(NullPointerException.class, () -> new QueryReportApiServiceProperties(
            new QueryReportApiServiceProperties.Http(true, "127.0.0.1", 0),
            null
        ));
        assertThrows(IllegalArgumentException.class, () -> new QueryReportApiServiceProperties.Http(true, " ", 0));
        assertThrows(IllegalArgumentException.class, () -> new QueryReportApiServiceProperties.Http(true, null, 0));
        assertThrows(IllegalArgumentException.class, () -> new QueryReportApiServiceProperties.Http(true, "127.0.0.1", -1));
        assertThrows(IllegalArgumentException.class, () -> new QueryReportApiServiceProperties.Http(true, "127.0.0.1", 65_536));
        assertThrows(NullPointerException.class, () -> new QueryReportApiServiceProperties.AnalysisOrchestrator(null));
        assertThrows(IllegalArgumentException.class, () -> new QueryReportApiServiceProperties.Grpc(" ", 9092, 5));
        assertThrows(IllegalArgumentException.class, () -> new QueryReportApiServiceProperties.Grpc("127.0.0.1", 65_536, 5));
        assertThrows(IllegalArgumentException.class, () -> new QueryReportApiServiceProperties.Grpc("127.0.0.1", 9092, 0));
        assertThrows(IllegalArgumentException.class, () -> new DownstreamServiceStatus(" ", "UP"));
        assertThrows(IllegalArgumentException.class, () -> new QueryReportApiStatusSnapshot(" ", List.of()));

        var properties = new QueryReportApiServiceProperties(
            new QueryReportApiServiceProperties.Http(false, "127.0.0.1", 0),
            new QueryReportApiServiceProperties.AnalysisOrchestrator(
                new QueryReportApiServiceProperties.Grpc("127.0.0.1", 0, 1)
            )
        );
        var lifecycle = new QueryReportApiHttpServerLifecycle(properties, new QueryReportApiHttpHandler(
            new QueryReportApiStatusService(),
            new QueryReportApiRepositoryAnalysisSubmissionService(new FakePreparationPort())
        ));

        lifecycle.start();
        lifecycle.stop();

        assertFalse(lifecycle.isRunning());
        assertFalse(lifecycle.isAutoStartup());
        assertEquals(-1, lifecycle.port());
        assertTrue(HealthProbe.isHealthCheck(new String[] { "--healthcheck" }));
        assertFalse(HealthProbe.isHealthCheck(new String[] { "--spring.profiles.active=test" }));
        assertEquals(1, HealthProbe.run(new String[] {
            "--forensics.query-report-api.service.http.host=127.0.0.1",
            "--forensics.query-report-api.service.http.port=1"
        }));
    }

    @Test
    void readsAnalysisOrchestratorGrpcProperties() {
        var environment = new MockEnvironment()
            .withProperty("forensics.query-report-api.service.http.enabled", "false")
            .withProperty("forensics.query-report-api.service.http.host", "0.0.0.0")
            .withProperty("forensics.query-report-api.service.http.port", "18080")
            .withProperty("forensics.query-report-api.service.analysis-orchestrator.grpc.host", "analysis-orchestrator-service")
            .withProperty("forensics.query-report-api.service.analysis-orchestrator.grpc.port", "9098")
            .withProperty("forensics.query-report-api.service.analysis-orchestrator.grpc.deadline-seconds", "7");

        var properties = new QueryReportApiServicePropertiesConfiguration().queryReportApiServiceProperties(environment);

        assertFalse(properties.http().enabled());
        assertEquals("0.0.0.0", properties.http().host());
        assertEquals(18080, properties.http().port());
        assertEquals("analysis-orchestrator-service", properties.analysisOrchestrator().grpc().host());
        assertEquals(9098, properties.analysisOrchestrator().grpc().port());
        assertEquals(7, properties.analysisOrchestrator().grpc().deadlineSeconds());
    }

    @Test
    void preservesQueryReportApiDomainStatusModels() {
        var downstream = new DownstreamServiceStatus("repository-analysis-service", "UP");
        var snapshot = new QueryReportApiStatusSnapshot("UP", List.of(downstream));

        assertEquals("repository-analysis-service", downstream.name());
        assertEquals("UP", downstream.status());
        assertEquals("UP", snapshot.status());
        assertEquals(List.of(downstream), snapshot.services());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.services().add(downstream));

        assertThrows(IllegalArgumentException.class, () -> new DownstreamServiceStatus(null, "UP"));
        assertThrows(IllegalArgumentException.class, () -> new DownstreamServiceStatus("repository-analysis-service", " "));
        assertThrows(IllegalArgumentException.class, () -> new QueryReportApiStatusSnapshot(null, List.of()));
        assertThrows(NullPointerException.class, () -> new QueryReportApiStatusSnapshot("UP", null));
    }

    private static int responseCode(int port, String path) throws Exception {
        var connection = URI.create("http://127.0.0.1:" + port + path).toURL().openConnection();
        return ((java.net.HttpURLConnection) connection).getResponseCode();
    }

    private static final class FakePreparationPort implements RepositoryAnalysisOwnerPort {
        @Override
        public RepositoryToBtmSubmission start(SubmissionRequest request) {
            return new RepositoryToBtmSubmission(
                request.analysisRunId(),
                "ACCEPTED",
                "/repository-analyses/" + request.analysisRunId(),
                "/repository-analyses/" + request.analysisRunId() + "/jobs",
                "BTM_DELIVERY_NOT_READY",
                "BtmArtifactDeliveryService",
                request.correlationId(),
                List.of(Diagnostic.info("OK", "prepared"))
            );
        }

        @Override
        public RepositoryToBtmStatus status(StatusRequest request) {
            return new RepositoryToBtmStatus(
                request.analysisRunId(),
                null,
                null,
                null,
                "AVAILABLE",
                "ACCEPTED",
                "repository-to-btm",
                null,
                List.of(Diagnostic.info("OK", "loaded"))
            );
        }
    }
}
