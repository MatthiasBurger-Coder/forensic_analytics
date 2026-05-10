package de.burger.forensics.analytics.ingestion.grpc.mapper;

import de.burger.forensics.analytics.application.ingestion.command.PluginIdentityCommand;
import de.burger.forensics.analytics.ingestion.v1.PluginIdentity;

public final class PluginIdentityMapper {
    public PluginIdentityCommand toCommand(PluginIdentity identity) {
        return new PluginIdentityCommand(
            identity.getPluginName(),
            identity.getPluginVersion()
        );
    }
}
