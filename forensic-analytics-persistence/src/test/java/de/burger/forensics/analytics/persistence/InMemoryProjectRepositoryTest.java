package de.burger.forensics.analytics.persistence;

import de.burger.forensics.analytics.domain.workspace.ProjectId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceProject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryProjectRepositoryTest {
    private static final WorkspaceId WORKSPACE_A = new WorkspaceId("workspace-a");
    private static final WorkspaceId WORKSPACE_B = new WorkspaceId("workspace-b");
    private static final ProjectId PROJECT_A = new ProjectId("project-a");
    private static final ProjectId PROJECT_B = new ProjectId("project-b");

    @Test
    void savesAndFindsProjectsByWorkspace() {
        var repository = new InMemoryProjectRepository();
        var projectB = WorkspaceProject.active(PROJECT_B, WORKSPACE_A, "Project B");
        var projectA = WorkspaceProject.active(PROJECT_A, WORKSPACE_A, "Project A");
        var otherWorkspaceProject = WorkspaceProject.active(new ProjectId("project-c"), WORKSPACE_B, "Project C");

        repository.save(projectB);
        repository.save(projectA);
        repository.save(otherWorkspaceProject);

        assertEquals(projectA, repository.findById(PROJECT_A).orElseThrow());
        assertEquals(List.of(projectA, projectB), repository.findByWorkspace(WORKSPACE_A));
    }

    @Test
    void updatesExistingProject() {
        var repository = new InMemoryProjectRepository();
        var project = WorkspaceProject.active(PROJECT_A, WORKSPACE_A, "Project A");

        repository.save(project);
        repository.update(project.rename("Renamed"));

        assertEquals("Renamed", repository.findById(PROJECT_A).orElseThrow().name());
    }

    @Test
    void returnsEmptyForUnknownProjectOrWorkspace() {
        var repository = new InMemoryProjectRepository();

        assertTrue(repository.findById(PROJECT_A).isEmpty());
        assertEquals(List.of(), repository.findByWorkspace(WORKSPACE_A));
    }

    @Test
    void rejectsMissingValues() {
        var repository = new InMemoryProjectRepository();

        assertThrows(NullPointerException.class, () -> repository.save(null));
        assertThrows(NullPointerException.class, () -> repository.update(null));
        assertThrows(NullPointerException.class, () -> repository.findById(null));
        assertThrows(NullPointerException.class, () -> repository.findByWorkspace(null));
    }
}
