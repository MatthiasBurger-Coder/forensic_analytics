package de.burger.forensics.analytics.ingestion.grpc.mapper;

import de.burger.forensics.analytics.application.ingestion.command.BuildIdentityCommand;
import de.burger.forensics.analytics.ingestion.v1.BuildIdentity;

public final class BuildIdentityMapper {
    public BuildIdentityCommand toCommand(BuildIdentity identity) {
        return new BuildIdentityCommand(
            identity.getProjectId(),
            identity.getRepositoryUrl(),
            identity.getBranchName(),
            identity.getCommitHash(),
            identity.getBuildId(),
            identity.getScanTimestamp()
        );
    }
}
