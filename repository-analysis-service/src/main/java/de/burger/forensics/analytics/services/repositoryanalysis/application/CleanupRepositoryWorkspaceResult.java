package de.burger.forensics.analytics.services.repositoryanalysis.application;

import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.Diagnostic;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.RepositoryWorkspaceStatus;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.WorkspaceId;

import java.util.List;
import java.util.Objects;

public record CleanupRepositoryWorkspaceResult(
    WorkspaceId workspaceId,
    RepositoryWorkspaceStatus workspaceStatus,
    List<Diagnostic> diagnostics
) {
    public CleanupRepositoryWorkspaceResult {
        Objects.requireNonNull(workspaceId, "workspace id must not be null");
        workspaceStatus = Objects.requireNonNullElse(workspaceStatus, RepositoryWorkspaceStatus.CLEANED);
        diagnostics = List.copyOf(Objects.requireNonNullElse(diagnostics, List.of()));
    }
}
