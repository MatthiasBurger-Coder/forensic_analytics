package de.burger.forensics.analytics.application.ingestion.command;

import java.util.Objects;

public record StartAnalysisSessionCommand(
    BuildIdentityCommand buildIdentity,
    PluginIdentityCommand pluginIdentity,
    String schemaVersion
) {
    public StartAnalysisSessionCommand {
        Objects.requireNonNull(buildIdentity, "buildIdentity must not be null");
        Objects.requireNonNull(pluginIdentity, "pluginIdentity must not be null");
        Objects.requireNonNull(schemaVersion, "schemaVersion must not be null");
    }
}
