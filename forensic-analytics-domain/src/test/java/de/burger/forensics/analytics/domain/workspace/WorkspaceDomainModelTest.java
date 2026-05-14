package de.burger.forensics.analytics.domain.workspace;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkspaceDomainModelTest {
    private static final WorkspaceId WORKSPACE_A = new WorkspaceId("workspace-a");
    private static final WorkspaceId WORKSPACE_B = new WorkspaceId("workspace-b");
    private static final ProjectId PROJECT_A = new ProjectId("project-a");
    private static final UserId USER_A = new UserId("user-a");

    @Test
    void workspaceTracksLifecycleState() {
        var workspace = Workspace.active(WORKSPACE_A, "Workspace A");

        assertEquals(WORKSPACE_A, workspace.id());
        assertEquals("Workspace A", workspace.name());
        assertTrue(workspace.acceptsChanges());
        assertEquals("Renamed", workspace.rename("Renamed").name());
        assertFalse(workspace.archive().acceptsChanges());
    }

    @Test
    void projectKeepsWorkspaceBoundary() {
        var project = WorkspaceProject.active(PROJECT_A, WORKSPACE_A, "Case A");

        assertEquals(PROJECT_A, project.id());
        assertEquals(WORKSPACE_A, project.workspaceId());
        assertTrue(project.belongsTo(WORKSPACE_A));
        assertFalse(project.belongsTo(WORKSPACE_B));
        assertEquals("Case B", project.rename("Case B").name());
        assertFalse(project.archive().acceptsChanges());
    }

    @Test
    void membershipsKeepExplicitWorkspaceContext() {
        var project = WorkspaceProject.active(PROJECT_A, WORKSPACE_A, "Case A");
        var workspaceMembership = new WorkspaceMembership(WORKSPACE_A, USER_A, WorkspaceRole.OWNER);
        var projectMembership = new ProjectMembership(project, USER_A, WorkspaceRole.ANALYST);

        assertEquals(WORKSPACE_A, workspaceMembership.workspaceId());
        assertEquals(USER_A, workspaceMembership.userId());
        assertEquals(WorkspaceRole.OWNER, workspaceMembership.role());
        assertEquals(WORKSPACE_A, projectMembership.workspaceId());
        assertEquals(PROJECT_A, projectMembership.projectId());
        assertEquals(WorkspaceRole.ANALYST, projectMembership.role());
    }

    @Test
    void assetScopesSeparateSharedAndProjectAssets() {
        AssetScope sharedScope = new SharedWorkspaceAssetScope(WORKSPACE_A);
        AssetScope projectScope = new ProjectAssetScope(WORKSPACE_A, PROJECT_A);

        assertTrue(sharedScope.isShared());
        assertFalse(sharedScope.isProjectScoped());
        assertEquals(Optional.empty(), sharedScope.projectId());
        assertFalse(projectScope.isShared());
        assertTrue(projectScope.isProjectScoped());
        assertEquals(Optional.of(PROJECT_A), projectScope.projectId());
        assertEquals(WORKSPACE_A, projectScope.workspaceId());
    }

    @Test
    void workspaceAssetKeepsOwnershipAndChecksumMetadata() {
        var sharedAsset = new WorkspaceAsset(
            new AssetId("asset-a"),
            new SharedWorkspaceAssetScope(WORKSPACE_A),
            "evidence.bin",
            "sha256:abc",
            42L
        );
        var projectAsset = new WorkspaceAsset(
            new AssetId("asset-b"),
            new ProjectAssetScope(WORKSPACE_A, PROJECT_A),
            "report.json",
            "sha256:def",
            24L
        );

        assertEquals(WORKSPACE_A, sharedAsset.workspaceId());
        assertTrue(sharedAsset.isShared());
        assertFalse(sharedAsset.isProjectScoped());
        assertEquals(WORKSPACE_A, projectAsset.workspaceId());
        assertFalse(projectAsset.isShared());
        assertTrue(projectAsset.isProjectScoped());
        assertEquals(Optional.of(PROJECT_A), projectAsset.scope().projectId());
    }

    @Test
    void retentionPolicyRequiresPositiveDays() {
        assertEquals(30, new RetentionPolicy(30).retentionDays());
        assertThrows(IllegalArgumentException.class, () -> new RetentionPolicy(0));
        assertThrows(IllegalArgumentException.class, () -> new RetentionPolicy(-1));
    }

    @Test
    void rolesAreExplicit() {
        assertEquals(
            Set.of(
                WorkspaceRole.OWNER,
                WorkspaceRole.ADMIN,
                WorkspaceRole.ANALYST,
                WorkspaceRole.REVIEWER,
                WorkspaceRole.VIEWER,
                WorkspaceRole.AUDITOR
            ),
            EnumSet.allOf(WorkspaceRole.class)
        );
    }

    @Test
    void permissionsAreGrantedToExpectedRoles() {
        assertTrue(WorkspacePermission.READ_WORKSPACE.isGrantedTo(WorkspaceRole.VIEWER));
        assertTrue(WorkspacePermission.UPDATE_WORKSPACE.isGrantedTo(WorkspaceRole.OWNER));
        assertTrue(WorkspacePermission.UPDATE_WORKSPACE.isGrantedTo(WorkspaceRole.ADMIN));
        assertFalse(WorkspacePermission.UPDATE_WORKSPACE.isGrantedTo(WorkspaceRole.VIEWER));
        assertTrue(WorkspacePermission.MANAGE_WORKSPACE_MEMBERS.isGrantedTo(WorkspaceRole.ADMIN));
        assertFalse(WorkspacePermission.MANAGE_WORKSPACE_MEMBERS.isGrantedTo(WorkspaceRole.ANALYST));
        assertTrue(WorkspacePermission.CREATE_PROJECT.isGrantedTo(WorkspaceRole.OWNER));
        assertFalse(WorkspacePermission.CREATE_PROJECT.isGrantedTo(WorkspaceRole.REVIEWER));
        assertTrue(WorkspacePermission.READ_PROJECT.isGrantedTo(WorkspaceRole.ADMIN));
        assertFalse(WorkspacePermission.READ_PROJECT.isGrantedTo(WorkspaceRole.ANALYST));
        assertTrue(WorkspacePermission.MANAGE_PROJECT_MEMBERS.isGrantedTo(WorkspaceRole.OWNER));
        assertFalse(WorkspacePermission.MANAGE_PROJECT_MEMBERS.isGrantedTo(WorkspaceRole.AUDITOR));
        assertTrue(WorkspacePermission.READ_WORKSPACE_AUDIT.isGrantedTo(WorkspaceRole.AUDITOR));
        assertFalse(WorkspacePermission.READ_WORKSPACE_AUDIT.isGrantedTo(WorkspaceRole.VIEWER));
        assertTrue(WorkspacePermission.MANAGE_SHARED_ASSETS.isGrantedTo(WorkspaceRole.ADMIN));
        assertFalse(WorkspacePermission.MANAGE_SHARED_ASSETS.isGrantedTo(WorkspaceRole.VIEWER));
    }

    @Test
    void storageAreasHaveStableDirectoryNames() {
        assertEquals("evidence_original", ProjectStorageArea.EVIDENCE_ORIGINAL.directoryName());
        assertEquals("evidence_processed", ProjectStorageArea.EVIDENCE_PROCESSED.directoryName());
        assertEquals("analysis_results", ProjectStorageArea.ANALYSIS_RESULTS.directoryName());
        assertEquals("reports", ProjectStorageArea.REPORTS.directoryName());
        assertEquals("logs", ProjectStorageArea.LOGS.directoryName());
    }

    @Test
    void rejectsInvalidWorkspaceValues() {
        assertThrows(IllegalArgumentException.class, () -> new WorkspaceId(null));
        assertThrows(IllegalArgumentException.class, () -> new WorkspaceId(" "));
        assertThrows(IllegalArgumentException.class, () -> new ProjectId(null));
        assertThrows(IllegalArgumentException.class, () -> new ProjectId(" "));
        assertThrows(IllegalArgumentException.class, () -> new UserId(null));
        assertThrows(IllegalArgumentException.class, () -> new UserId(" "));
        assertThrows(IllegalArgumentException.class, () -> new AssetId(null));
        assertThrows(IllegalArgumentException.class, () -> new AssetId(" "));
        assertThrows(NullPointerException.class, () -> new Workspace(null, "Workspace", WorkspaceStatus.ACTIVE));
        assertThrows(IllegalArgumentException.class, () -> new Workspace(WORKSPACE_A, " ", WorkspaceStatus.ACTIVE));
        assertThrows(NullPointerException.class, () -> new Workspace(WORKSPACE_A, "Workspace", null));
        assertThrows(NullPointerException.class, () -> new WorkspaceProject(null, WORKSPACE_A, "Case", ProjectStatus.ACTIVE));
        assertThrows(NullPointerException.class, () -> new WorkspaceProject(PROJECT_A, null, "Case", ProjectStatus.ACTIVE));
        assertThrows(IllegalArgumentException.class, () -> new WorkspaceProject(PROJECT_A, WORKSPACE_A, " ", ProjectStatus.ACTIVE));
        assertThrows(NullPointerException.class, () -> new WorkspaceProject(PROJECT_A, WORKSPACE_A, "Case", null));
        assertThrows(NullPointerException.class, () -> WorkspaceProject.active(PROJECT_A, WORKSPACE_A, "Case").belongsTo(null));
    }

    @Test
    void rejectsInvalidMembershipAndAssetValues() {
        var project = WorkspaceProject.active(PROJECT_A, WORKSPACE_A, "Case A");

        assertThrows(NullPointerException.class, () -> new WorkspaceMembership(null, USER_A, WorkspaceRole.OWNER));
        assertThrows(NullPointerException.class, () -> new WorkspaceMembership(WORKSPACE_A, null, WorkspaceRole.OWNER));
        assertThrows(NullPointerException.class, () -> new WorkspaceMembership(WORKSPACE_A, USER_A, null));
        assertThrows(NullPointerException.class, () -> new ProjectMembership(null, USER_A, WorkspaceRole.ANALYST));
        assertThrows(NullPointerException.class, () -> new ProjectMembership(project, null, WorkspaceRole.ANALYST));
        assertThrows(NullPointerException.class, () -> new ProjectMembership(project, USER_A, null));
        assertThrows(NullPointerException.class, () -> new SharedWorkspaceAssetScope(null));
        assertThrows(NullPointerException.class, () -> new ProjectAssetScope(null, PROJECT_A));
        assertThrows(NullPointerException.class, () -> new ProjectAssetScope(WORKSPACE_A, null));
        assertThrows(NullPointerException.class, () -> new WorkspaceAsset(null, new SharedWorkspaceAssetScope(WORKSPACE_A), "file", "sha", 1L));
        assertThrows(NullPointerException.class, () -> new WorkspaceAsset(new AssetId("asset"), null, "file", "sha", 1L));
        assertThrows(IllegalArgumentException.class, () -> new WorkspaceAsset(new AssetId("asset"), new SharedWorkspaceAssetScope(WORKSPACE_A), " ", "sha", 1L));
        assertThrows(IllegalArgumentException.class, () -> new WorkspaceAsset(new AssetId("asset"), new SharedWorkspaceAssetScope(WORKSPACE_A), "file", " ", 1L));
        assertThrows(IllegalArgumentException.class, () -> new WorkspaceAsset(new AssetId("asset"), new SharedWorkspaceAssetScope(WORKSPACE_A), "file", "sha", -1L));
    }
}
