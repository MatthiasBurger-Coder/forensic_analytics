package de.burger.forensics.analytics.application.ingestion.command;

import java.util.Objects;

public record PluginIdentityCommand(String pluginName, String pluginVersion) {
    public PluginIdentityCommand {
        Objects.requireNonNull(pluginName, "pluginName must not be null");
        Objects.requireNonNull(pluginVersion, "pluginVersion must not be null");
    }
}
