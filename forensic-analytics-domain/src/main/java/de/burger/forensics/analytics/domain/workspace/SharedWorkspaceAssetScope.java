package de.burger.forensics.analytics.domain.workspace;

import java.util.Objects;
import java.util.Optional;

public record SharedWorkspaceAssetScope(WorkspaceId workspaceId) implements AssetScope {
    public SharedWorkspaceAssetScope {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
    }

    @Override
    public Optional<ProjectId> projectId() {
        return Optional.empty();
    }
}
