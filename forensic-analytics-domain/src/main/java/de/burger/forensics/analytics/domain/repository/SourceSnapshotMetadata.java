package de.burger.forensics.analytics.domain.repository;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record SourceSnapshotMetadata(
    String projectId,
    String repositoryLocation,
    Optional<String> branchName,
    Optional<String> commitHash,
    Optional<Instant> capturedAt
) {
    public SourceSnapshotMetadata {
        RequiredRepositoryText.requireText(projectId, "project id");
        RequiredRepositoryText.requireText(repositoryLocation, "repository location");
        branchName = copyOptionalText(branchName, "branch name");
        commitHash = copyOptionalText(commitHash, "commit hash");
        capturedAt = Objects.requireNonNull(capturedAt, "capturedAt must not be null");
    }

    public static SourceSnapshotMetadata fromRepositoryMetadata(
        RepositoryMetadata metadata,
        Optional<Instant> capturedAt
    ) {
        Objects.requireNonNull(metadata, "metadata must not be null");
        Objects.requireNonNull(capturedAt, "capturedAt must not be null");
        return new SourceSnapshotMetadata(
            metadata.projectId(),
            metadata.repositoryLocation(),
            Optional.of(metadata.branchName()),
            Optional.of(metadata.commitHash()),
            capturedAt
        );
    }

    private static Optional<String> copyOptionalText(Optional<String> value, String fieldName) {
        var copied = Objects.requireNonNull(value, fieldName + " must not be null");
        copied.ifPresent(text -> RequiredRepositoryText.requireText(text, fieldName));
        return copied;
    }
}
