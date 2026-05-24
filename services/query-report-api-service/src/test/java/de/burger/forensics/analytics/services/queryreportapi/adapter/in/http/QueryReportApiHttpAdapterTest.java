package de.burger.forensics.analytics.services.queryreportapi.adapter.in.http;

import com.sun.net.httpserver.HttpServer;
import de.burger.forensics.analytics.services.queryreportapi.application.QueryReportApiIdempotencyConflictException;
import de.burger.forensics.analytics.services.queryreportapi.application.QueryReportApiRepositoryAnalysisException;
import de.burger.forensics.analytics.services.queryreportapi.application.QueryReportApiRepositoryAnalysisSubmissionService;
import de.burger.forensics.analytics.services.queryreportapi.application.QueryReportApiStatusService;
import de.burger.forensics.analytics.services.queryreportapi.application.QueryReportApiWorkspaceException;
import de.burger.forensics.analytics.services.queryreportapi.application.QueryReportApiWorkspaceService;
import de.burger.forensics.analytics.services.queryreportapi.application.port.RepositoryAnalysisOwnerPort;
import de.burger.forensics.analytics.services.queryreportapi.application.port.RepositoryWorkspaceOwnerPort;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.Diagnostic;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.RepositoryToBtmSubmission;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.RepositoryToBtmStatus;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.StatusRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.SubmissionRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.BranchRefreshResponse;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.CreateWorkspaceRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.GetWorkspaceRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.RefreshWorkspaceBranchRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.RepositoryIdentity;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceBranchResponse;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceFacadeConfiguration;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceMetadataRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceMetadataResponse;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspacePolicy;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryReportApiHttpAdapterTest {
    @Test
    void exposesQueryReportApiShellRoutesOnly() throws Exception {
        var server = server();
        try {
            var port = server.getAddress().getPort();

            assertEquals(new Response(200, "{\"status\":\"UP\"}", null), response(port, "/health", "GET"));
            assertEquals(new Response(200, "{\"status\":\"UP\"}", null), response(port, "/api/health", "GET"));

            var status = response(port, "/api/status", "GET");
            assertEquals(200, status.code());
            assertTrue(status.body().contains("\"status\":\"UP\""));
            assertTrue(status.body().contains("\"name\":\"analysis-orchestrator-service\""));
            assertTrue(status.body().contains("\"status\":\"UNKNOWN\""));

            var notFound = response(port, "/api/repository-analyses", "GET");
            assertEquals(404, notFound.code());
            assertTrue(notFound.body().contains("NOT_FOUND"));

            var methodNotAllowed = response(port, "/api/status", "POST");
            assertEquals(405, methodNotAllowed.code());
            assertTrue(methodNotAllowed.body().contains("METHOD_NOT_ALLOWED"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void acceptsRepositoryAnalysisSubmissionThroughQueryReportApiFacade() throws Exception {
        var server = server();
        try {
            var port = server.getAddress().getPort();

            var accepted = response(port, "/api/repository-analyses", "POST", validRequest(), "correlation-1", "idem-1");
            var analysisRunId = valueOf(accepted.body(), "analysisRunId");
            var status = response(port, "/api/repository-analyses/" + analysisRunId, "GET", "", "correlation-2", null);

            assertEquals(202, accepted.code());
            assertEquals("correlation-1", accepted.correlationId());
            assertTrue(accepted.body().contains("\"status\":\"ACCEPTED\""));
            assertTrue(accepted.body().contains("\"btmDeliveryStatus\":\"BTM_DELIVERY_NOT_READY\""));
            assertTrue(accepted.body().contains("\"statusUrl\":\"/repository-analyses/analysis-run-"));
            assertTrue(accepted.body().contains("\"jobsUrl\":\"/repository-analyses/analysis-run-"));
            assertFalse(accepted.body().contains("workspace-"));
            assertFalse(accepted.body().contains("/tmp"));
            assertFalse(accepted.body().contains("token="));
            assertTrue(accepted.body().contains("Diagnostic details redacted"));
            assertEquals(200, status.code());
            assertEquals("correlation-2", status.correlationId());
            assertTrue(status.body().contains("\"analysisRunId\":\"" + analysisRunId + "\""));
            assertTrue(status.body().contains("\"status\":\"ACCEPTED\""));
            assertTrue(status.body().contains("\"workflow\":\"repository-to-btm\""));
            assertTrue(status.body().contains("\"sourceSnapshotStatus\":\"AVAILABLE\""));
            assertFalse(status.body().contains("\"statusUrl\""));
            assertFalse(status.body().contains("\"jobsUrl\""));
            assertFalse(status.body().contains("\"btmDeliveryStatus\""));
            assertFalse(status.body().contains("/tmp"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void extensionRoutesRemainUnavailableWithoutBtmReportOrReplayParity() throws Exception {
        var server = server();
        try {
            var port = server.getAddress().getPort();
            var unavailableRoutes = List.of(
                "/api/repository-analyses/analysis-run-1/jobs",
                "/api/repository-analyses/analysis-run-1/results",
                "/api/repository-analyses/analysis-run-1/replay",
                "/api/repository-analyses/analysis-run-1/reports"
            );

            for (var route : unavailableRoutes) {
                var response = response(port, route, "GET", "", "correlation-1", null);

                assertEquals(404, response.code(), route);
                assertTrue(response.body().contains("\"code\":\"NOT_FOUND\""), route);
                assertFalse(response.body().contains("BTM_DELIVERY_READY"), route);
                assertFalse(response.body().contains("workspace-"), route);
                assertFalse(response.body().contains("/tmp"), route);
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    void validatesRepositoryAnalysisStatusCorrelationHeader() throws Exception {
        var server = server();
        try {
            var port = server.getAddress().getPort();
            var missingCorrelation = response(
                port,
                "/api/repository-analyses/analysis-run-1",
                "GET",
                "",
                null,
                null
            );
            var unsafeCorrelation = response(
                port,
                "/api/repository-analyses/analysis-run-1",
                "GET",
                "",
                "bad\"correlation",
                null
            );

            assertEquals(400, missingCorrelation.code());
            assertTrue(missingCorrelation.body().contains("\"code\":\"VALIDATION_ERROR\""));
            assertTrue(missingCorrelation.body().contains("\"correlationId\":\"unknown\""));
            assertEquals(400, unsafeCorrelation.code());
            assertTrue(unsafeCorrelation.body().contains("\"code\":\"VALIDATION_ERROR\""));
            assertTrue(unsafeCorrelation.body().contains("\"correlationId\":\"unknown\""));
            assertFalse(unsafeCorrelation.body().contains("bad\"correlation"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsMissingHeadersUnsafeRemotesAndConflictingIdempotency() throws Exception {
        var server = server();
        try {
            var port = server.getAddress().getPort();

            assertEquals(400, response(port, "/api/repository-analyses", "POST", validRequest(), null, "idem-1").code());
            assertEquals(400, response(port, "/api/repository-analyses", "POST", validRequest(), "correlation-1", null).code());
            assertEquals(400, response(
                port,
                "/api/repository-analyses",
                "POST",
                validRequest().replace("https://example.com/acme/demo.git", "https://127.0.0.1/acme/demo.git"),
                "correlation-1",
                "idem-2"
            ).code());
            assertEquals(202, response(port, "/api/repository-analyses", "POST", validRequest(), "correlation-1", "idem-3").code());
            var conflict = response(
                port,
                "/api/repository-analyses",
                "POST",
                validRequest().replace("https://example.com/acme/demo.git", "https://example.com/acme/other.git"),
                "correlation-1",
                "idem-3"
            );

            assertEquals(409, conflict.code());
            assertTrue(conflict.body().contains("\"code\":\"CONFLICT\""));
            assertFalse(conflict.body().contains("/tmp"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void mapsUnsupportedMethodsMalformedPayloadsAndBackendFailures() throws Exception {
        var server = server();
        try {
            var port = server.getAddress().getPort();

            assertEquals(405, response(port, "/api/status", "PUT").code());
            assertEquals(404, response(port, "/api/unknown", "POST", "{}", "correlation-1", "idem-404").code());
            assertEquals(400, response(
                port,
                "/api/repository-analyses",
                "POST",
                validRequest().replace("\"ephemeral\": false,", ""),
                "correlation-1",
                "idem-missing-boolean"
            ).code());
            assertEquals(400, response(
                port,
                "/api/repository-analyses",
                "POST",
                validRequest().replace("\"maxWorkspaceBytes\": 100000", "\"maxWorkspaceBytes\": null"),
                "correlation-1",
                "idem-missing-long"
            ).code());
            assertEquals(400, response(
                port,
                "/api/repository-analyses",
                "POST",
                "x".repeat(64 * 1024 + 1),
                "correlation-1",
                "idem-too-large"
            ).code());
            var unsafeCorrelation = response(
                port,
                "/api/repository-analyses",
                "POST",
                validRequest(),
                "bad\"correlation",
                "idem-unsafe-correlation"
            );
            var unsafeIdempotency = response(
                port,
                "/api/repository-analyses",
                "POST",
                validRequest(),
                "correlation-1",
                "idem unsafe"
            );

            assertEquals(400, unsafeCorrelation.code());
            assertTrue(unsafeCorrelation.body().contains("\"correlationId\":\"unknown\""));
            assertFalse(unsafeCorrelation.body().contains("bad\"correlation"));
            assertEquals(400, unsafeIdempotency.code());
            assertEquals("correlation-1", unsafeIdempotency.correlationId());
        } finally {
            server.stop(0);
        }

        var failing = server(new FailingPreparationPort());
        try {
            var response = response(
                failing.getAddress().getPort(),
                "/api/repository-analyses",
                "POST",
                validRequest(),
                "correlation-1",
                "idem-backend"
            );

            assertEquals(503, response.code());
            assertEquals("correlation-1", response.correlationId());
            assertTrue(response.body().contains("\"retryable\":true"));
            assertFalse(response.body().contains("/tmp"));
        } finally {
            failing.stop(0);
        }
    }

    @Test
    void exposesRepositoryWorkspaceFacadeRoutes() throws Exception {
        var server = server();
        try {
            var port = server.getAddress().getPort();

            var metadata = response(
                port,
                "/api/workspace-metadata",
                "POST",
                workspaceMetadataRequest(),
                "correlation-workspace-1",
                "idem-workspace-metadata-1"
            );
            var created = response(
                port,
                "/api/workspaces",
                "POST",
                validWorkspaceRequest(),
                "correlation-workspace-2",
                "idem-workspace-create-1"
            );
            var loaded = response(
                port,
                "/api/workspaces/workspace-0001",
                "GET",
                "",
                "correlation-workspace-3",
                null
            );
            var refreshed = response(
                port,
                "/api/workspaces/workspace-0001/branches/workspace-branch-0001/refresh",
                "POST",
                "",
                "correlation-workspace-4",
                "idem-workspace-refresh-1"
            );

            assertEquals(200, metadata.code());
            assertEquals("correlation-workspace-1", metadata.correlationId());
            assertTrue(metadata.body().contains("\"repositoryKey\":\"example.com/acme/demo\""));
            assertTrue(metadata.body().contains("\"workspaceTitle\":\"demo\""));
            assertFalse(metadata.body().contains("\"workspaceId\""));
            assertSafePublicBody(metadata.body());

            assertEquals(200, created.code());
            assertEquals("correlation-workspace-2", created.correlationId());
            assertTrue(created.body().contains("\"workspaceId\":\"workspace-0001\""));
            assertTrue(created.body().contains("\"workspaceTitle\":\"demo\""));
            assertTrue(created.body().contains("\"status\":\"CHECKED_OUT\""));
            assertTrue(created.body().contains("\"sourceRoots\":[\"src/main/java\"]"));
            assertSafePublicBody(created.body());

            assertEquals(200, loaded.code());
            assertEquals("correlation-workspace-3", loaded.correlationId());
            assertTrue(loaded.body().contains("\"workspaceId\":\"workspace-0001\""));
            assertSafePublicBody(loaded.body());

            assertEquals(200, refreshed.code());
            assertEquals("correlation-workspace-4", refreshed.correlationId());
            assertTrue(refreshed.body().contains("\"workspaceBranchId\":\"workspace-branch-0001\""));
            assertTrue(refreshed.body().contains("\"status\":\"UP_TO_DATE\""));
            assertTrue(refreshed.body().contains("\"changed\":false"));
            assertSafePublicBody(refreshed.body());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsWorkspaceMissingHeadersUnsafeRemotesAndConflictingPreviewIdempotency() throws Exception {
        var server = server();
        try {
            var port = server.getAddress().getPort();

            assertEquals(400, response(port, "/api/workspace-metadata", "POST", workspaceMetadataRequest(), null, "idem-1").code());
            assertEquals(400, response(port, "/api/workspace-metadata", "POST", workspaceMetadataRequest(), "correlation-1", null).code());
            assertEquals(400, response(port, "/api/workspaces", "POST", validWorkspaceRequest(), "correlation-1", null).code());
            assertEquals(400, response(
                port,
                "/api/workspaces/workspace-0001/branches/workspace-branch-0001/refresh",
                "POST",
                "",
                "correlation-1",
                null
            ).code());
            assertEquals(400, response(
                port,
                "/api/workspaces",
                "POST",
                validWorkspaceRequest().replace("https://example.com/acme/demo.git", "https://127.0.0.1/acme/demo.git"),
                "correlation-1",
                "idem-2"
            ).code());
            assertEquals(400, response(
                port,
                "/api/workspaces",
                "POST",
                validWorkspaceRequest().replace("\"selectedBranch\": \"main\"", "\"selectedBranch\": \"\""),
                "correlation-1",
                "idem-blank-branch"
            ).code());
            assertEquals(400, response(
                port,
                "/api/workspaces/workspace-0001/branches/bad/refresh",
                "POST",
                "",
                "correlation-1",
                "idem-3"
            ).code());

            assertEquals(200, response(
                port,
                "/api/workspace-metadata",
                "POST",
                workspaceMetadataRequest(),
                "correlation-1",
                "idem-preview-conflict"
            ).code());
            var conflict = response(
                port,
                "/api/workspace-metadata",
                "POST",
                workspaceMetadataRequest().replace("https://example.com/acme/demo.git", "https://example.com/acme/other.git"),
                "correlation-1",
                "idem-preview-conflict"
            );

            assertEquals(409, conflict.code());
            assertTrue(conflict.body().contains("\"code\":\"IDEMPOTENCY_CONFLICT\""));
            assertTrue(conflict.body().contains("The idempotency key was already used with different input."));
            assertSafePublicBody(conflict.body());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void mapsOwnerIdempotencyConflictForCreateAndRefresh() throws Exception {
        var server = server(new FakePreparationPort(), new ConflictingWorkspacePort());
        try {
            var port = server.getAddress().getPort();
            var createConflict = response(
                port,
                "/api/workspaces",
                "POST",
                validWorkspaceRequest(),
                "correlation-1",
                "idem-conflict-create"
            );
            var refreshConflict = response(
                port,
                "/api/workspaces/workspace-0001/branches/workspace-branch-0001/refresh",
                "POST",
                "",
                "correlation-2",
                "idem-conflict-refresh"
            );

            assertEquals(409, createConflict.code());
            assertEquals("correlation-1", createConflict.correlationId());
            assertTrue(createConflict.body().contains("\"code\":\"IDEMPOTENCY_CONFLICT\""));
            assertEquals(409, refreshConflict.code());
            assertEquals("correlation-2", refreshConflict.correlationId());
            assertTrue(refreshConflict.body().contains("\"code\":\"IDEMPOTENCY_CONFLICT\""));
            assertSafePublicBody(createConflict.body());
            assertSafePublicBody(refreshConflict.body());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void redactsWorkspaceDiagnosticsAndMapsBackendFailures() throws Exception {
        var leaking = server(new FakePreparationPort(), new LeakingWorkspacePort());
        try {
            var response = response(
                leaking.getAddress().getPort(),
                "/api/workspaces",
                "POST",
                validWorkspaceRequest(),
                "correlation-1",
                "idem-workspace-leak"
            );

            assertEquals(200, response.code());
            assertTrue(response.body().contains("Diagnostic details redacted"));
            assertSafePublicBody(response.body());
        } finally {
            leaking.stop(0);
        }

        var failing = server(new FakePreparationPort(), new FailingWorkspacePort());
        try {
            var response = response(
                failing.getAddress().getPort(),
                "/api/workspaces",
                "POST",
                validWorkspaceRequest(),
                "correlation-1",
                "idem-workspace-backend"
            );

            assertEquals(503, response.code());
            assertEquals("correlation-1", response.correlationId());
            assertTrue(response.body().contains("\"code\":\"BACKEND_UNAVAILABLE\""));
            assertTrue(response.body().contains("\"retryable\":true"));
            assertSafePublicBody(response.body());
        } finally {
            failing.stop(0);
        }
    }

    private static HttpServer server() throws IOException {
        return server(new FakePreparationPort(), new FakeWorkspacePort());
    }

    private static HttpServer server(RepositoryAnalysisOwnerPort port) throws IOException {
        return server(port, new FakeWorkspacePort());
    }

    private static HttpServer server(RepositoryAnalysisOwnerPort port, RepositoryWorkspaceOwnerPort workspacePort) throws IOException {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", new QueryReportApiHttpHandler(
            new QueryReportApiStatusService(),
            new QueryReportApiRepositoryAnalysisSubmissionService(port),
            new QueryReportApiWorkspaceService(
                workspacePort,
                new WorkspaceFacadeConfiguration(
                    "query-report-workspace.v1",
                    60,
                    new WorkspacePolicy(false, true, false, false, 60, 1_073_741_824L)
                )
            )
        ));
        server.start();
        return server;
    }

    private static Response response(int port, String path, String method) throws IOException {
        return response(port, path, method, "", null, null);
    }

    private static Response response(
        int port,
        String path,
        String method,
        String body,
        String correlationId,
        String idempotencyKey
    ) throws IOException {
        var connection = (HttpURLConnection) URI.create("http://127.0.0.1:" + port + path).toURL().openConnection();
        connection.setRequestMethod(method);
        if (correlationId != null) {
            connection.setRequestProperty("X-Correlation-Id", correlationId);
        }
        if (idempotencyKey != null) {
            connection.setRequestProperty("Idempotency-Key", idempotencyKey);
        }
        if (!body.isEmpty()) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
        }
        var stream = connection.getResponseCode() >= 400 ? connection.getErrorStream() : connection.getInputStream();
        try (stream) {
            return new Response(
                connection.getResponseCode(),
                new String(stream.readAllBytes(), StandardCharsets.UTF_8),
                connection.getHeaderField("X-Correlation-Id")
            );
        }
    }

    private static String validRequest() {
        return """
            {
              "requestId": "request-1",
              "schemaVersion": "gateway.v1",
              "requestedOutputs": ["BTM_RULES"],
              "repositoryUrl": "https://example.com/acme/demo.git",
              "provider": "github",
              "branch": "main",
              "buildContext": {
                "buildTool": "gradle",
                "buildId": "build-1",
                "rootProjectName": "demo",
                "declaredModules": [":app"],
                "attributes": {"tenant": "demo"}
              },
              "workspacePolicy": {
                "ephemeral": false,
                "allowShallowClone": true,
                "allowPartialClone": false,
                "allowSparseCheckout": false,
                "timeoutSeconds": 60,
                "maxWorkspaceBytes": 100000
              }
            }
            """;
    }

    private static String workspaceMetadataRequest() {
        return """
            {
              "repositoryUrl": "https://example.com/acme/demo.git"
            }
            """;
    }

    private static String validWorkspaceRequest() {
        return """
            {
              "repositoryUrl": "https://example.com/acme/demo.git",
              "selectedBranch": "main",
              "workspacePolicy": {
                "ephemeral": false,
                "allowShallowClone": true,
                "allowPartialClone": false,
                "allowSparseCheckout": false,
                "timeoutSeconds": 60,
                "maxWorkspaceBytes": 1073741824
              }
            }
            """;
    }

    private static void assertSafePublicBody(String body) {
        assertFalse(body.contains("/tmp"), body);
        assertFalse(body.contains("/var/lib/forensic-analytics"), body);
        assertFalse(body.contains("repository-workspaces"), body);
        assertFalse(body.contains("repository-source-data"), body);
        assertFalse(body.contains("raw stdout"), body);
        assertFalse(body.contains("raw stderr"), body);
        assertFalse(body.contains("stdout"), body);
        assertFalse(body.contains("stderr"), body);
        assertFalse(body.contains("jdbc:"), body);
        assertFalse(body.toLowerCase(java.util.Locale.ROOT).contains("h2"), body);
        assertFalse(body.contains("token="), body);
        assertFalse(body.contains("credential"), body);
    }

    private static String valueOf(String json, String name) {
        var marker = "\"" + name + "\":\"";
        var start = json.indexOf(marker);
        if (start < 0) {
            throw new AssertionError("Missing JSON field " + name + " in " + json);
        }
        var valueStart = start + marker.length();
        var valueEnd = json.indexOf('"', valueStart);
        return json.substring(valueStart, valueEnd);
    }

    private record Response(int code, String body, String correlationId) {
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
                List.of(
                    Diagnostic.info("ORCHESTRATION_ACCEPTED", "Analysis Orchestrator accepted orchestration"),
                    Diagnostic.info("PATH_LEAK", "git clone https://example.com/private.git failed in /tmp/repository-workspaces/workspace-1 with token=abc")
                )
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
                List.of(Diagnostic.info("ORCHESTRATION_STATUS", "Analysis Orchestrator status loaded"))
            );
        }
    }

    private static class FakeWorkspacePort implements RepositoryWorkspaceOwnerPort {
        @Override
        public WorkspaceMetadataResponse previewMetadata(WorkspaceMetadataRequest request) {
            return new WorkspaceMetadataResponse(
                "example.com/acme/demo",
                "example.com",
                "acme",
                "demo",
                "demo",
                "main",
                List.of(Diagnostic.info("METADATA_READY", "Repository metadata loaded"))
            );
        }

        @Override
        public WorkspaceResponse create(CreateWorkspaceRequest request) {
            return workspace(List.of(Diagnostic.info("CHECKOUT_READY", "Workspace checkout completed")));
        }

        @Override
        public WorkspaceResponse get(GetWorkspaceRequest request) {
            return workspace(List.of(Diagnostic.info("WORKSPACE_READY", "Workspace state loaded")));
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
                List.of(Diagnostic.info("BRANCH_UP_TO_DATE", "Branch is already up to date"))
            );
        }

        protected WorkspaceResponse workspace(List<Diagnostic> diagnostics) {
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
                    diagnostics
                )),
                "CHECKED_OUT",
                diagnostics
            );
        }
    }

    private static final class LeakingWorkspacePort extends FakeWorkspacePort {
        @Override
        public WorkspaceResponse create(CreateWorkspaceRequest request) {
            return workspace(List.of(Diagnostic.info(
                "PATH_LEAK",
                "raw stderr stdout jdbc:h2:file:/var/lib/forensic-analytics/repository-source-data/repository-source from git clone https://user:token@example.com/acme/private.git in /var/lib/forensic-analytics/repository-workspaces/workspace-1"
            )));
        }
    }

    private static final class ConflictingWorkspacePort extends FakeWorkspacePort {
        @Override
        public WorkspaceResponse create(CreateWorkspaceRequest request) {
            throw conflict();
        }

        @Override
        public BranchRefreshResponse refresh(RefreshWorkspaceBranchRequest request) {
            throw conflict();
        }

        private static QueryReportApiIdempotencyConflictException conflict() {
            return new QueryReportApiIdempotencyConflictException("idempotency key was reused with different input");
        }
    }

    private static final class FailingWorkspacePort implements RepositoryWorkspaceOwnerPort {
        @Override
        public WorkspaceMetadataResponse previewMetadata(WorkspaceMetadataRequest request) {
            throw unavailable();
        }

        @Override
        public WorkspaceResponse create(CreateWorkspaceRequest request) {
            throw unavailable();
        }

        @Override
        public WorkspaceResponse get(GetWorkspaceRequest request) {
            throw unavailable();
        }

        @Override
        public BranchRefreshResponse refresh(RefreshWorkspaceBranchRequest request) {
            throw unavailable();
        }

        private static QueryReportApiWorkspaceException unavailable() {
            return new QueryReportApiWorkspaceException(
                503,
                "BACKEND_UNAVAILABLE",
                true,
                "Repository Source service is unavailable"
            );
        }
    }

    private static final class FailingPreparationPort implements RepositoryAnalysisOwnerPort {
        @Override
        public RepositoryToBtmSubmission start(SubmissionRequest request) {
            throw unavailable();
        }

        @Override
        public RepositoryToBtmStatus status(StatusRequest request) {
            throw unavailable();
        }

        private static QueryReportApiRepositoryAnalysisException unavailable() {
            return new QueryReportApiRepositoryAnalysisException(
                503,
                "BACKEND_UNAVAILABLE",
                true,
                "Repository analysis service is unavailable"
            );
        }
    }
}
