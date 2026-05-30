package de.burger.forensics.analytics.services.ingestion.adapter.in.file;

import de.burger.forensics.analytics.services.ingestion.domain.BuildIdentity;
import de.burger.forensics.analytics.services.ingestion.domain.ModuleIdentity;
import de.burger.forensics.analytics.services.ingestion.domain.PluginIdentity;

import java.util.List;
import java.util.Objects;

public record EngineIngestionRequest(
    String schemaVersion,
    BuildIdentity buildIdentity,
    ModuleIdentity moduleIdentity,
    PluginIdentity pluginIdentity,
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
