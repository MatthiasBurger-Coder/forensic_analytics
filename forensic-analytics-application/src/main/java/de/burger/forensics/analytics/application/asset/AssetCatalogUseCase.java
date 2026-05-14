package de.burger.forensics.analytics.application.asset;

import de.burger.forensics.analytics.application.asset.command.ListProjectAssetsCommand;
import de.burger.forensics.analytics.application.asset.command.ListSharedAssetsCommand;
import de.burger.forensics.analytics.application.asset.command.RegisterProjectAssetCommand;
import de.burger.forensics.analytics.application.asset.command.RegisterSharedAssetCommand;
import de.burger.forensics.analytics.domain.workspace.WorkspaceAsset;

import java.util.List;

public interface AssetCatalogUseCase {
    WorkspaceAsset registerShared(RegisterSharedAssetCommand command);

    WorkspaceAsset registerProject(RegisterProjectAssetCommand command);

    List<WorkspaceAsset> listShared(ListSharedAssetsCommand command);

    List<WorkspaceAsset> listProject(ListProjectAssetsCommand command);
}
