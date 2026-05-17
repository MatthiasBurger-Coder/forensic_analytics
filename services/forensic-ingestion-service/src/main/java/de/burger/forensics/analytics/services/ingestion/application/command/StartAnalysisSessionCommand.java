package de.burger.forensics.analytics.services.ingestion.application.command;

import de.burger.forensics.analytics.services.ingestion.domain.BuildIdentity;
import de.burger.forensics.analytics.services.ingestion.domain.PluginIdentity;

import java.util.Objects;

public record StartAnalysisSessionCommand(
    BuildIdentity buildIdentity,
    PluginIdentity pluginIdentity,
    String schemaVersion
) {
    public StartAnalysisSessionCommand {
        Objects.requireNonNull(buildIdentity, "buildIdentity must not be null");
        Objects.requireNonNull(pluginIdentity, "pluginIdentity must not be null");
        if (schemaVersion == null || schemaVersion.isBlank()) {
            throw new IllegalArgumentException("schemaVersion must not be blank");
        }
        schemaVersion = schemaVersion.strip();
    }
}
