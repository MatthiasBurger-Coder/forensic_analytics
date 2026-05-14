package de.burger.forensics.analytics.domain.repository;

import de.burger.forensics.analytics.domain.artifact.ArtifactReference;

import java.util.List;
import java.util.Objects;

public record SourceSnapshot(
    SourceSnapshotId id,
    SourceSnapshotMetadata metadata,
    ArtifactReference sourceArtifact,
    List<String> sourceRoots,
    SourceSnapshotCompleteness completeness,
    List<String> limitations
) {
    public SourceSnapshot {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(metadata, "metadata must not be null");
        Objects.requireNonNull(sourceArtifact, "sourceArtifact must not be null");
        sourceRoots = copySourceRoots(sourceRoots);
        Objects.requireNonNull(completeness, "completeness must not be null");
        limitations = copyLimitations(limitations);
        requireLimitationsForIncompleteSnapshot(completeness, limitations);
    }

    public static SourceSnapshot captured(
        SourceSnapshotMetadata metadata,
        ArtifactReference sourceArtifact,
        List<String> sourceRoots,
        SourceSnapshotCompleteness completeness,
        List<String> limitations
    ) {
        return new SourceSnapshot(
            SourceSnapshotId.deterministic(metadata, sourceArtifact, sourceRoots),
            metadata,
            sourceArtifact,
            sourceRoots,
            completeness,
            limitations
        );
    }

    static List<String> copySourceRoots(List<String> sourceRoots) {
        return List.copyOf(Objects.requireNonNull(sourceRoots, "sourceRoots must not be null")).stream()
            .peek(sourceRoot -> RequiredRepositoryText.requireText(sourceRoot, "source root"))
            .toList();
    }

    private static List<String> copyLimitations(List<String> limitations) {
        return List.copyOf(Objects.requireNonNull(limitations, "limitations must not be null")).stream()
            .peek(limitation -> RequiredRepositoryText.requireText(limitation, "limitation"))
            .toList();
    }

    private static void requireLimitationsForIncompleteSnapshot(
        SourceSnapshotCompleteness completeness,
        List<String> limitations
    ) {
        if (SourceSnapshotCompleteness.INCOMPLETE.equals(completeness) && limitations.isEmpty()) {
            throw new IllegalArgumentException("incomplete source snapshot must include at least one limitation");
        }
    }
}
