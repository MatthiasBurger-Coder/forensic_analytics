package de.burger.forensics.analytics.application.ingestion.command;

import java.util.Objects;

public record BuildIdentityCommand(
    String projectId,
    String repositoryUrl,
    String branchName,
    String commitHash,
    String buildId,
    String scanTimestamp
) {
    public BuildIdentityCommand {
        Objects.requireNonNull(projectId, "projectId must not be null");
        Objects.requireNonNull(repositoryUrl, "repositoryUrl must not be null");
        Objects.requireNonNull(branchName, "branchName must not be null");
        Objects.requireNonNull(commitHash, "commitHash must not be null");
        Objects.requireNonNull(buildId, "buildId must not be null");
        Objects.requireNonNull(scanTimestamp, "scanTimestamp must not be null");
    }
}
