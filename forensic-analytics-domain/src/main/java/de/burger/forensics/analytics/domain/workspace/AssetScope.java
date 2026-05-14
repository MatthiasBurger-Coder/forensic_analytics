package de.burger.forensics.analytics.domain.workspace;

import java.util.Optional;

public sealed interface AssetScope permits SharedWorkspaceAssetScope, ProjectAssetScope {
    WorkspaceId workspaceId();

    Optional<ProjectId> projectId();

    default boolean isShared() {
        return projectId().isEmpty();
    }

    default boolean isProjectScoped() {
        return projectId().isPresent();
    }
}
