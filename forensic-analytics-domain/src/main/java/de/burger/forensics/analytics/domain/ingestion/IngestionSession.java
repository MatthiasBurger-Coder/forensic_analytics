package de.burger.forensics.analytics.domain.ingestion;

import java.util.Objects;

public record IngestionSession(
    String sessionId,
    String projectId,
    String schemaVersion,
    IngestionSessionState state,
    long receivedItems
) {
    public IngestionSession {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(projectId, "projectId must not be null");
        Objects.requireNonNull(schemaVersion, "schemaVersion must not be null");
        Objects.requireNonNull(state, "state must not be null");
        if (receivedItems < 0) {
            throw new IllegalArgumentException("receivedItems must not be negative");
        }
    }

    public static IngestionSession start(String sessionId, String projectId, String schemaVersion) {
        return new IngestionSession(sessionId, projectId, schemaVersion, IngestionSessionState.ACTIVE, 0);
    }

    public boolean acceptsPayload() {
        return state == IngestionSessionState.ACTIVE;
    }

    public IngestionSession withReceivedItems(long nextReceivedItems) {
        return new IngestionSession(sessionId, projectId, schemaVersion, state, nextReceivedItems);
    }

    public IngestionSession complete() {
        return new IngestionSession(sessionId, projectId, schemaVersion, IngestionSessionState.COMPLETED, receivedItems);
    }

    public IngestionSession abort() {
        return new IngestionSession(sessionId, projectId, schemaVersion, IngestionSessionState.ABORTED, receivedItems);
    }
}
