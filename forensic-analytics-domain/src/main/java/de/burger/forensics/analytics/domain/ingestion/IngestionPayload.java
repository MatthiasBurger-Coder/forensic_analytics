package de.burger.forensics.analytics.domain.ingestion;

import java.util.Arrays;
import java.util.Objects;

public record IngestionPayload(
    String sessionId,
    String moduleName,
    String schemaVersion,
    String payloadType,
    byte[] payload
) {
    public IngestionPayload {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(moduleName, "moduleName must not be null");
        Objects.requireNonNull(schemaVersion, "schemaVersion must not be null");
        Objects.requireNonNull(payloadType, "payloadType must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        payload = Arrays.copyOf(payload, payload.length);
    }

    @Override
    public byte[] payload() {
        return Arrays.copyOf(payload, payload.length);
    }
}
