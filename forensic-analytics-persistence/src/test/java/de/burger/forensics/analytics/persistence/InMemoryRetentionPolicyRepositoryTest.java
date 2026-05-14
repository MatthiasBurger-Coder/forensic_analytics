package de.burger.forensics.analytics.persistence;

import de.burger.forensics.analytics.domain.workspace.RetentionPolicy;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryRetentionPolicyRepositoryTest {
    private static final WorkspaceId WORKSPACE_A = new WorkspaceId("workspace-a");

    @Test
    void savesAndUpdatesRetentionPolicy() {
        var repository = new InMemoryRetentionPolicyRepository();

        repository.save(WORKSPACE_A, new RetentionPolicy(30));
        repository.save(WORKSPACE_A, new RetentionPolicy(60));

        assertEquals(new RetentionPolicy(60), repository.findByWorkspace(WORKSPACE_A).orElseThrow());
    }

    @Test
    void returnsEmptyForMissingWorkspace() {
        assertTrue(new InMemoryRetentionPolicyRepository().findByWorkspace(WORKSPACE_A).isEmpty());
    }

    @Test
    void rejectsMissingValues() {
        var repository = new InMemoryRetentionPolicyRepository();

        assertThrows(NullPointerException.class, () -> repository.save(null, new RetentionPolicy(30)));
        assertThrows(NullPointerException.class, () -> repository.save(WORKSPACE_A, null));
        assertThrows(NullPointerException.class, () -> repository.findByWorkspace(null));
    }
}
