package de.burger.forensics.analytics.services.queryreportapi.bootstrap;

import de.burger.forensics.analytics.services.queryreportapi.adapter.in.http.QueryReportApiHttpHandler;
import de.burger.forensics.analytics.services.queryreportapi.application.QueryReportApiRepositoryAnalysisSubmissionService;
import de.burger.forensics.analytics.services.queryreportapi.application.QueryReportApiStatusService;
import de.burger.forensics.analytics.services.queryreportapi.application.QueryReportApiWorkspaceService;
import de.burger.forensics.analytics.services.queryreportapi.application.port.RepositoryAnalysisOwnerPort;
import de.burger.forensics.analytics.services.queryreportapi.application.port.RepositoryWorkspaceOwnerPort;
import de.burger.forensics.analytics.services.queryreportapi.domain.DownstreamServiceStatus;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.Diagnostic;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.RepositoryToBtmSubmission;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.RepositoryToBtmStatus;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.StatusRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.SubmissionRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.BranchRefreshResponse;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.CleanupWorkspaceRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.CreateWorkspaceRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.GetWorkspaceRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.ListWorkspacesRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.PublicRepositoryIdentity;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.RefreshWorkspaceBranchRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.RepositoryIdentity;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceBranchResponse;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceCleanupResponse;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceFacadeConfiguration;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceListItemResponse;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceListResponse;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceMetadataRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceMetadataResponse;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspacePolicy;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceResponse;
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
            ),
            new QueryReportApiServiceProperties.RepositorySource(
                new QueryReportApiServiceProperties.Grpc("127.0.0.1", 0, 1)
            ),
            workspaceFacade()
        ));
        assertThrows(NullPointerException.class, () -> new QueryReportApiServiceProperties(
            new QueryReportApiServiceProperties.Http(true, "127.0.0.1", 0),
            null,
            new QueryReportApiServiceProperties.RepositorySource(
                new QueryReportApiServiceProperties.Grpc("127.0.0.1", 0, 1)
            ),
            workspaceFacade()
        ));
        assertThrows(NullPointerException.class, () -> new QueryReportApiServiceProperties(
            new QueryReportApiServiceProperties.Http(true, "127.0.0.1", 0),
            new QueryReportApiServiceProperties.AnalysisOrchestrator(
                new QueryReportApiServiceProperties.Grpc("127.0.0.1", 0, 1)
            ),
            null,
            workspaceFacade()
        ));
        assertThrows(NullPointerException.class, () -> new QueryReportApiServiceProperties(
            new QueryReportApiServiceProperties.Http(true, "127.0.0.1", 0),
            new QueryReportApiServiceProperties.AnalysisOrchestrator(
                new QueryReportApiServiceProperties.Grpc("127.0.0.1", 0, 1)
            ),
            new QueryReportApiServiceProperties.RepositorySource(
                new QueryReportApiServiceProperties.Grpc("127.0.0.1", 0, 1)
            ),
            null
        ));
        assertThrows(NullPointerException.class, () -> new QueryReportApiServiceProperties(
            new QueryReportApiServiceProperties.Http(true, "127.0.0.1", 0),
            new QueryReportApiServiceProperties.AnalysisOrchestrator(
                new QueryReportApiServiceProperties.Grpc("127.0.0.1", 0, 1)
            ),
            new QueryReportApiServiceProperties.RepositorySource(
                new QueryReportApiServiceProperties.Grpc("127.0.0.1", 0, 1)
            ),
            workspaceFacade(),
            null
        ));
        assertThrows(IllegalArgumentException.class, () -> new QueryReportApiServiceProperties.Http(true, " ", 0));
        assertThrows(IllegalArgumentException.class, () -> new QueryReportApiServiceProperties.Http(true, null, 0));
        assertThrows(IllegalArgumentException.class, () -> new QueryReportApiServiceProperties.Http(true, "127.0.0.1", -1));
        assertThrows(IllegalArgumentException.class, () -> new QueryReportApiServiceProperties.Http(true, "127.0.0.1", 65_536));
        assertThrows(NullPointerException.class, () -> new QueryReportApiServiceProperties.AnalysisOrchestrator(null));
        assertThrows(NullPointerException.class, () -> new QueryReportApiServiceProperties.RepositorySource(null));
        assertThrows(IllegalArgumentException.class, () -> new QueryReportApiServiceProperties.Grpc(" ", 9092, 5));
        assertThrows(IllegalArgumentException.class, () -> new QueryReportApiServiceProperties.Grpc("127.0.0.1", 65_536, 5));
        assertThrows(IllegalArgumentException.class, () -> new QueryReportApiServiceProperties.Grpc("127.0.0.1", 9092, 0));
        assertThrows(IllegalArgumentException.class, () -> new QueryReportApiServiceProperties.WorkspaceFacade(" ", 60, false, true, false, false, 60, 100_000));
        assertThrows(IllegalArgumentException.class, () -> new QueryReportApiServiceProperties.WorkspaceFacade("query-report-workspace.v1", 0, false, true, false, false, 60, 100_000));
        assertThrows(IllegalArgumentException.class, () -> new QueryReportApiServiceProperties.WorkspaceFacade("query-report-workspace.v1", 60, false, true, false, false, 0, 100_000));
        assertThrows(IllegalArgumentException.class, () -> new QueryReportApiServiceProperties.WorkspaceFacade("query-report-workspace.v1", 60, false, true, false, false, 60, 0));
        assertThrows(IllegalArgumentException.class, () -> new DownstreamServiceStatus(" ", "UP"));
        assertThrows(IllegalArgumentException.class, () -> new QueryReportApiStatusSnapshot(" ", List.of()));

        var properties = new QueryReportApiServiceProperties(
            new QueryReportApiServiceProperties.Http(false, "127.0.0.1", 0),
            new QueryReportApiServiceProperties.AnalysisOrchestrator(
                new QueryReportApiServiceProperties.Grpc("127.0.0.1", 0, 1)
            ),
            new QueryReportApiServiceProperties.RepositorySource(
                new QueryReportApiServiceProperties.Grpc("127.0.0.1", 0, 1)
            ),
            workspaceFacade()
        );
        var lifecycle = new QueryReportApiHttpServerLifecycle(properties, new QueryReportApiHttpHandler(
            new QueryReportApiStatusService(),
            new QueryReportApiRepositoryAnalysisSubmissionService(new FakePreparationPort()),
            workspaceService()
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
            .withProperty("forensics.query-report-api.service.analysis-orchestrator.grpc.deadline-seconds", "7")
            .withProperty("forensics.query-report-api.service.repository-source.grpc.host", "repository-source-service")
            .withProperty("forensics.query-report-api.service.repository-source.grpc.port", "9092")
            .withProperty("forensics.query-report-api.service.repository-source.grpc.deadline-seconds", "8")
            .withProperty("forensics.query-report-api.service.workspace.schema-version", "query-report-workspace.v2")
            .withProperty("forensics.query-report-api.service.workspace.metadata.timeout-seconds", "45")
            .withProperty("forensics.query-report-api.service.workspace.refresh.ephemeral", "false")
            .withProperty("forensics.query-report-api.service.workspace.refresh.allow-shallow-clone", "true")
            .withProperty("forensics.query-report-api.service.workspace.refresh.allow-partial-clone", "false")
            .withProperty("forensics.query-report-api.service.workspace.refresh.allow-sparse-checkout", "false")
            .withProperty("forensics.query-report-api.service.workspace.refresh.timeout-seconds", "46")
            .withProperty("forensics.query-report-api.service.workspace.refresh.max-workspace-bytes", "123456")
            .withProperty("forensics.query-report-api.service.settings.operator-token", "operator-token");

        var properties = new QueryReportApiServicePropertiesConfiguration().queryReportApiServiceProperties(environment);

        assertFalse(properties.http().enabled());
        assertEquals("0.0.0.0", properties.http().host());
        assertEquals(18080, properties.http().port());
        assertEquals("analysis-orchestrator-service", properties.analysisOrchestrator().grpc().host());
        assertEquals(9098, properties.analysisOrchestrator().grpc().port());
        assertEquals(7, properties.analysisOrchestrator().grpc().deadlineSeconds());
        assertEquals("repository-source-service", properties.repositorySource().grpc().host());
        assertEquals(9092, properties.repositorySource().grpc().port());
        assertEquals(8, properties.repositorySource().grpc().deadlineSeconds());
        assertEquals("query-report-workspace.v2", properties.workspaceFacade().schemaVersion());
        assertEquals(45, properties.workspaceFacade().metadataTimeoutSeconds());
        assertEquals(46, properties.workspaceFacade().refreshTimeoutSeconds());
        assertEquals(123456, properties.workspaceFacade().refreshMaxWorkspaceBytes());
        assertEquals("operator-token", properties.settingsFacade().operatorToken());
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

    private static QueryReportApiServiceProperties.WorkspaceFacade workspaceFacade() {
        return new QueryReportApiServiceProperties.WorkspaceFacade(
            "query-report-workspace.v1",
            60,
            false,
            true,
            false,
            false,
            60,
            1_073_741_824L
        );
    }

    private static QueryReportApiWorkspaceService workspaceService() {
        return new QueryReportApiWorkspaceService(
            new FakeWorkspacePort(),
            new WorkspaceFacadeConfiguration(
                "query-report-workspace.v1",
                60,
                new WorkspacePolicy(false, true, false, false, 60, 1_073_741_824L)
            )
        );
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

    private static final class FakeWorkspacePort implements RepositoryWorkspaceOwnerPort {
        @Override
        public WorkspaceMetadataResponse previewMetadata(WorkspaceMetadataRequest request) {
            return new WorkspaceMetadataResponse(
                "example.com/acme/demo",
                "example.com",
                "acme",
                "demo",
                "demo",
                "main",
                List.of("main"),
                List.of(Diagnostic.info("OK", "loaded"))
            );
        }

        @Override
        public WorkspaceResponse create(CreateWorkspaceRequest request) {
            return workspace();
        }

        @Override
        public WorkspaceResponse get(GetWorkspaceRequest request) {
            return workspace();
        }

        @Override
        public WorkspaceListResponse list(ListWorkspacesRequest request) {
            return new WorkspaceListResponse(
                List.of(new WorkspaceListItemResponse(
                    "workspace-0001",
                    "demo",
                    new PublicRepositoryIdentity(
                        "example.com/acme/demo",
                        "example.com",
                        "acme",
                        "demo"
                    ),
                    workspace().branches(),
                    "CHECKED_OUT",
                    List.of(Diagnostic.info("OK", "loaded"))
                )),
                List.of(Diagnostic.info("OK", "loaded"))
            );
        }

        @Override
        public WorkspaceCleanupResponse cleanup(CleanupWorkspaceRequest request) {
            return new WorkspaceCleanupResponse(
                "workspace-0001",
                "CLEANED",
                List.of(Diagnostic.info("OK", "cleaned"))
            );
        }

        @Override
        public BranchRefreshResponse refresh(RefreshWorkspaceBranchRequest request) {
            return new BranchRefreshResponse(
                "workspace-branch-0001",
                "main",
                "UP_TO_DATE",
                false,
                null,
                "abcdef1",
                "source-snapshot-0001",
                List.of(Diagnostic.info("OK", "loaded"))
            );
        }

        private static WorkspaceResponse workspace() {
            return new WorkspaceResponse(
                "workspace-0001",
                "demo",
                new RepositoryIdentity(
                    "example.com/acme/demo",
                    "https://example.com/acme/demo.git",
                    "example.com",
                    "acme",
                    "demo",
                    "main"
                ),
                List.of(new WorkspaceBranchResponse(
                    "workspace-branch-0001",
                    "main",
                    "CHECKED_OUT",
                    "abcdef1",
                    "source-snapshot-0001",
                    List.of("src/main/java"),
                    List.of(Diagnostic.info("OK", "loaded"))
                )),
                "CHECKED_OUT",
                List.of(Diagnostic.info("OK", "loaded"))
            );
        }
    }
}
