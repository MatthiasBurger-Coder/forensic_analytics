package de.burger.forensics.analytics.persistence;

import de.burger.forensics.analytics.domain.workspace.AssetId;
import de.burger.forensics.analytics.domain.workspace.ProjectAssetScope;
import de.burger.forensics.analytics.domain.workspace.ProjectId;
import de.burger.forensics.analytics.domain.workspace.SharedWorkspaceAssetScope;
import de.burger.forensics.analytics.domain.workspace.WorkspaceAsset;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryAssetRepositoryTest {
    private static final WorkspaceId WORKSPACE_A = new WorkspaceId("workspace-a");
    private static final WorkspaceId WORKSPACE_B = new WorkspaceId("workspace-b");
    private static final ProjectId PROJECT_A = new ProjectId("project-a");
    private static final ProjectId PROJECT_B = new ProjectId("project-b");

    @Test
    void storesSharedAndProjectAssetsSeparately() {
        var repository = new InMemoryAssetRepository();
        var shared = sharedAsset("asset-a", WORKSPACE_A);
        var project = projectAsset("asset-b", WORKSPACE_A, PROJECT_A);
        var otherProject = projectAsset("asset-c", WORKSPACE_A, PROJECT_B);
        var otherWorkspace = sharedAsset("asset-d", WORKSPACE_B);

        repository.save(project);
        repository.save(otherWorkspace);
        repository.save(otherProject);
        repository.save(shared);

        assertEquals(shared, repository.findById(new AssetId("asset-a")).orElseThrow());
        assertEquals(List.of(shared), repository.findSharedByWorkspace(WORKSPACE_A));
        assertEquals(List.of(project), repository.findByProject(WORKSPACE_A, PROJECT_A));
    }

    @Test
    void returnsEmptyForUnknownAssets() {
        var repository = new InMemoryAssetRepository();

        assertTrue(repository.findById(new AssetId("missing")).isEmpty());
        assertEquals(List.of(), repository.findSharedByWorkspace(WORKSPACE_A));
        assertEquals(List.of(), repository.findByProject(WORKSPACE_A, PROJECT_A));
    }

    @Test
    void rejectsMissingValues() {
        var repository = new InMemoryAssetRepository();

        assertThrows(NullPointerException.class, () -> repository.save(null));
        assertThrows(NullPointerException.class, () -> repository.findById(null));
        assertThrows(NullPointerException.class, () -> repository.findSharedByWorkspace(null));
        assertThrows(NullPointerException.class, () -> repository.findByProject(null, PROJECT_A));
        assertThrows(NullPointerException.class, () -> repository.findByProject(WORKSPACE_A, null));
    }

    private static WorkspaceAsset sharedAsset(String assetId, WorkspaceId workspaceId) {
        return new WorkspaceAsset(
            new AssetId(assetId),
            new SharedWorkspaceAssetScope(workspaceId),
            assetId + ".bin",
            "sha256:" + assetId,
            1L
        );
    }

    private static WorkspaceAsset projectAsset(String assetId, WorkspaceId workspaceId, ProjectId projectId) {
        return new WorkspaceAsset(
            new AssetId(assetId),
            new ProjectAssetScope(workspaceId, projectId),
            assetId + ".bin",
            "sha256:" + assetId,
            1L
        );
    }
}
