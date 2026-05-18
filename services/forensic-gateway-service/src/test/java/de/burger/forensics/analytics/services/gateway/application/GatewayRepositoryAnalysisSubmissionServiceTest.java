package de.burger.forensics.analytics.services.gateway.application;

import de.burger.forensics.analytics.services.gateway.application.port.RepositoryAnalysisPreparationPort;
import de.burger.forensics.analytics.services.gateway.domain.GatewayRepositoryAnalysis.BuildContext;
import de.burger.forensics.analytics.services.gateway.domain.GatewayRepositoryAnalysis.Diagnostic;
import de.burger.forensics.analytics.services.gateway.domain.GatewayRepositoryAnalysis.RepositoryPreparationCommand;
import de.burger.forensics.analytics.services.gateway.domain.GatewayRepositoryAnalysis.RepositoryPreparationResult;
import de.burger.forensics.analytics.services.gateway.domain.GatewayRepositoryAnalysis.RepositoryToBtmSubmission;
import de.burger.forensics.analytics.services.gateway.domain.GatewayRepositoryAnalysis.SubmissionRequest;
import de.burger.forensics.analytics.services.gateway.domain.GatewayRepositoryAnalysis.WorkspacePolicy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayRepositoryAnalysisSubmissionServiceTest {
    @Test
    void submitsRepositoryPreparationAndReplaysIdempotentRequests() {
        var port = new CapturingPort();
        var service = new GatewayRepositoryAnalysisSubmissionService(port);

        var submitted = service.submit(request("idem-1", "https://example.com/acme/demo.git", "main", ""));
        var replayed = service.submit(request("idem-1", "https://example.com/acme/demo.git", "main", ""));

        assertSame(submitted, replayed);
        assertEquals(1, port.calls);
        assertTrue(submitted.analysisRunId().startsWith("analysis-run-"));
        assertEquals("ACCEPTED", submitted.status());
        assertEquals("/repository-analyses/" + submitted.analysisRunId(), submitted.statusUrl());
        assertEquals("BTM_DELIVERY_NOT_READY", submitted.btmDeliveryStatus());
        assertEquals("correlation-1", submitted.correlationId());
        assertEquals(submitted.analysisRunId(), port.lastCommand.analysisRunId());
        assertFalse(submitted.toString().contains("workspace-"));
    }

    @Test
    void rejectsConflictingIdempotencyAndUnsafeRequestsBeforeCallingPort() {
        var port = new CapturingPort();
        var service = new GatewayRepositoryAnalysisSubmissionService(port);
        service.submit(request("idem-1", "https://example.com/acme/demo.git", "main", ""));

        assertThrows(GatewayIdempotencyConflictException.class, () -> service.submit(
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
        var service = new GatewayRepositoryAnalysisSubmissionService(port);

        service.submit(request("commit-only", "https://example.com/acme/demo.git", "", "abcdef1"));

        assertEquals("", port.lastCommand.request().branch());
        assertEquals("abcdef1", port.lastCommand.request().commit());
    }

    @Test
    void validatesRepositoryBoundariesAndWorkspacePolicyBeforeCallingPort() {
        var unsafeRemotes = List.of(
            "https://localhost/acme/demo.git",
            "https://demo.localhost/acme/demo.git",
            "https://10.0.0.1/acme/demo.git",
            "https://192.168.0.1/acme/demo.git",
            "https://172.16.0.1/acme/demo.git",
            "https://169.254.169.254/acme/demo.git",
            "https://[::1]/acme/demo.git",
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
        var result = new RepositoryPreparationResult("analysis-run-1", "source-snapshot-1", null, null);
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
        assertEquals("", result.checkoutStatus());
        assertEquals(List.of(), result.diagnostics());
        assertEquals("", submission.btmDeliveryService());
        assertEquals(List.of(), submission.diagnostics());
        assertEquals("", diagnostic.severity());
        assertEquals("", diagnostic.code());
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

    private static final class CapturingPort implements RepositoryAnalysisPreparationPort {
        private int calls;
        private RepositoryPreparationCommand lastCommand;

        @Override
        public RepositoryPreparationResult prepare(RepositoryPreparationCommand command) {
            calls++;
            lastCommand = command;
            return new RepositoryPreparationResult(
                command.analysisRunId(),
                "source-snapshot-1",
                "CHECKED_OUT",
                List.of(Diagnostic.info("DOWNSTREAM_OK", "Repository Analysis accepted the snapshot"))
            );
        }
    }
}
