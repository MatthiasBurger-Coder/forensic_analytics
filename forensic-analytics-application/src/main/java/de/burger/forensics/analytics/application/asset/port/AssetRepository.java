package de.burger.forensics.analytics.application.asset.port;

import de.burger.forensics.analytics.domain.workspace.AssetId;
import de.burger.forensics.analytics.domain.workspace.ProjectId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceAsset;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;

import java.util.List;
import java.util.Optional;

public interface AssetRepository {
    void save(WorkspaceAsset asset);

    Optional<WorkspaceAsset> findById(AssetId assetId);

    List<WorkspaceAsset> findSharedByWorkspace(WorkspaceId workspaceId);

    List<WorkspaceAsset> findByProject(WorkspaceId workspaceId, ProjectId projectId);
}
