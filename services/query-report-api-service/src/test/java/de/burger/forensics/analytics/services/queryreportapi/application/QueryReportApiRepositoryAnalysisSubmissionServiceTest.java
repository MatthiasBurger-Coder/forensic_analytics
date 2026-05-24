package de.burger.forensics.analytics.services.queryreportapi.application;

import de.burger.forensics.analytics.services.queryreportapi.application.port.RepositoryAnalysisOwnerPort;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.BuildContext;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.Diagnostic;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.RepositoryToBtmSubmission;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.RepositoryToBtmStatus;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.StatusRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.SubmissionRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.WorkspacePolicy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryReportApiRepositoryAnalysisSubmissionServiceTest {
    @Test
    void submitsRepositoryPreparationAndReplaysIdempotentRequests() {
        var port = new CapturingPort();
        var service = new QueryReportApiRepositoryAnalysisSubmissionService(port);

        var submitted = service.submit(request("idem-1", "https://example.com/acme/demo.git", "main", ""));
        var replayed = service.submit(request("idem-1", "https://example.com/acme/demo.git", "main", ""));

        assertSame(submitted, replayed);
        assertEquals(1, port.calls);
        assertTrue(submitted.analysisRunId().startsWith("analysis-run-"));
        assertEquals("ACCEPTED", submitted.status());
        assertEquals("/repository-analyses/" + submitted.analysisRunId(), submitted.statusUrl());
        assertEquals("BTM_DELIVERY_NOT_READY", submitted.btmDeliveryStatus());
        assertEquals("correlation-1", submitted.correlationId());
        assertEquals(submitted.analysisRunId(), port.lastRequest.analysisRunId());
        assertFalse(submitted.toString().contains("workspace-"));
    }

    @Test
    void rejectsConflictingIdempotencyAndUnsafeRequestsBeforeCallingPort() {
        var port = new CapturingPort();
        var service = new QueryReportApiRepositoryAnalysisSubmissionService(port);
        service.submit(request("idem-1", "https://example.com/acme/demo.git", "main", ""));

        assertThrows(QueryReportApiIdempotencyConflictException.class, () -> service.submit(
            request("idem-1", "https://example.com/acme/other.git", "main", "")
        ));
        assertThrows(IllegalArgumentException.class, () -> request("idem-2", "file:/tmp/repo.git", "main", ""));
        assertThrows(IllegalArgumentException.class, () -> request("idem-2", "https://user@example.com/repo.git", "main", ""));
        assertThrows(IllegalArgumentException.class, () -> request("idem-2", "https://example.com/repo.git?token=x", "main", ""));
        assertThrows(IllegalArgumentException.class, () -> request("idem-2", "https://127.0.0.1/repo.git", "main", ""));
        assertThrows(IllegalArgumentException.class, () -> request("idem-2", "https://example.com/repo.git", "-main", ""));
        assertThrows(IllegalArgumentException.class, () -> request("idem-2", "https://example.com/repo.git", "", ""));
        assertThrows(IllegalArgumentException.class, () -> new SubmissionRequest(
            "request-1",
            "idem-2",
            "gateway.v1",
            "correlation-1",
            List.of("SOURCE_FACTS"),
            "https://example.com/repo.git",
            "",
            "main",
            "",
            "",
            buildContext(),
            policy()
        ));

        assertEquals(1, port.calls);
    }

    @Test
    void mapsCommitOnlyAndBranchCommitRequestsToPreparationCommand() {
        var port = new CapturingPort();
        var service = new QueryReportApiRepositoryAnalysisSubmissionService(port);

        service.submit(request("commit-only", "https://example.com/acme/demo.git", "", "abcdef1"));

        assertEquals("", port.lastRequest.branch());
        assertEquals("abcdef1", port.lastRequest.commit());
    }

    @Test
    void readsRepositoryToBtmStatusThroughOwnerPort() {
        var port = new CapturingPort();
        var service = new QueryReportApiRepositoryAnalysisSubmissionService(port);

        var status = service.status(new StatusRequest("request-status", "correlation-1", "analysis-run-1"));

        assertEquals("analysis-run-1", port.lastStatusRequest.analysisRunId());
        assertEquals("correlation-1", port.lastStatusRequest.correlationId());
        assertEquals("ACCEPTED", status.status());
        assertEquals("repository-to-btm", status.workflow());
        assertEquals("AVAILABLE", status.sourceSnapshotStatus());
        assertThrows(IllegalArgumentException.class, () -> new StatusRequest("request-status", "correlation-1", "/tmp/run"));
    }

    @Test
    void validatesRepositoryBoundariesAndWorkspacePolicyBeforeCallingPort() {
        var unsafeRemotes = List.of(
            "https://localhost/acme/demo.git",
            "https://localhost./acme/demo.git",
            "https://demo.localhost/acme/demo.git",
            "https://example.invalid./acme/demo.git",
            "https://example/acme/demo.git",
            "https://10.0.0.1/acme/demo.git",
            "https://192.168.0.1/acme/demo.git",
            "https://172.16.0.1/acme/demo.git",
            "https://169.254.169.254/acme/demo.git",
            "https://192.31.196.1/acme/demo.git",
            "https://192.52.193.1/acme/demo.git",
            "https://192.175.48.1/acme/demo.git",
            "https://[::1]/acme/demo.git",
            "https://[0000:0000:0000:0000:0000:0000:0000:0001]/acme/demo.git",
            "https://[0064:ff9b::1]/acme/demo.git",
            "https://[64:ff9b:1::1]/acme/demo.git",
            "https://[100:0:0:1::1]/acme/demo.git",
            "https://[2001:0db8::1]/acme/demo.git",
            "https://[3fff::1]/acme/demo.git",
            "https://[5f00::1]/acme/demo.git",
            "https://[fc00::1]/acme/demo.git",
            "https://[fe80::1]/acme/demo.git"
        );
        unsafeRemotes.forEach(remote ->
            assertThrows(IllegalArgumentException.class, () -> request("idem-remote", remote, "main", ""))
        );

        assertThrows(IllegalArgumentException.class, () -> request("idem-ref", "https://example.com/repo.git", "feature..main", ""));
        assertThrows(IllegalArgumentException.class, () -> request("idem-ref", "https://example.com/repo.git", "main\nnext", ""));
        assertThrows(IllegalArgumentException.class, () -> request("idem-commit", "https://example.com/repo.git", "", "not-a-commit"));
        assertThrows(IllegalArgumentException.class, () -> requestWithWorkspaceName("workspace-1"));
        assertThrows(IllegalArgumentException.class, () -> requestWithProvider("github-token"));
        assertThrows(IllegalArgumentException.class, () -> new BuildContext("gradle", "build-1", "/home/demo", List.of(":app"), Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new BuildContext("gradle", "build-1", "demo", List.of(" "), Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new BuildContext("gradle", "build-1", "demo", List.of(":app"), Map.of("token", "x")));
        assertThrows(IllegalArgumentException.class, () -> new BuildContext("gradle", "build-1", "demo", List.of(":app"), Map.of("tenant", "/mnt/data")));
        assertThrows(IllegalArgumentException.class, () -> new WorkspacePolicy(true, true, false, false, 60, 100_000));
        assertThrows(IllegalArgumentException.class, () -> new WorkspacePolicy(false, true, true, false, 60, 100_000));
        assertThrows(IllegalArgumentException.class, () -> new WorkspacePolicy(false, true, false, true, 60, 100_000));
        assertThrows(IllegalArgumentException.class, () -> new WorkspacePolicy(false, true, false, false, 0, 100_000));
        assertThrows(IllegalArgumentException.class, () -> new WorkspacePolicy(false, true, false, false, 3_601, 100_000));
        assertThrows(IllegalArgumentException.class, () -> new WorkspacePolicy(false, true, false, false, 60, 0));
        assertThrows(IllegalArgumentException.class, () -> new WorkspacePolicy(false, true, false, false, 60, 107_374_182_401L));

        var commit = request("idem-commit", "https://example.com/repo.git", "", "ABCDEF1");
        assertEquals("abcdef1", commit.commit());
    }

    @Test
    void normalizesOptionalValueObjectsWithoutLeakingWorkspaceData() {
        var context = new BuildContext("gradle", "build-1", null, null, null);
        var submission = new RepositoryToBtmSubmission(
            "analysis-run-1",
            "ACCEPTED",
            "/repository-analyses/analysis-run-1",
            "/repository-analyses/analysis-run-1/jobs",
            "BTM_DELIVERY_NOT_READY",
            null,
            "correlation-1",
            null
        );
        var diagnostic = new Diagnostic(null, null, "message");

        assertEquals(List.of(), context.declaredModules());
        assertEquals(Map.of(), context.attributes());
        assertEquals("", submission.btmDeliveryService());
        assertEquals(List.of(), submission.diagnostics());
        assertEquals("", diagnostic.severity());
        assertEquals("", diagnostic.code());
        assertEquals("UNKNOWN", new Diagnostic("debug", "SAFE_CODE", "message").severity());
        assertEquals("SAFE_CODE:1", new Diagnostic("warning", "safe_code:1", "message").code());
        assertEquals("line one  line two", new Diagnostic("INFO", "SAFE_CODE", "line one\r\nline two").message());
        assertThrows(IllegalArgumentException.class, () -> new Diagnostic("INFO", "SAFE_CODE", " "));
    }

    @Test
    void redactsPublicDiagnosticsFromOwnerResponses() {
        var unsafe = new RepositoryToBtmSubmission(
            "analysis-run-1",
            "ACCEPTED",
            "/repository-analyses/analysis-run-1",
            "/repository-analyses/analysis-run-1/jobs",
            "BTM_DELIVERY_NOT_READY",
            "BtmArtifactDeliveryService",
            "correlation-1",
            List.of(
                new Diagnostic("WARNING", "TOKEN_/TMP", "git clone https://example.com/private.git failed with token=abc in /tmp/workspace-1"),
                new Diagnostic("ERROR", "PATH_LEAK", "file:/mnt/d/repository-workspaces/workspace-1/stdout")
            )
        );

        assertEquals("DIAGNOSTIC_REDACTED", unsafe.diagnostics().get(0).code());
        assertEquals("Diagnostic details redacted", unsafe.diagnostics().get(0).message());
        assertEquals("Diagnostic details redacted", unsafe.diagnostics().get(1).message());
        assertFalse(unsafe.toString().contains("/tmp"));
        assertFalse(unsafe.toString().contains("workspace-"));
        assertFalse(unsafe.toString().contains("token="));
        assertFalse(unsafe.toString().contains("file:"));

        List.of(
            "C:\\Users\\demo\\repo",
            "raw stdout from worker",
            "raw stderr from worker",
            "/var/lib/forensic-analytics/workspaces/workspace-1",
            "repository-workspaces/workspace-1",
            "https://example.com/private.git",
            "https://example.com/private/repository"
        ).forEach(message ->
            assertEquals("Diagnostic details redacted", new Diagnostic("WARNING", "SAFE_CODE", message).message())
        );
    }

    private static SubmissionRequest request(String idempotencyKey, String repositoryUrl, String branch, String commit) {
        return new SubmissionRequest(
            "request-1",
            idempotencyKey,
            "gateway.v1",
            "correlation-1",
            List.of("BTM_RULES"),
            repositoryUrl,
            "github",
            branch,
            commit,
            "",
            buildContext(),
            policy()
        );
    }

    private static SubmissionRequest requestWithWorkspaceName(String workspaceName) {
        return new SubmissionRequest(
            "request-1",
            "idem-workspace",
            "gateway.v1",
            "correlation-1",
            List.of("BTM_RULES"),
            "https://example.com/acme/demo.git",
            "github",
            "main",
            "",
            workspaceName,
            buildContext(),
            policy()
        );
    }

    private static SubmissionRequest requestWithProvider(String provider) {
        return new SubmissionRequest(
            "request-1",
            "idem-provider",
            "gateway.v1",
            "correlation-1",
            List.of("BTM_RULES"),
            "https://example.com/acme/demo.git",
            provider,
            "main",
            "",
            "",
            buildContext(),
            policy()
        );
    }

    private static BuildContext buildContext() {
        return new BuildContext("gradle", "build-1", "demo", List.of(":app"), Map.of("tenant", "demo"));
    }

    private static WorkspacePolicy policy() {
        return new WorkspacePolicy(false, true, false, false, 60, 100_000);
    }

    private static final class CapturingPort implements RepositoryAnalysisOwnerPort {
        private int calls;
        private SubmissionRequest lastRequest;
        private StatusRequest lastStatusRequest;

        @Override
        public RepositoryToBtmSubmission start(SubmissionRequest request) {
            calls++;
            lastRequest = request;
            return new RepositoryToBtmSubmission(
                request.analysisRunId(),
                "ACCEPTED",
                "/repository-analyses/" + request.analysisRunId(),
                "/repository-analyses/" + request.analysisRunId() + "/jobs",
                "BTM_DELIVERY_NOT_READY",
                "BtmArtifactDeliveryService",
                request.correlationId(),
                List.of(Diagnostic.info("ORCHESTRATION_ACCEPTED", "Analysis Orchestrator accepted orchestration"))
            );
        }

        @Override
        public RepositoryToBtmStatus status(StatusRequest request) {
            lastStatusRequest = request;
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
}
