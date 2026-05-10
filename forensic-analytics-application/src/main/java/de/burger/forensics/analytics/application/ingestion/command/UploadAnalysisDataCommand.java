package de.burger.forensics.analytics.application.ingestion.command;

import de.burger.forensics.analytics.domain.ingestion.AnalysisPayloadDescriptor;

import java.util.Arrays;
import java.util.Objects;

public record UploadAnalysisDataCommand(
    String sessionId,
    BuildIdentityCommand buildIdentity,
    ModuleIdentityCommand moduleIdentity,
    PluginIdentityCommand pluginIdentity,
    String schemaVersion,
    AnalysisPayloadDescriptor payloadDescriptor,
    byte[] payload
) {
    public UploadAnalysisDataCommand {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(buildIdentity, "buildIdentity must not be null");
        Objects.requireNonNull(moduleIdentity, "moduleIdentity must not be null");
        Objects.requireNonNull(pluginIdentity, "pluginIdentity must not be null");
        Objects.requireNonNull(schemaVersion, "schemaVersion must not be null");
        Objects.requireNonNull(payloadDescriptor, "payloadDescriptor must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        payload = Arrays.copyOf(payload, payload.length);
    }

    @Override
    public byte[] payload() {
        return Arrays.copyOf(payload, payload.length);
    }
}
