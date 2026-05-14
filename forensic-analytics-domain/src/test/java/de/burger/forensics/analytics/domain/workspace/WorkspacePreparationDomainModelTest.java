package de.burger.forensics.analytics.domain.workspace;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkspacePreparationDomainModelTest {
    @Test
    void preparedWorkspaceKeepsLifecyclePathAndLease() {
        var startedAt = Instant.parse("2026-05-14T12:00:00Z");
        var lease = new WorkspaceLease(
            "analysis-1",
            startedAt,
            Optional.of(startedAt.plusSeconds(60)),
            WorkspacePreparationStatus.READY
        );
        var workspace = new PreparedWorkspace(
            new WorkspaceId("workspace-1"),
            WorkspacePreparationStatus.READY,
            new WorkspacePath("/tmp/workspace-1"),
            lease,
            List.of("Workspace created")
        );

        assertEquals(WorkspacePreparationStatus.READY, workspace.status());
        assertEquals("/tmp/workspace-1", workspace.path().value());
        assertEquals(Optional.of(startedAt.plusSeconds(60)), workspace.lease().expiresAt());
    }

    @Test
    void workspacePolicyKeepsRequestedCloneAndCleanupPolicy() {
        var policy = new WorkspacePolicy(
            true,
            true,
            false,
            false,
            Duration.ofSeconds(30),
            1024L,
            WorkspaceCleanupPolicy.DELETE_ON_COMPLETION
        );

        assertEquals(Duration.ofSeconds(30), policy.timeout());
        assertEquals(WorkspaceCleanupPolicy.DELETE_ON_COMPLETION, policy.cleanupPolicy());
    }

    @Test
    void rejectsInvalidWorkspacePreparationValues() {
        assertThrows(IllegalArgumentException.class, () -> new WorkspacePath(" "));
        assertThrows(IllegalArgumentException.class, () -> new WorkspacePolicy(false, false, false, false, Duration.ofSeconds(-1), 0, WorkspaceCleanupPolicy.RETAIN_FOR_REVIEW));
        assertThrows(IllegalArgumentException.class, () -> new WorkspacePolicy(false, false, false, false, Duration.ZERO, -1, WorkspaceCleanupPolicy.RETAIN_FOR_REVIEW));
        assertThrows(NullPointerException.class, () -> new WorkspaceLease("owner", Instant.now(), null, WorkspacePreparationStatus.READY));
        assertThrows(IllegalArgumentException.class, () -> new WorkspaceLease(" ", Instant.now(), Optional.empty(), WorkspacePreparationStatus.READY));
        assertThrows(
            IllegalArgumentException.class,
            () -> new WorkspaceLease(
                "owner",
                Instant.parse("2026-05-14T12:00:00Z"),
                Optional.of(Instant.parse("2026-05-14T12:00:00Z")),
                WorkspacePreparationStatus.READY
            )
        );
        assertThrows(NullPointerException.class, () -> new PreparedWorkspace(null, WorkspacePreparationStatus.READY, new WorkspacePath("/tmp/a"), new WorkspaceLease("owner", Instant.now(), Optional.empty(), WorkspacePreparationStatus.READY), List.of()));
        assertThrows(
            IllegalArgumentException.class,
            () -> new PreparedWorkspace(
                new WorkspaceId("workspace-1"),
                WorkspacePreparationStatus.READY,
                new WorkspacePath("/tmp/a"),
                new WorkspaceLease("owner", Instant.now(), Optional.empty(), WorkspacePreparationStatus.FAILED),
                List.of()
            )
        );
    }
}
