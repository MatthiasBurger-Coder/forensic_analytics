package de.burger.forensics.analytics.services.ingestion.domain;

public record PluginIdentity(String pluginName, String pluginVersion) {
    public PluginIdentity {
        pluginName = RequiredText.require(pluginName, "pluginName");
        pluginVersion = RequiredText.require(pluginVersion, "pluginVersion");
    }
}
