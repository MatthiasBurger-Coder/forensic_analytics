package de.burger.forensics.analytics.domain.workspace;

import java.util.List;
import java.util.Objects;

public record PreparedWorkspace(
    WorkspaceId workspaceId,
    WorkspacePreparationStatus status,
    WorkspacePath path,
    WorkspaceLease lease,
    List<String> diagnostics
) {
    public PreparedWorkspace {
        workspaceId = Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        status = Objects.requireNonNull(status, "status must not be null");
        path = Objects.requireNonNull(path, "path must not be null");
        lease = Objects.requireNonNull(lease, "lease must not be null");
        requireMatchingLeaseStatus(status, lease);
        diagnostics = copyDiagnostics(diagnostics);
    }

    private static List<String> copyDiagnostics(List<String> diagnostics) {
        return List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics must not be null")).stream()
            .peek(diagnostic -> RequiredWorkspaceText.requireText(diagnostic, "diagnostic"))
            .toList();
    }

    private static void requireMatchingLeaseStatus(WorkspacePreparationStatus status, WorkspaceLease lease) {
        if (!status.equals(lease.status())) {
            throw new IllegalArgumentException("workspace status must match lease status");
        }
    }
}
