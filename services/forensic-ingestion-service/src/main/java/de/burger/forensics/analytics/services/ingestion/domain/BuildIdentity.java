package de.burger.forensics.analytics.services.ingestion.domain;

public record BuildIdentity(
    String projectId,
    String repositoryUrl,
    String branchName,
    String commitHash,
    String buildId,
    String scanTimestamp
) {
    public BuildIdentity {
        projectId = RequiredText.require(projectId, "projectId");
        repositoryUrl = RequiredText.require(repositoryUrl, "repositoryUrl");
        commitHash = RequiredText.require(commitHash, "commitHash");
        buildId = RequiredText.require(buildId, "buildId");
        branchName = branchName == null ? "" : branchName.strip();
        scanTimestamp = scanTimestamp == null ? "" : scanTimestamp.strip();
    }
}
