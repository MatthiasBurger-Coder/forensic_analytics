package de.burger.forensics.analytics.services.repositorysource.adapter.out.filesystem;

import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceBranchId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspacePolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class FileSystemRepositoryWorkspaceAdapterTest {
    @TempDir
    private Path root;

    @Test
    void preparesOpaqueWorkspaceUnderConfiguredRootAndCleansIt() throws Exception {
        var adapter = new FileSystemRepositoryWorkspaceAdapter(root);
        var workspace = adapter.prepare(
            new AnalysisRunId("run:1"),
            new WorkspacePolicy(true, true, false, false, 60, 100_000)
        );
        var marker = workspace.workspacePath().resolve("marker.txt");
        var gitHome = workspace.workspacePath().resolve(".repository-source-git-home");
        Files.writeString(marker, "demo");
        Files.createDirectories(gitHome.resolve(".config"));

        assertTrue(workspace.workspaceId().value().startsWith("workspace-"));
        assertFalse(workspace.workspaceId().value().contains("run"));
        assertTrue(workspace.workspacePath().startsWith(root.toRealPath()));
        assertNotEquals(root.toRealPath(), workspace.workspacePath());
        assertTrue(Files.exists(marker));
        assertTrue(Files.exists(gitHome));

        adapter.cleanup(workspace.workspaceId());
        adapter.cleanup(workspace.workspaceId());
        adapter.cleanup(new WorkspaceId("workspace-unknown"));

        assertFalse(Files.exists(workspace.workspacePath()));
        assertFalse(Files.exists(gitHome));
        assertTrue(Files.exists(root));
    }

    @Test
    void reportsPreparationAndCleanupFailuresWithoutReturningPrivatePaths() throws Exception {
        var fileRoot = Files.createFile(root.resolve("not-a-directory"));
        var prepareFailure = new FileSystemRepositoryWorkspaceAdapter(fileRoot);

        var prepareError = assertThrows(IllegalStateException.class, () -> prepareFailure.prepare(
            new AnalysisRunId("run-1"),
            new WorkspacePolicy(true, true, false, false, 60, 100_000)
        ));

        var rootWorkspace = new WorkspaceId("workspace-root");
        var cleanupRoot = new FileSystemRepositoryWorkspaceAdapter(root, Map.of(rootWorkspace, root));
        var cleanupError = assertThrows(IllegalStateException.class, () -> cleanupRoot.cleanup(rootWorkspace));

        assertEquals("Failed to prepare repository workspace", prepareError.getMessage());
        assertEquals("Refusing to clean workspace root", cleanupError.getMessage());
    }

    @Test
    void preparesBranchCheckoutUnderOpaqueWorkspaceAndBranchIds() throws Exception {
        var adapter = new FileSystemRepositoryWorkspaceAdapter(root);
        var workspaceId = new WorkspaceId("workspace-0001");
        var workspaceBranchId = new WorkspaceBranchId("workspace-branch-0001");

        var prepared = adapter.prepareBranchCheckout(
            workspaceId,
            workspaceBranchId,
            new WorkspacePolicy(true, true, false, false, 60, 100_000)
        );
        var marker = prepared.workspacePath().resolve("checkout").resolve("marker.txt");
        Files.createDirectories(marker.getParent());
        Files.writeString(marker, "demo");

        assertEquals(workspaceId, prepared.workspaceId());
        assertTrue(prepared.workspacePath().startsWith(root.toRealPath()));
        assertTrue(prepared.workspacePath().endsWith(Path.of("workspace-0001", "branches", "workspace-branch-0001")));
        assertFalse(prepared.workspacePath().toString().contains("feature"));

        adapter.cleanupBranchCheckout(workspaceId, workspaceBranchId);
        adapter.cleanupBranchCheckout(workspaceId, workspaceBranchId);

        assertFalse(Files.exists(prepared.workspacePath()));
        assertTrue(Files.exists(root));
    }

    @Test
    void cleansPreparedWorkspaceAfterAdapterRestart() throws Exception {
        var first = new FileSystemRepositoryWorkspaceAdapter(root);
        var workspace = first.prepare(
            new AnalysisRunId("run-1"),
            new WorkspacePolicy(true, true, false, false, 60, 100_000)
        );
        Files.writeString(workspace.workspacePath().resolve("marker.txt"), "demo");

        new FileSystemRepositoryWorkspaceAdapter(root).cleanup(workspace.workspaceId());

        assertFalse(Files.exists(workspace.workspacePath()));
        assertTrue(Files.exists(root));
    }

    @Test
    void cleansBranchCheckoutAfterAdapterRestart() throws Exception {
        var first = new FileSystemRepositoryWorkspaceAdapter(root);
        var workspaceId = new WorkspaceId("workspace-0001");
        var workspaceBranchId = new WorkspaceBranchId("workspace-branch-0001");
        var prepared = first.prepareBranchCheckout(
            workspaceId,
            workspaceBranchId,
            new WorkspacePolicy(true, true, false, false, 60, 100_000)
        );
        Files.writeString(prepared.workspacePath().resolve("marker.txt"), "demo");

        new FileSystemRepositoryWorkspaceAdapter(root).cleanupBranchCheckout(workspaceId, workspaceBranchId);

        assertFalse(Files.exists(prepared.workspacePath()));
        assertTrue(Files.exists(root.resolve(workspaceId.value())));
    }

    @Test
    void rejectsEscapedWorkspaceMappingsDuringCleanup() {
        var escapedWorkspace = new WorkspaceId("workspace-escaped");
        var escaped = new FileSystemRepositoryWorkspaceAdapter(root, Map.of(escapedWorkspace, root.resolveSibling("outside")));
        var escapedError = assertThrows(IllegalStateException.class, () -> escaped.cleanup(escapedWorkspace));

        var missingWorkspace = new WorkspaceId("workspace-missing-target");
        var missing = new FileSystemRepositoryWorkspaceAdapter(root, Map.of(missingWorkspace, root.resolve("missing-target")));

        missing.cleanup(missingWorkspace);

        assertEquals("Workspace path escaped configured root", escapedError.getMessage());
    }

    @Test
    void rejectsSymlinkedWorkspaceBeforePreparingBranchCheckout() throws Exception {
        var outside = Files.createTempDirectory(root.getParent(), "outside-workspace-");
        try {
            Files.createSymbolicLink(root.resolve("workspace-0001"), outside);
        } catch (UnsupportedOperationException | java.io.IOException error) {
            assumeTrue(false, "symbolic links are not available in this filesystem");
        }
        var adapter = new FileSystemRepositoryWorkspaceAdapter(root);

        var failure = assertThrows(IllegalStateException.class, () -> adapter.prepareBranchCheckout(
            new WorkspaceId("workspace-0001"),
            new WorkspaceBranchId("workspace-branch-0001"),
            new WorkspacePolicy(true, true, false, false, 60, 100_000)
        ));

        assertEquals("Workspace path escaped configured root", failure.getMessage());
        assertFalse(Files.exists(outside.resolve("branches")));
    }

    @Test
    void rejectsSymlinkedBranchAncestorBeforePreparingBranchCheckout() throws Exception {
        var outside = Files.createTempDirectory(root.getParent(), "outside-branches-");
        Files.createDirectories(root.resolve("workspace-0001"));
        try {
            Files.createSymbolicLink(root.resolve("workspace-0001").resolve("branches"), outside);
        } catch (UnsupportedOperationException | java.io.IOException error) {
            assumeTrue(false, "symbolic links are not available in this filesystem");
        }
        var adapter = new FileSystemRepositoryWorkspaceAdapter(root);

        var failure = assertThrows(IllegalStateException.class, () -> adapter.prepareBranchCheckout(
            new WorkspaceId("workspace-0001"),
            new WorkspaceBranchId("workspace-branch-0001"),
            new WorkspacePolicy(true, true, false, false, 60, 100_000)
        ));

        assertEquals("Workspace path escaped configured root", failure.getMessage());
        assertFalse(Files.exists(outside.resolve("workspace-branch-0001")));
    }
}
