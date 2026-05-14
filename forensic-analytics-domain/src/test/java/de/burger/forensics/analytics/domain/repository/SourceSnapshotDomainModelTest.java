package de.burger.forensics.analytics.domain.repository;

import de.burger.forensics.analytics.domain.artifact.ArtifactReference;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SourceSnapshotDomainModelTest {
    @Test
    void snapshotMetadataKeepsUnavailableRevisionFactsExplicit() {
        var metadata = new SourceSnapshotMetadata(
            "project-a",
            "file:///workspace/project",
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );

        assertFalse(metadata.branchName().isPresent());
        assertFalse(metadata.commitHash().isPresent());
        assertFalse(metadata.capturedAt().isPresent());
    }

    @Test
    void snapshotMetadataCanBeCreatedFromRepositoryMetadata() {
        var capturedAt = Instant.parse("2026-05-14T10:15:30Z");

        var snapshotMetadata = SourceSnapshotMetadata.fromRepositoryMetadata(metadata(), Optional.of(capturedAt));

        assertEquals("project-a", snapshotMetadata.projectId());
        assertEquals("file:///workspace/project", snapshotMetadata.repositoryLocation());
        assertEquals(Optional.of("main"), snapshotMetadata.branchName());
        assertEquals(Optional.of("abcdef"), snapshotMetadata.commitHash());
        assertEquals(Optional.of(capturedAt), snapshotMetadata.capturedAt());
    }

    @Test
    void sourceSnapshotIdentityIsDeterministicForTheSameInput() {
        var first = SourceSnapshot.captured(
            SourceSnapshotMetadata.fromRepositoryMetadata(metadata(), Optional.empty()),
            artifact("source.tar", "sha256:source-a"),
            List.of("src/main/java"),
            SourceSnapshotCompleteness.COMPLETE,
            List.of()
        );
        var second = SourceSnapshot.captured(
            SourceSnapshotMetadata.fromRepositoryMetadata(metadata(), Optional.empty()),
            artifact("source.tar", "sha256:source-a"),
            List.of("src/main/java"),
            SourceSnapshotCompleteness.COMPLETE,
            List.of()
        );
        var differentArtifact = SourceSnapshot.captured(
            SourceSnapshotMetadata.fromRepositoryMetadata(metadata(), Optional.empty()),
            artifact("source.tar", "sha256:source-b"),
            List.of("src/main/java"),
            SourceSnapshotCompleteness.COMPLETE,
            List.of()
        );

        assertEquals(first.id(), second.id());
        assertNotEquals(first.id(), differentArtifact.id());
    }

    @Test
    void sourceSnapshotCopiesMutableInputCollections() {
        var sourceRoots = new ArrayList<String>();
        sourceRoots.add("src/main/java");
        var limitations = new ArrayList<String>();
        limitations.add("generated sources unavailable");

        var snapshot = SourceSnapshot.captured(
            SourceSnapshotMetadata.fromRepositoryMetadata(metadata(), Optional.empty()),
            artifact("source.tar", "sha256:source"),
            sourceRoots,
            SourceSnapshotCompleteness.INCOMPLETE,
            limitations
        );
        sourceRoots.add("generated");
        limitations.add("test sources unavailable");

        assertEquals(List.of("src/main/java"), snapshot.sourceRoots());
        assertEquals(List.of("generated sources unavailable"), snapshot.limitations());
    }

    @Test
    void incompleteSnapshotsRequireLimitations() {
        assertThrows(
            IllegalArgumentException.class,
            () -> SourceSnapshot.captured(
                SourceSnapshotMetadata.fromRepositoryMetadata(metadata(), Optional.empty()),
                artifact("source.tar", "sha256:source"),
                List.of("src/main/java"),
                SourceSnapshotCompleteness.INCOMPLETE,
                List.of()
            )
        );
    }

    @Test
    void sourceSnapshotRejectsMissingAndBlankValues() {
        var snapshotMetadata = SourceSnapshotMetadata.fromRepositoryMetadata(metadata(), Optional.empty());
        var artifact = artifact("source.tar", "sha256:source");

        assertThrows(IllegalArgumentException.class, () -> new SourceSnapshotId(null));
        assertThrows(IllegalArgumentException.class, () -> new SourceSnapshotId(" "));
        assertThrows(
            IllegalArgumentException.class,
            () -> new SourceSnapshotMetadata(" ", "file:///workspace/project", Optional.empty(), Optional.empty(), Optional.empty())
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new SourceSnapshotMetadata("project-a", " ", Optional.empty(), Optional.empty(), Optional.empty())
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new SourceSnapshotMetadata("project-a", "repo", Optional.of(" "), Optional.empty(), Optional.empty())
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new SourceSnapshotMetadata("project-a", "repo", Optional.empty(), Optional.of(" "), Optional.empty())
        );
        assertThrows(NullPointerException.class, () -> SourceSnapshotMetadata.fromRepositoryMetadata(null, Optional.empty()));
        assertThrows(NullPointerException.class, () -> SourceSnapshotMetadata.fromRepositoryMetadata(metadata(), null));
        assertThrows(
            NullPointerException.class,
            () -> SourceSnapshot.captured(null, artifact, List.of("src/main/java"), SourceSnapshotCompleteness.COMPLETE, List.of())
        );
        assertThrows(
            NullPointerException.class,
            () -> SourceSnapshot.captured(snapshotMetadata, null, List.of("src/main/java"), SourceSnapshotCompleteness.COMPLETE, List.of())
        );
        assertThrows(
            NullPointerException.class,
            () -> SourceSnapshot.captured(snapshotMetadata, artifact, null, SourceSnapshotCompleteness.COMPLETE, List.of())
        );
        assertThrows(
            NullPointerException.class,
            () -> SourceSnapshot.captured(snapshotMetadata, artifact, List.of("src/main/java"), null, List.of())
        );
        assertThrows(
            NullPointerException.class,
            () -> SourceSnapshot.captured(snapshotMetadata, artifact, List.of("src/main/java"), SourceSnapshotCompleteness.COMPLETE, null)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> SourceSnapshot.captured(snapshotMetadata, artifact, List.of(" "), SourceSnapshotCompleteness.COMPLETE, List.of())
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> SourceSnapshot.captured(snapshotMetadata, artifact, List.of("src/main/java"), SourceSnapshotCompleteness.INCOMPLETE, List.of(" "))
        );
    }

    private static RepositoryMetadata metadata() {
        return new RepositoryMetadata("project-a", "file:///workspace/project", "main", "abcdef");
    }

    private static ArtifactReference artifact(String path, String sha256) {
        return new ArtifactReference(path, "source-snapshot", sha256, 42L);
    }
}
