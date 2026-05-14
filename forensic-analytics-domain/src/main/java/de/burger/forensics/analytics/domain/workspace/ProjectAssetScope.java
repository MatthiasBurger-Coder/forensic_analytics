package de.burger.forensics.analytics.domain.workspace;

import java.util.Objects;
import java.util.Optional;

public record ProjectAssetScope(
    WorkspaceId workspaceId,
    ProjectId scopedProjectId
) implements AssetScope {
    public ProjectAssetScope {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(scopedProjectId, "scopedProjectId must not be null");
    }

    @Override
    public Optional<ProjectId> projectId() {
        return Optional.of(scopedProjectId);
    }
}
