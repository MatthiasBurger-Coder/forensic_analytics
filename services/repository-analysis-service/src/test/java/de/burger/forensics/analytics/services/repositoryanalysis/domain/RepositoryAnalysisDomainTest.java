package de.burger.forensics.analytics.services.repositoryanalysis.domain;

import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.ArtifactReference;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.CheckoutResult;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.CheckoutStatus;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.Diagnostic;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.RepositoryPreparation;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.RepositoryReference;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.RepositoryWorkspaceStatus;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.RevisionSelector;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceRoot;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceSnapshot;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceSnapshotCompleteness;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceSnapshotId;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.WorkspaceId;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.WorkspacePolicy;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositoryAnalysisDomainTest {
    private static final String SHA = "a".repeat(64);
    private static final RepositoryReference REPOSITORY = new RepositoryReference(
        "https://example.com/acme/demo.git",
        "github",
        Map.of("tenant", "demo")
    );
    private static final RevisionSelector REVISION = new RevisionSelector("main", true, "", false);
    private static final SourceRoot SOURCE_ROOT = new SourceRoot("src/main/java", "java");

    @Test
    void acceptsCleanRepositoryPreparationAndDeterministicSnapshotIds() {
        var checkout = checkout();
        var snapshotId = SourceSnapshotId.deterministic(REPOSITORY, REVISION, checkout.resolvedCommit(), SHA);
        var snapshot = new SourceSnapshot(
            snapshotId,
            SourceSnapshotCompleteness.COMPLETE,
            List.of(SOURCE_ROOT),
            new ArtifactReference("snapshots/demo/manifest.json", "application/json", SHA, 100),
            List.of()
        );
        var preparation = new RepositoryPreparation(
            new AnalysisRunId("run-1"),
            snapshotId,
            new WorkspaceId("workspace-run-1"),
            REPOSITORY,
            REVISION,
            checkout,
            snapshot,
            RepositoryWorkspaceStatus.CHECKED_OUT,
            Instant.parse("2026-05-16T10:15:30Z"),
            Instant.parse("2026-05-16T10:15:31Z"),
            List.of(Diagnostic.info("OK", "done")),
            Map.of("request", "demo")
        );

        assertTrue(snapshotId.value().startsWith("source-snapshot-"));
        assertEquals(snapshotId, preparation.sourceSnapshotId());
        assertEquals(RepositoryWorkspaceStatus.CLEANED, preparation
            .withWorkspaceStatus(RepositoryWorkspaceStatus.CLEANED, Instant.parse("2026-05-16T10:15:32Z"))
            .workspaceStatus());
    }

    @Test
    void rejectsUnsafeRepositoryReferencesAndSafeAttributes() {
        assertThrows(IllegalArgumentException.class, () -> new RepositoryReference("http://example.com/repo.git", "", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryReference("https://user@example.com/repo.git", "", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryReference("https://example.com/repo.git?token=x", "", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryReference("https://localhost/repo.git", "", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryReference("https://0.0.0.0/repo.git", "", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryReference("https://127.0.0.1/repo.git", "", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryReference("https://[::1]/repo.git", "", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryReference("https://[::ffff:127.0.0.1]/repo.git", "", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryReference("https://[::ffff:10.0.0.1]/repo.git", "", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryReference("https://[fc00::1]/repo.git", "", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryReference("https://[fd00::1]/repo.git", "", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryReference("https://[fe80::1]/repo.git", "", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryReference("https://example.com/repo.git", "", Map.of("token", "x")));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryReference("https://example.com/repo.git", "", Map.of("path", "/home/user/repo")));
    }

    @Test
    void rejectsUnsafeRevisionAndWorkspacePolicies() {
        assertEquals("abcdef1", new RevisionSelector("", false, "ABCDEF1", true).commit());
        assertThrows(IllegalArgumentException.class, () -> new RevisionSelector("", false, "", false));
        assertThrows(IllegalArgumentException.class, () -> new RevisionSelector("", true, "abcdef1", false));
        assertThrows(IllegalArgumentException.class, () -> new RevisionSelector("main", false, "", true));
        assertThrows(IllegalArgumentException.class, () -> new RevisionSelector("-main", false, "", false));
        assertThrows(IllegalArgumentException.class, () -> new RevisionSelector("main..other", false, "", false));
        assertThrows(IllegalArgumentException.class, () -> new RevisionSelector("", false, "not-a-commit", true));
        assertThrows(IllegalArgumentException.class, () -> new WorkspacePolicy(true, false, true, false, 60, 100));
        assertThrows(IllegalArgumentException.class, () -> new WorkspacePolicy(true, false, false, true, 60, 100));
        assertThrows(IllegalArgumentException.class, () -> new WorkspacePolicy(true, false, false, false, 0, 100));
        assertThrows(IllegalArgumentException.class, () -> new WorkspacePolicy(true, false, false, false, 3_601, 100));
        assertThrows(IllegalArgumentException.class, () -> new WorkspacePolicy(true, false, false, false, 60, 0));
        assertThrows(IllegalArgumentException.class, () -> new WorkspacePolicy(true, false, false, false, 60, 107_374_182_401L));
        assertEquals("", new RevisionSelector(null, false, "abcdef1", true).branch());
    }

    @Test
    void rejectsPrivatePathsAndInvalidEvidenceReferences() {
        assertThrows(IllegalArgumentException.class, () -> new WorkspaceId("/tmp/workspace"));
        assertThrows(IllegalArgumentException.class, () -> new SourceRoot("/src/main/java", "java"));
        assertThrows(IllegalArgumentException.class, () -> new SourceRoot("../src/main/java", "java"));
        assertThrows(IllegalArgumentException.class, () -> new SourceRoot("src\\main\\java", "java"));
        assertThrows(IllegalArgumentException.class, () -> new ArtifactReference("file:/tmp/manifest.json", "application/json", SHA, 1));
        assertThrows(IllegalArgumentException.class, () -> new ArtifactReference("manifest\n.json", "application/json", SHA, 1));
        assertThrows(IllegalArgumentException.class, () -> new ArtifactReference("C:/tmp/manifest.json", "application/json", SHA, 1));
        assertThrows(IllegalArgumentException.class, () -> new ArtifactReference("manifest.json", "application/json", "bad", 1));
        assertThrows(IllegalArgumentException.class, () -> new ArtifactReference("manifest.json", "application/json", SHA, -1));
        assertThrows(IllegalArgumentException.class, () -> new SourceSnapshot(
            new SourceSnapshotId("source-snapshot-1"),
            SourceSnapshotCompleteness.COMPLETE,
            List.of(),
            new ArtifactReference("manifest.json", "application/json", SHA, 1),
            List.of()
        ));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryPreparation(
            new AnalysisRunId("run-1"),
            new SourceSnapshotId("source-snapshot-a"),
            new WorkspaceId("workspace-1"),
            REPOSITORY,
            REVISION,
            checkout(),
            new SourceSnapshot(
                new SourceSnapshotId("source-snapshot-b"),
                SourceSnapshotCompleteness.COMPLETE,
                List.of(SOURCE_ROOT),
                new ArtifactReference("manifest.json", "application/json", SHA, 1),
                List.of()
            ),
            RepositoryWorkspaceStatus.CHECKED_OUT,
            Instant.EPOCH,
            Instant.EPOCH,
            List.of(),
            Map.of()
        ));
    }

    private static CheckoutResult checkout() {
        return new CheckoutResult(
            CheckoutStatus.CHECKED_OUT,
            REPOSITORY.remoteUrl(),
            "b".repeat(40),
            "main",
            "",
            true,
            1,
            List.of(),
            false,
            false,
            List.of(SOURCE_ROOT)
        );
    }

    @Test
    void rejectsIncompleteCheckoutMetadataAndUnsafeAttributes() {
        assertEquals(RepositoryAnalysisDomain.DiagnosticSeverity.INFO, new RepositoryAnalysisDomain.Diagnostic(
            "CODE",
            "message",
            null
        ).severity());
        assertEquals(RepositoryAnalysisDomain.DiagnosticSeverity.ERROR, RepositoryAnalysisDomain.Diagnostic
            .error("ERROR", "message")
            .severity());
        assertThrows(IllegalArgumentException.class, () -> RepositoryAnalysisDomain.requireText(null, "value"));
        assertThrows(IllegalArgumentException.class, () -> RepositoryAnalysisDomain.safeAttributes(Map.of("note", "secret-value")));
        assertThrows(IllegalArgumentException.class, () -> RepositoryAnalysisDomain.safeAttributes(Map.of("note", "C:/Users/demo")));
        assertThrows(IllegalArgumentException.class, () -> new CheckoutResult(
            CheckoutStatus.CHECKED_OUT,
            REPOSITORY.remoteUrl(),
            "b".repeat(40),
            "",
            "",
            false,
            -1,
            List.of(),
            false,
            false,
            List.of(SOURCE_ROOT)
        ));
        assertThrows(IllegalArgumentException.class, () -> new CheckoutResult(
            CheckoutStatus.CHECKED_OUT,
            REPOSITORY.remoteUrl(),
            "b".repeat(40),
            "",
            "",
            false,
            0,
            null,
            false,
            false,
            List.of()
        ));
    }
}
