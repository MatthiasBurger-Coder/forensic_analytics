package de.burger.forensics.analytics.domain.repository;

public record RepositoryMetadata(
    String projectId,
    String repositoryLocation,
    String branchName,
    String commitHash
) {
    public RepositoryMetadata {
        requireText(projectId, "project id");
        requireText(repositoryLocation, "repository location");
        requireText(branchName, "branch name");
        requireText(commitHash, "commit hash");
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
