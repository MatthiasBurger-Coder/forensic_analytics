package de.burger.forensics.analytics.persistence;

import de.burger.forensics.analytics.domain.workspace.ProjectId;
import de.burger.forensics.analytics.domain.workspace.ProjectMembership;
import de.burger.forensics.analytics.domain.workspace.UserId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceProject;
import de.burger.forensics.analytics.domain.workspace.WorkspaceRole;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryProjectMembershipRepositoryTest {
    private static final WorkspaceId WORKSPACE_A = new WorkspaceId("workspace-a");
    private static final WorkspaceId WORKSPACE_B = new WorkspaceId("workspace-b");
    private static final ProjectId PROJECT_A = new ProjectId("project-a");
    private static final ProjectId PROJECT_B = new ProjectId("project-b");
    private static final UserId USER_A = new UserId("user-a");
    private static final UserId USER_B = new UserId("user-b");

    @Test
    void savesAndFindsProjectMembershipsByProjectAndUser() {
        var repository = new InMemoryProjectMembershipRepository();
        var membershipA = membership(WORKSPACE_A, PROJECT_A, USER_A, WorkspaceRole.ANALYST);
        var membershipB = membership(WORKSPACE_A, PROJECT_A, USER_B, WorkspaceRole.REVIEWER);
        var membershipOtherProject = membership(WORKSPACE_A, PROJECT_B, USER_A, WorkspaceRole.VIEWER);
        var membershipOtherWorkspace = membership(WORKSPACE_B, PROJECT_A, USER_A, WorkspaceRole.VIEWER);

        repository.save(membershipB);
        repository.save(membershipA);
        repository.save(membershipOtherProject);
        repository.save(membershipOtherWorkspace);

        assertEquals(membershipA, repository.findMembership(WORKSPACE_A, PROJECT_A, USER_A).orElseThrow());
        assertEquals(List.of(membershipA, membershipB), repository.findByProject(WORKSPACE_A, PROJECT_A));
        assertEquals(List.of(membershipA, membershipOtherProject, membershipOtherWorkspace), repository.findByUser(USER_A));
    }

    @Test
    void updatesAndRemovesMembership() {
        var repository = new InMemoryProjectMembershipRepository();
        var membership = membership(WORKSPACE_A, PROJECT_A, USER_A, WorkspaceRole.ANALYST);

        repository.save(membership);
        repository.save(membership(WORKSPACE_A, PROJECT_A, USER_A, WorkspaceRole.REVIEWER));
        repository.removeMembership(WORKSPACE_A, PROJECT_A, USER_A);

        assertTrue(repository.findMembership(WORKSPACE_A, PROJECT_A, USER_A).isEmpty());
    }

    @Test
    void returnsEmptyForUnknownMemberships() {
        var repository = new InMemoryProjectMembershipRepository();

        assertTrue(repository.findMembership(WORKSPACE_A, PROJECT_A, USER_A).isEmpty());
        assertEquals(List.of(), repository.findByProject(WORKSPACE_A, PROJECT_A));
        assertEquals(List.of(), repository.findByUser(USER_A));
    }

    @Test
    void rejectsMissingValues() {
        var repository = new InMemoryProjectMembershipRepository();

        assertThrows(NullPointerException.class, () -> repository.save(null));
        assertThrows(NullPointerException.class, () -> repository.findMembership(null, PROJECT_A, USER_A));
        assertThrows(NullPointerException.class, () -> repository.findMembership(WORKSPACE_A, null, USER_A));
        assertThrows(NullPointerException.class, () -> repository.findMembership(WORKSPACE_A, PROJECT_A, null));
        assertThrows(NullPointerException.class, () -> repository.findByProject(null, PROJECT_A));
        assertThrows(NullPointerException.class, () -> repository.findByProject(WORKSPACE_A, null));
        assertThrows(NullPointerException.class, () -> repository.findByUser(null));
        assertThrows(NullPointerException.class, () -> repository.removeMembership(null, PROJECT_A, USER_A));
        assertThrows(NullPointerException.class, () -> repository.removeMembership(WORKSPACE_A, null, USER_A));
        assertThrows(NullPointerException.class, () -> repository.removeMembership(WORKSPACE_A, PROJECT_A, null));
    }

    private static ProjectMembership membership(
        WorkspaceId workspaceId,
        ProjectId projectId,
        UserId userId,
        WorkspaceRole role
    ) {
        return new ProjectMembership(WorkspaceProject.active(projectId, workspaceId, projectId.value()), userId, role);
    }
}
