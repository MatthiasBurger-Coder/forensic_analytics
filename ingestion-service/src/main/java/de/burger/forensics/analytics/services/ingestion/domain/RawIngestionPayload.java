package de.burger.forensics.analytics.services.ingestion.domain;

import java.util.Arrays;
import java.util.Objects;

public record RawIngestionPayload(
    BuildIdentity buildIdentity,
    ModuleIdentity moduleIdentity,
    PluginIdentity pluginIdentity,
    String schemaVersion,
    PayloadDescriptor descriptor,
    byte[] payload
) {
    public RawIngestionPayload {
        Objects.requireNonNull(buildIdentity, "buildIdentity must not be null");
        Objects.requireNonNull(moduleIdentity, "moduleIdentity must not be null");
        Objects.requireNonNull(pluginIdentity, "pluginIdentity must not be null");
        schemaVersion = RequiredText.require(schemaVersion, "schemaVersion");
        Objects.requireNonNull(descriptor, "descriptor must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        if (payload.length == 0) {
            throw new IllegalArgumentException("payload must not be empty");
        }
        payload = payload.clone();
    }

    @Override
    public byte[] payload() {
        return payload.clone();
    }

    public String payloadId() {
        return descriptor.payloadId();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof RawIngestionPayload payloadRecord
            && buildIdentity.equals(payloadRecord.buildIdentity)
            && moduleIdentity.equals(payloadRecord.moduleIdentity)
            && pluginIdentity.equals(payloadRecord.pluginIdentity)
            && schemaVersion.equals(payloadRecord.schemaVersion)
            && descriptor.equals(payloadRecord.descriptor)
            && Arrays.equals(payload, payloadRecord.payload);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hash(buildIdentity, moduleIdentity, pluginIdentity, schemaVersion, descriptor)
            + Arrays.hashCode(payload);
    }
}
