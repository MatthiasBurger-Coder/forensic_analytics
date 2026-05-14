package de.burger.forensics.analytics.domain.audit;

import de.burger.forensics.analytics.domain.workspace.UserId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public record AuditEvent(
    WorkspaceId workspaceId,
    UserId actorUserId,
    String action,
    String targetType,
    String targetId,
    Instant occurredAt,
    Map<String, String> metadata
) {
    public AuditEvent {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(actorUserId, "actorUserId must not be null");
        action = requireText(action, "action");
        targetType = requireText(targetType, "targetType");
        targetId = requireText(targetId, "targetId");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        metadata = immutableSortedMetadata(metadata);
    }

    private static Map<String, String> immutableSortedMetadata(Map<String, String> metadata) {
        Objects.requireNonNull(metadata, "metadata must not be null");
        var sortedMetadata = new TreeMap<String, String>();
        metadata.forEach((key, value) -> sortedMetadata.put(
            requireText(key, "metadata key"),
            requireText(value, "metadata value")
        ));
        return Collections.unmodifiableMap(sortedMetadata);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
