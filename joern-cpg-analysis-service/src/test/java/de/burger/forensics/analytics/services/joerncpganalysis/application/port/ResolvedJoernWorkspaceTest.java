package de.burger.forensics.analytics.services.joerncpganalysis.application.port;

import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.SourceSnapshotId;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResolvedJoernWorkspaceTest {
    @Test
    void normalizesResolvedWorkspacePaths() {
        var workspace = new ResolvedJoernWorkspace(
            new SourceSnapshotId("snapshot-1"),
            "workspace-1",
            Path.of("build/../build/test-workspace"),
            List.of(Path.of("build/test-workspace/src")),
            42
        );

        assertEquals(Path.of("build/test-workspace").toAbsolutePath().normalize(), workspace.workspacePath());
        assertEquals(42, workspace.workspaceBytes());
    }

    @Test
    void rejectsInvalidResolvedWorkspaceState() {
        var snapshot = new SourceSnapshotId("snapshot-1");
        var workspacePath = Path.of("build/test-workspace");
        var sourceRoots = List.of(Path.of("build/test-workspace/src"));

        assertThrows(NullPointerException.class, () -> new ResolvedJoernWorkspace(null, "workspace-1", workspacePath, sourceRoots, 1));
        assertThrows(IllegalArgumentException.class, () -> new ResolvedJoernWorkspace(snapshot, " ", workspacePath, sourceRoots, 1));
        assertThrows(NullPointerException.class, () -> new ResolvedJoernWorkspace(snapshot, "workspace-1", null, sourceRoots, 1));
        assertThrows(NullPointerException.class, () -> new ResolvedJoernWorkspace(snapshot, "workspace-1", workspacePath, null, 1));
        assertThrows(IllegalArgumentException.class, () -> new ResolvedJoernWorkspace(snapshot, "workspace-1", workspacePath, List.of(), 1));
        assertThrows(IllegalArgumentException.class, () -> new ResolvedJoernWorkspace(snapshot, "workspace-1", workspacePath, sourceRoots, -1));
    }
}
