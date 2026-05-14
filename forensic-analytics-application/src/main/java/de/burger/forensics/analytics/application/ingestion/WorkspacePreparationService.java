package de.burger.forensics.analytics.application.ingestion;

import de.burger.forensics.analytics.application.ingestion.command.WorkspacePreparationRequest;
import de.burger.forensics.analytics.application.ingestion.port.WorkspacePreparationPort;
import de.burger.forensics.analytics.domain.workspace.PreparedWorkspace;
import de.burger.forensics.analytics.domain.workspace.WorkspacePreparationStatus;

import java.util.Objects;

public final class WorkspacePreparationService {
    private final WorkspacePreparationPort workspacePreparationPort;

    public WorkspacePreparationService(WorkspacePreparationPort workspacePreparationPort) {
        this.workspacePreparationPort = Objects.requireNonNull(
            workspacePreparationPort,
            "workspacePreparationPort must not be null"
        );
    }

    public PreparedWorkspace prepare(WorkspacePreparationRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        var workspace = Objects.requireNonNull(
            workspacePreparationPort.prepare(request),
            "prepared workspace must not be null"
        );
        if (!WorkspacePreparationStatus.READY.equals(workspace.status())) {
            throw new RepositoryAnalysisIngestionException(
                "Workspace preparation did not produce a ready workspace: " + workspace.status()
            );
        }
        return workspace;
    }

    public PreparedWorkspace cleanup(PreparedWorkspace workspace) {
        Objects.requireNonNull(workspace, "workspace must not be null");
        return Objects.requireNonNull(
            workspacePreparationPort.cleanup(workspace),
            "cleaned workspace must not be null"
        );
    }
}
