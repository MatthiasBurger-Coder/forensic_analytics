package de.burger.forensics.analytics.services.ingestion.domain;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record IngestionSession(
    String sessionId,
    BuildIdentity buildIdentity,
    PluginIdentity pluginIdentity,
    String schemaVersion,
    IngestionSessionState state,
    Map<String, RawIngestionPayload> payloads,
    String abortReason
) {
    public IngestionSession {
        sessionId = RequiredText.require(sessionId, "sessionId");
        Objects.requireNonNull(buildIdentity, "buildIdentity must not be null");
        Objects.requireNonNull(pluginIdentity, "pluginIdentity must not be null");
        schemaVersion = RequiredText.require(schemaVersion, "schemaVersion");
        Objects.requireNonNull(state, "state must not be null");
        payloads = Map.copyOf(Objects.requireNonNull(payloads, "payloads must not be null"));
        abortReason = abortReason == null ? "" : abortReason.strip();
    }

    public static IngestionSession accepted(
        String sessionId,
        BuildIdentity buildIdentity,
        PluginIdentity pluginIdentity,
        String schemaVersion
    ) {
        return new IngestionSession(
            sessionId,
            buildIdentity,
            pluginIdentity,
            schemaVersion,
            IngestionSessionState.ACCEPTED,
            Map.of(),
            ""
        );
    }

    public PayloadAcceptance accept(RawIngestionPayload payload) {
        Objects.requireNonNull(payload, "payload must not be null");
        requireAccepted("accept payloads");
        requireMatchingSessionProvenance(payload);
        var existingPayload = payloads.get(payload.payloadId());
        if (existingPayload != null && existingPayload.equals(payload)) {
            return new PayloadAcceptance(this, false);
        }
        if (existingPayload != null) {
            throw new IllegalArgumentException("payloadId " + payload.payloadId() + " conflicts with existing payload");
        }
        var nextPayloads = new LinkedHashMap<>(payloads);
        nextPayloads.put(payload.payloadId(), payload);
        return new PayloadAcceptance(new IngestionSession(
            sessionId,
            buildIdentity,
            pluginIdentity,
            schemaVersion,
            state,
            nextPayloads,
            abortReason
        ), true);
    }

    public IngestionSession completed() {
        requireAccepted("complete");
        return new IngestionSession(
            sessionId,
            buildIdentity,
            pluginIdentity,
            schemaVersion,
            IngestionSessionState.COMPLETED,
            payloads,
            ""
        );
    }

    public IngestionSession aborted(String reason) {
        requireAccepted("abort");
        return new IngestionSession(
            sessionId,
            buildIdentity,
            pluginIdentity,
            schemaVersion,
            IngestionSessionState.ABORTED,
            payloads,
            RequiredText.require(reason, "reason")
        );
    }

    public int receivedItems() {
        return payloads.size();
    }

    private void requireAccepted(String action) {
        if (state != IngestionSessionState.ACCEPTED) {
            throw new IllegalStateException("session " + sessionId + " cannot " + action + " while " + state);
        }
    }

    private void requireMatchingSessionProvenance(RawIngestionPayload payload) {
        if (!buildIdentity.equals(payload.buildIdentity())) {
            throw new IllegalArgumentException("payload build identity does not match session build identity");
        }
        if (!pluginIdentity.equals(payload.pluginIdentity())) {
            throw new IllegalArgumentException("payload plugin identity does not match session plugin identity");
        }
        if (!schemaVersion.equals(payload.schemaVersion())) {
            throw new IllegalArgumentException("payload schema version does not match session schema version");
        }
    }

    public record PayloadAcceptance(IngestionSession session, boolean acceptedNewPayload) {
    }
}
