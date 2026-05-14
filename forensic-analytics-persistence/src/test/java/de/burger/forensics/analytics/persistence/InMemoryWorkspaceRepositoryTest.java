package de.burger.forensics.analytics.persistence;

import de.burger.forensics.analytics.domain.workspace.UserId;
import de.burger.forensics.analytics.domain.workspace.Workspace;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceMembership;
import de.burger.forensics.analytics.domain.workspace.WorkspaceRole;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryWorkspaceRepositoryTest {
    private static final WorkspaceId WORKSPACE_A = new WorkspaceId("workspace-a");
    private static final WorkspaceId WORKSPACE_B = new WorkspaceId("workspace-b");
    private static final UserId USER_A = new UserId("user-a");

    @Test
    void savesWorkspacesAndMemberships() {
        var repository = new InMemoryWorkspaceRepository();
        var workspaceA = Workspace.active(WORKSPACE_A, "Workspace A");
        var workspaceB = Workspace.active(WORKSPACE_B, "Workspace B");

        repository.save(workspaceB);
        repository.save(workspaceA);
        repository.saveMembership(new WorkspaceMembership(WORKSPACE_A, USER_A, WorkspaceRole.OWNER));
        repository.saveMembership(new WorkspaceMembership(WORKSPACE_B, USER_A, WorkspaceRole.VIEWER));

        assertEquals(workspaceA, repository.findById(WORKSPACE_A).orElseThrow());
        assertEquals(
            new WorkspaceMembership(WORKSPACE_A, USER_A, WorkspaceRole.OWNER),
            repository.findMembership(WORKSPACE_A, USER_A).orElseThrow()
        );
        assertEquals(
            List.of(new WorkspaceMembership(WORKSPACE_A, USER_A, WorkspaceRole.OWNER)),
            repository.findMemberships(WORKSPACE_A)
        );
        assertEquals(List.of(workspaceA, workspaceB), repository.findByMember(USER_A));
    }

    @Test
    void updatesExistingWorkspaceAndMembership() {
        var repository = new InMemoryWorkspaceRepository();
        var workspace = Workspace.active(WORKSPACE_A, "Workspace A");

        repository.save(workspace);
        repository.update(workspace.rename("Renamed"));
        repository.saveMembership(new WorkspaceMembership(WORKSPACE_A, USER_A, WorkspaceRole.VIEWER));
        repository.saveMembership(new WorkspaceMembership(WORKSPACE_A, USER_A, WorkspaceRole.ADMIN));

        assertEquals("Renamed", repository.findById(WORKSPACE_A).orElseThrow().name());
        assertEquals(
            new WorkspaceMembership(WORKSPACE_A, USER_A, WorkspaceRole.ADMIN),
            repository.findMembership(WORKSPACE_A, USER_A).orElseThrow()
        );
    }

    @Test
    void returnsEmptyForUnknownWorkspaceOrMembership() {
        var repository = new InMemoryWorkspaceRepository();

        assertTrue(repository.findById(WORKSPACE_A).isEmpty());
        assertTrue(repository.findMembership(WORKSPACE_A, USER_A).isEmpty());
        assertEquals(List.of(), repository.findMemberships(WORKSPACE_A));
        assertEquals(List.of(), repository.findByMember(USER_A));
    }

    @Test
    void removesMembership() {
        var repository = new InMemoryWorkspaceRepository();
        repository.saveMembership(new WorkspaceMembership(WORKSPACE_A, USER_A, WorkspaceRole.OWNER));

        repository.removeMembership(WORKSPACE_A, USER_A);

        assertTrue(repository.findMembership(WORKSPACE_A, USER_A).isEmpty());
    }

    @Test
    void rejectsMissingValues() {
        var repository = new InMemoryWorkspaceRepository();

        assertThrows(NullPointerException.class, () -> repository.save(null));
        assertThrows(NullPointerException.class, () -> repository.update(null));
        assertThrows(NullPointerException.class, () -> repository.findById(null));
        assertThrows(NullPointerException.class, () -> repository.findByMember(null));
        assertThrows(NullPointerException.class, () -> repository.saveMembership(null));
        assertThrows(NullPointerException.class, () -> repository.findMembership(null, USER_A));
        assertThrows(NullPointerException.class, () -> repository.findMembership(WORKSPACE_A, null));
        assertThrows(NullPointerException.class, () -> repository.findMemberships(null));
        assertThrows(NullPointerException.class, () -> repository.removeMembership(null, USER_A));
        assertThrows(NullPointerException.class, () -> repository.removeMembership(WORKSPACE_A, null));
    }
}
