package de.burger.forensics.analytics.persistence;

import de.burger.forensics.analytics.application.asset.port.AssetRepository;
import de.burger.forensics.analytics.domain.workspace.AssetId;
import de.burger.forensics.analytics.domain.workspace.ProjectId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceAsset;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryAssetRepository implements AssetRepository {
    private final Map<AssetId, WorkspaceAsset> assets = new ConcurrentHashMap<>();

    @Override
    public void save(WorkspaceAsset asset) {
        Objects.requireNonNull(asset, "asset must not be null");
        assets.put(asset.id(), asset);
    }

    @Override
    public Optional<WorkspaceAsset> findById(AssetId assetId) {
        Objects.requireNonNull(assetId, "assetId must not be null");
        return Optional.ofNullable(assets.get(assetId));
    }

    @Override
    public List<WorkspaceAsset> findSharedByWorkspace(WorkspaceId workspaceId) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        return assets.values().stream()
            .filter(asset -> asset.workspaceId().equals(workspaceId))
            .filter(WorkspaceAsset::isShared)
            .sorted(Comparator.comparing(asset -> asset.id().value()))
            .toList();
    }

    @Override
    public List<WorkspaceAsset> findByProject(WorkspaceId workspaceId, ProjectId projectId) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(projectId, "projectId must not be null");
        return assets.values().stream()
            .filter(asset -> asset.workspaceId().equals(workspaceId))
            .filter(asset -> asset.scope().projectId().filter(projectId::equals).isPresent())
            .sorted(Comparator.comparing(asset -> asset.id().value()))
            .toList();
    }
}
