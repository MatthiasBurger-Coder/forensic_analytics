package de.burger.forensics.analytics.persistence.storage;

import de.burger.forensics.analytics.domain.workspace.ProjectId;
import de.burger.forensics.analytics.domain.workspace.ProjectStorageArea;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IsolatedProjectStoragePathResolverTest {
    @TempDir
    Path tempDir;

    @Test
    void resolvesProjectStorageAreasUnderWorkspaceAndProject() {
        var resolver = new IsolatedProjectStoragePathResolver(tempDir);
        var project = project("workspace-a", "project-a");

        assertEquals(
            tempDir.toAbsolutePath().normalize()
                .resolve("workspaces")
                .resolve("workspace-a")
                .resolve("projects")
                .resolve("project-a")
                .resolve("evidence_original"),
            resolver.projectArea(project, ProjectStorageArea.EVIDENCE_ORIGINAL)
        );
    }

    @Test
    void resolvesStoredFilesInsideProjectArea() {
        var resolver = new IsolatedProjectStoragePathResolver(tempDir);
        var project = project("workspace-a", "project-a");

        var resolved = resolver.projectFile(project, ProjectStorageArea.REPORTS, "report.json");

        assertTrue(resolved.startsWith(tempDir.toAbsolutePath().normalize()));
        assertEquals(
            tempDir.toAbsolutePath().normalize()
                .resolve("workspaces")
                .resolve("workspace-a")
                .resolve("projects")
                .resolve("project-a")
                .resolve("reports")
                .resolve("report.json"),
            resolved
        );
    }

    @Test
    void resolvesSharedWorkspaceFilesSeparatelyFromProjectFiles() {
        var resolver = new IsolatedProjectStoragePathResolver(tempDir);

        var sharedFile = resolver.sharedFile(new WorkspaceId("workspace-a"), "shared.bin");

        assertEquals(
            tempDir.toAbsolutePath().normalize()
                .resolve("workspaces")
                .resolve("workspace-a")
                .resolve("shared")
                .resolve("shared.bin"),
            sharedFile
        );
    }

    @Test
    void exposesExpectedDeterministicStorageDirectories() {
        assertEquals(
            Set.of("evidence_original", "evidence_processed", "analysis_results", "reports", "logs"),
            Stream.of(ProjectStorageArea.values())
                .map(ProjectStorageArea::directoryName)
                .collect(Collectors.toSet())
        );
    }

    @Test
    void rejectsPathTraversalAndNestedClientPaths() {
        var resolver = new IsolatedProjectStoragePathResolver(tempDir);

        assertThrows(
            IllegalArgumentException.class,
            () -> resolver.projectArea(project("../workspace-a", "project-a"), ProjectStorageArea.EVIDENCE_ORIGINAL)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> resolver.projectArea(project("workspace-a", "project/../b"), ProjectStorageArea.EVIDENCE_ORIGINAL)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> resolver.projectFile(project("workspace-a", "project-a"), ProjectStorageArea.REPORTS, "../report.json")
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> resolver.projectFile(project("workspace-a", "project-a"), ProjectStorageArea.REPORTS, "nested/report.json")
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> resolver.projectFile(project("workspace-a", "project-a"), ProjectStorageArea.REPORTS, "..")
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> resolver.projectFile(project("workspace-a", "project-a"), ProjectStorageArea.REPORTS, ".")
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> resolver.projectFile(project("workspace-a", "project-a"), ProjectStorageArea.REPORTS, "nested\\report.json")
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> resolver.sharedArea(new WorkspaceId("../workspace-a"))
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> resolver.sharedFile(new WorkspaceId("workspace-a"), "nested/shared.bin")
        );
    }

    @Test
    void rejectsMissingValues() {
        var resolver = new IsolatedProjectStoragePathResolver(tempDir);
        var project = project("workspace-a", "project-a");

        assertThrows(NullPointerException.class, () -> new IsolatedProjectStoragePathResolver(null));
        assertThrows(NullPointerException.class, () -> resolver.projectArea(null, ProjectStorageArea.REPORTS));
        assertThrows(NullPointerException.class, () -> resolver.projectArea(project, null));
        assertThrows(NullPointerException.class, () -> resolver.sharedArea(null));
        assertThrows(IllegalArgumentException.class, () -> resolver.projectFile(project, ProjectStorageArea.REPORTS, null));
        assertThrows(IllegalArgumentException.class, () -> resolver.projectFile(project, ProjectStorageArea.REPORTS, " "));
        assertThrows(IllegalArgumentException.class, () -> resolver.sharedFile(new WorkspaceId("workspace-a"), null));
        assertThrows(IllegalArgumentException.class, () -> resolver.sharedFile(new WorkspaceId("workspace-a"), " "));
    }

    private static WorkspaceProject project(String workspaceId, String projectId) {
        return WorkspaceProject.active(new ProjectId(projectId), new WorkspaceId(workspaceId), "Project");
    }
}
