package de.burger.forensics.analytics.persistence;

import de.burger.forensics.analytics.domain.audit.AuditEvent;
import de.burger.forensics.analytics.domain.workspace.UserId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryAuditEventRepositoryTest {
    private static final WorkspaceId WORKSPACE_A = new WorkspaceId("workspace-a");
    private static final WorkspaceId WORKSPACE_B = new WorkspaceId("workspace-b");
    private static final UserId USER_A = new UserId("user-a");

    @Test
    void appendsPublishesAndFindsWorkspaceEventsDeterministically() {
        var repository = new InMemoryAuditEventRepository();
        var later = event(WORKSPACE_A, "workspace.updated", "workspace", "workspace-a", "2026-05-14T10:01:00Z");
        var earlier = event(WORKSPACE_A, "project.created", "project", "project-a", "2026-05-14T10:00:00Z");
        var sameTime = event(WORKSPACE_A, "project.archived", "project", "project-a", "2026-05-14T10:00:00Z");

        repository.append(later);
        repository.publish(event(WORKSPACE_B, "workspace.created", "workspace", "workspace-b", "2026-05-14T09:00:00Z"));
        repository.append(sameTime);
        repository.append(earlier);

        assertEquals(List.of(sameTime, earlier, later), repository.findByWorkspace(WORKSPACE_A));
    }

    @Test
    void rejectsMissingValues() {
        var repository = new InMemoryAuditEventRepository();

        assertThrows(NullPointerException.class, () -> repository.append(null));
        assertThrows(NullPointerException.class, () -> repository.publish(null));
        assertThrows(NullPointerException.class, () -> repository.findByWorkspace(null));
    }

    private static AuditEvent event(
        WorkspaceId workspaceId,
        String action,
        String targetType,
        String targetId,
        String occurredAt
    ) {
        return new AuditEvent(
            workspaceId,
            USER_A,
            action,
            targetType,
            targetId,
            Instant.parse(occurredAt),
            Map.of()
        );
    }
}
