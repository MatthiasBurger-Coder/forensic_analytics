package de.burger.forensics.analytics.services.analysisstore.domain;

import de.burger.forensics.analytics.services.analysisstore.domain.RepositoryToBtmOrchestrationDomain.BtmDeliveryReadiness;
import de.burger.forensics.analytics.services.analysisstore.domain.RepositoryToBtmOrchestrationDomain.BuildContext;
import de.burger.forensics.analytics.services.analysisstore.domain.RepositoryToBtmOrchestrationDomain.DiagnosticSeverity;
import de.burger.forensics.analytics.services.analysisstore.domain.RepositoryToBtmOrchestrationDomain.OperationStatus;
import de.burger.forensics.analytics.services.analysisstore.domain.RepositoryToBtmOrchestrationDomain.OrchestrationMetadata;
import de.burger.forensics.analytics.services.analysisstore.domain.RepositoryToBtmOrchestrationDomain.OrchestrationState;
import de.burger.forensics.analytics.services.analysisstore.domain.RepositoryToBtmOrchestrationDomain.RepositoryReference;
import de.burger.forensics.analytics.services.analysisstore.domain.RepositoryToBtmOrchestrationDomain.RepositoryToBtmDiagnostic;
import de.burger.forensics.analytics.services.analysisstore.domain.RepositoryToBtmOrchestrationDomain.RepositoryToBtmOrchestrationStatus;
import de.burger.forensics.analytics.services.analysisstore.domain.RepositoryToBtmOrchestrationDomain.RequestedOutput;
import de.burger.forensics.analytics.services.analysisstore.domain.RepositoryToBtmOrchestrationDomain.RevisionSelector;
import de.burger.forensics.analytics.services.analysisstore.domain.RepositoryToBtmOrchestrationDomain.StartRepositoryToBtmCommand;
import de.burger.forensics.analytics.services.analysisstore.domain.RepositoryToBtmOrchestrationDomain.WorkspacePolicy;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositoryToBtmOrchestrationDomainTest {
    @Test
    void validatesRepositoryToBtmCommandAndDeterministicIds() {
        var command = command();

        assertTrue(command.fingerprint().contains("https://example.com/acme/demo.git"));
        assertEquals(
            RepositoryToBtmOrchestrationDomain.repositoryAnalysisJobId(runId()),
            RepositoryToBtmOrchestrationDomain.repositoryAnalysisJobId(runId())
        );
        assertEquals(
            RepositoryToBtmOrchestrationDomain.pendingSourceSnapshotId(runId()),
            RepositoryToBtmOrchestrationDomain.pendingSourceSnapshotId(runId())
        );
        assertThrows(NullPointerException.class, () -> new StartRepositoryToBtmCommand(
            null,
            command.repository(),
            command.revision(),
            command.workspacePolicy(),
            command.buildContext(),
            command.requestedOutputs(),
            command.attributes()
        ));
        assertThrows(IllegalArgumentException.class, () -> new StartRepositoryToBtmCommand(
            command.metadata(),
            command.repository(),
            command.revision(),
            command.workspacePolicy(),
            command.buildContext(),
            List.of(),
            command.attributes()
        ));
        assertThrows(NullPointerException.class, () -> new StartRepositoryToBtmCommand(
            command.metadata(),
            command.repository(),
            command.revision(),
            command.workspacePolicy(),
            command.buildContext(),
            Collections.singletonList(null),
            command.attributes()
        ));
    }

    @Test
    void rejectsPrivateRepositoryCoordinatesAndUnsafeRevisionSelectors() {
        var privateRemotes = List.of(
            "http://example.com/acme/demo.git",
            "https://user@example.com/acme/demo.git",
            "https://example.com/acme/demo.git?token=x",
            "https://example.com/acme/demo.git#main",
            "https://localhost/acme/demo.git",
            "https://demo.localhost/acme/demo.git",
            "https://0.1.2.3/acme/demo.git",
            "https://127.0.0.1/acme/demo.git",
            "https://10.0.0.1/acme/demo.git",
            "https://192.168.0.1/acme/demo.git",
            "https://172.16.0.1/acme/demo.git",
            "https://169.254.169.254/acme/demo.git",
            "https://[::]/acme/demo.git",
            "https://[::1]/acme/demo.git",
            "https://[0:0:0:0:0:0:0:0]/acme/demo.git",
            "https://[0:0:0:0:0:0:0:1]/acme/demo.git",
            "https://[::ffff:192.0.2.1]/acme/demo.git",
            "https://[0:0:0:0:0:ffff:192.0.2.1]/acme/demo.git",
            "https://[fc00::1]/acme/demo.git",
            "https://[fd00::1]/acme/demo.git",
            "https://[fe80::1%25eth0]/acme/demo.git"
        );

        privateRemotes.forEach(remote -> assertThrows(IllegalArgumentException.class, () -> new RepositoryReference(remote, "github")));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryReference("https://example.com/acme/demo.git", "github-token"));
        assertEquals("abcdef1", new RevisionSelector("", "ABCDEF1").commit());
        assertThrows(IllegalArgumentException.class, () -> new RevisionSelector("", ""));
        assertThrows(IllegalArgumentException.class, () -> new RevisionSelector("-main", ""));
        assertThrows(IllegalArgumentException.class, () -> new RevisionSelector("feature..main", ""));
        assertThrows(IllegalArgumentException.class, () -> new RevisionSelector("", "not-a-commit"));
    }

    @Test
    void validatesWorkspaceBuildContextAttributesAndPublicDiagnostics() {
        var attributes = new BuildContext("gradle", "build-1", "", List.of(":app"), Map.of("empty", " "));
        var diagnostic = new RepositoryToBtmDiagnostic(
            "token:/tmp",
            "git clone https://example.com/private/repository failed in /tmp/workspace-1 with token=abc",
            DiagnosticSeverity.WARNING,
            false,
            true
        );
        var accepted = OperationStatus.accepted("correlation-1", List.of(diagnostic));

        assertEquals("", attributes.attributes().get("empty"));
        assertEquals("DIAGNOSTIC_REDACTED", diagnostic.code());
        assertEquals("Diagnostic details redacted", diagnostic.message());
        assertEquals(List.of("DIAGNOSTIC_REDACTED"), accepted.diagnostics());
        assertFalse(accepted.retryable());
        assertThrows(IllegalArgumentException.class, () -> new BuildContext("gradle", "build-1", "/tmp/demo", List.of(":app"), Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new BuildContext("gradle", "build-1", "demo", List.of(" "), Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new BuildContext("gradle", "build-1", "demo", List.of(":app"), Map.of("token", "x")));
        assertThrows(IllegalArgumentException.class, () -> new BuildContext("gradle", "build-1", "demo", List.of(":app"), Map.of("tenant", "/mnt/repo")));
        assertThrows(IllegalArgumentException.class, () -> new WorkspacePolicy(true, true, false, false, 60, 100_000));
        assertThrows(IllegalArgumentException.class, () -> new WorkspacePolicy(false, true, true, false, 60, 100_000));
        assertThrows(IllegalArgumentException.class, () -> new WorkspacePolicy(false, true, false, true, 60, 100_000));
        assertThrows(IllegalArgumentException.class, () -> new WorkspacePolicy(false, true, false, false, 0, 100_000));
        assertThrows(IllegalArgumentException.class, () -> new WorkspacePolicy(false, true, false, false, 3_601, 100_000));
        assertThrows(IllegalArgumentException.class, () -> new WorkspacePolicy(false, true, false, false, 60, 0));
        assertThrows(IllegalArgumentException.class, () -> new WorkspacePolicy(false, true, false, false, 60, 107_374_182_401L));
        assertThrows(NullPointerException.class, () -> new RepositoryToBtmDiagnostic("OK", "message", null, false, false));
    }

    @Test
    void normalizesRepositoryToBtmStatusCollectionsAndAttributes() {
        var status = new RepositoryToBtmOrchestrationStatus(
            OperationStatus.accepted("correlation-1", List.of()),
            runId(),
            RepositoryToBtmOrchestrationDomain.repositoryAnalysisJobId(runId()),
            RepositoryToBtmOrchestrationDomain.pendingSourceSnapshotId(runId()),
            AnalysisCompleteness.INCOMPLETE,
            OrchestrationState.WAITING_FOR_REPOSITORY,
            BtmDeliveryReadiness.NOT_READY,
            true,
            null,
            null,
            Map.of("tenant", "demo")
        );

        assertEquals(List.of(), status.diagnostics());
        assertEquals(List.of(), status.acceptedGeneratedArtifacts());
        assertEquals("demo", status.attributes().get("tenant"));
    }

    private static StartRepositoryToBtmCommand command() {
        return new StartRepositoryToBtmCommand(
            new OrchestrationMetadata("request-1", "schema-v1", "correlation-1", runId()),
            new RepositoryReference("https://example.com/acme/demo.git", "github"),
            new RevisionSelector("main", ""),
            new WorkspacePolicy(false, true, false, false, 60, 100_000),
            new BuildContext("gradle", "build-1", "demo", List.of(":app"), Map.of("tenant", "demo")),
            List.of(RequestedOutput.BTM_RULES),
            Map.of("tenant", "demo")
        );
    }

    private static AnalysisRunId runId() {
        return new AnalysisRunId("run-1");
    }
}
