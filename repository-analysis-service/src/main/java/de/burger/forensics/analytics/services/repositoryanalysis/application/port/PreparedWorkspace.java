package de.burger.forensics.analytics.services.repositoryanalysis.application.port;

import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.WorkspaceId;

import java.nio.file.Path;
import java.util.Objects;

public record PreparedWorkspace(WorkspaceId workspaceId, Path workspacePath) {
    public PreparedWorkspace {
        Objects.requireNonNull(workspaceId, "workspace id must not be null");
        Objects.requireNonNull(workspacePath, "workspace path must not be null");
    }
}
