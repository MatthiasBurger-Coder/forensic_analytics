package de.burger.forensics.analytics.ingestion.grpc.mapper;

import de.burger.forensics.analytics.application.ingestion.command.ModuleIdentityCommand;
import de.burger.forensics.analytics.ingestion.v1.ModuleIdentity;

public final class ModuleIdentityMapper {
    public ModuleIdentityCommand toCommand(ModuleIdentity identity) {
        return new ModuleIdentityCommand(
            identity.getModuleName(),
            identity.getModulePath()
        );
    }
}
