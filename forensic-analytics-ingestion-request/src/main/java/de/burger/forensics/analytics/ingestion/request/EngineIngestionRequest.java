package de.burger.forensics.analytics.ingestion.request;

import de.burger.forensics.analytics.application.ingestion.command.BuildIdentityCommand;
import de.burger.forensics.analytics.application.ingestion.command.ModuleIdentityCommand;
import de.burger.forensics.analytics.application.ingestion.command.PluginIdentityCommand;

import java.util.List;
import java.util.Objects;

public record EngineIngestionRequest(
    String schemaVersion,
    BuildIdentityCommand buildIdentity,
    ModuleIdentityCommand moduleIdentity,
    PluginIdentityCommand pluginIdentity,
    List<EngineIngestionPayloadReference> payloads
) {
    public EngineIngestionRequest {
        requireText(schemaVersion, "schemaVersion");
        Objects.requireNonNull(buildIdentity, "buildIdentity must not be null");
        Objects.requireNonNull(moduleIdentity, "moduleIdentity must not be null");
        Objects.requireNonNull(pluginIdentity, "pluginIdentity must not be null");
        payloads = List.copyOf(Objects.requireNonNull(payloads, "payloads must not be null"));
        if (payloads.isEmpty()) {
            throw new IllegalArgumentException("payloads must not be empty");
        }
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
