package de.burger.forensics.analytics.services.repositoryanalysis.adapter.out.filesystem;

import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.WorkspaceId;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.WorkspacePolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileSystemRepositoryWorkspaceAdapterTest {
    @TempDir
    private Path root;

    @Test
    void preparesOpaqueWorkspaceUnderRootAndCleansItWithoutFollowingExternalPaths() throws Exception {
        var adapter = new FileSystemRepositoryWorkspaceAdapter(root);
        var workspace = adapter.prepare(
            new AnalysisRunId("run:1"),
            new WorkspacePolicy(true, true, false, false, 60, 100_000)
        );
        var marker = workspace.workspacePath().resolve("marker.txt");
        Files.writeString(marker, "demo");

        assertTrue(workspace.workspaceId().value().startsWith("workspace-"));
        assertFalse(workspace.workspaceId().value().contains("run"));
        assertTrue(workspace.workspacePath().startsWith(root.toRealPath()));
        assertTrue(Files.exists(marker));

        adapter.cleanup(workspace.workspaceId());
        adapter.cleanup(workspace.workspaceId());
        adapter.cleanup(new WorkspaceId("workspace-unknown"));

        assertFalse(Files.exists(workspace.workspacePath()));
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
    void rejectsEscapedCleanupTargetsAndIgnoresAlreadyMissingWorkspaces() {
        var escapedWorkspace = new WorkspaceId("workspace-escaped");
        var escaped = new FileSystemRepositoryWorkspaceAdapter(root, Map.of(escapedWorkspace, root.resolveSibling("outside")));
        var escapedError = assertThrows(IllegalStateException.class, () -> escaped.cleanup(escapedWorkspace));

        var missingWorkspace = new WorkspaceId("workspace-missing-target");
        var missing = new FileSystemRepositoryWorkspaceAdapter(root, Map.of(missingWorkspace, root.resolve("missing-target")));

        missing.cleanup(missingWorkspace);

        assertEquals("Workspace path escaped configured root", escapedError.getMessage());
    }
}
