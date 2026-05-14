package de.burger.forensics.analytics.domain.repository;

import de.burger.forensics.analytics.domain.artifact.ArtifactReference;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record SourceSnapshotId(String value) {
    public SourceSnapshotId {
        RequiredRepositoryText.requireText(value, "source snapshot id");
    }

    public static SourceSnapshotId deterministic(
        SourceSnapshotMetadata metadata,
        ArtifactReference sourceArtifact,
        List<String> sourceRoots
    ) {
        Objects.requireNonNull(metadata, "metadata must not be null");
        Objects.requireNonNull(sourceArtifact, "sourceArtifact must not be null");
        var canonicalRoots = SourceSnapshot.copySourceRoots(sourceRoots);
        var seed = String.join(
            "\n",
            metadata.projectId(),
            metadata.repositoryLocation(),
            metadata.branchName().orElse(""),
            metadata.commitHash().orElse(""),
            sourceArtifact.path(),
            sourceArtifact.type(),
            sourceArtifact.sha256(),
            Long.toString(sourceArtifact.sizeBytes()),
            String.join("\n", canonicalRoots)
        );
        return new SourceSnapshotId(UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString());
    }
}
