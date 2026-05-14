package de.burger.forensics.analytics.domain.audit;

import de.burger.forensics.analytics.domain.workspace.UserId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuditEventTest {
    private static final WorkspaceId WORKSPACE_ID = new WorkspaceId("workspace-a");
    private static final UserId USER_ID = new UserId("user-a");
    private static final Instant OCCURRED_AT = Instant.parse("2026-05-14T10:15:30Z");

    @Test
    void storesStableAuditEventMetadata() {
        var event = new AuditEvent(
            WORKSPACE_ID,
            USER_ID,
            "workspace.created",
            "workspace",
            "workspace-a",
            OCCURRED_AT,
            Map.of("status", "ACTIVE")
        );

        assertEquals(WORKSPACE_ID, event.workspaceId());
        assertEquals(USER_ID, event.actorUserId());
        assertEquals("workspace.created", event.action());
        assertEquals("workspace", event.targetType());
        assertEquals("workspace-a", event.targetId());
        assertEquals(OCCURRED_AT, event.occurredAt());
        assertEquals(Map.of("status", "ACTIVE"), event.metadata());
        assertThrows(UnsupportedOperationException.class, () -> event.metadata().put("x", "y"));
    }

    @Test
    void rejectsIncompleteAuditEvents() {
        assertThrows(NullPointerException.class, () -> event(null, USER_ID, "action", "target", "id", OCCURRED_AT, Map.of()));
        assertThrows(NullPointerException.class, () -> event(WORKSPACE_ID, null, "action", "target", "id", OCCURRED_AT, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> event(WORKSPACE_ID, USER_ID, null, "target", "id", OCCURRED_AT, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> event(WORKSPACE_ID, USER_ID, " ", "target", "id", OCCURRED_AT, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> event(WORKSPACE_ID, USER_ID, "action", null, "id", OCCURRED_AT, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> event(WORKSPACE_ID, USER_ID, "action", " ", "id", OCCURRED_AT, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> event(WORKSPACE_ID, USER_ID, "action", "target", null, OCCURRED_AT, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> event(WORKSPACE_ID, USER_ID, "action", "target", " ", OCCURRED_AT, Map.of()));
        assertThrows(NullPointerException.class, () -> event(WORKSPACE_ID, USER_ID, "action", "target", "id", null, Map.of()));
        assertThrows(NullPointerException.class, () -> event(WORKSPACE_ID, USER_ID, "action", "target", "id", OCCURRED_AT, null));
        assertThrows(IllegalArgumentException.class, () -> event(WORKSPACE_ID, USER_ID, "action", "target", "id", OCCURRED_AT, Map.of(" ", "value")));
        assertThrows(IllegalArgumentException.class, () -> event(WORKSPACE_ID, USER_ID, "action", "target", "id", OCCURRED_AT, Map.of("key", " ")));
    }

    private static AuditEvent event(
        WorkspaceId workspaceId,
        UserId actorUserId,
        String action,
        String targetType,
        String targetId,
        Instant occurredAt,
        Map<String, String> metadata
    ) {
        return new AuditEvent(workspaceId, actorUserId, action, targetType, targetId, occurredAt, metadata);
    }
}
