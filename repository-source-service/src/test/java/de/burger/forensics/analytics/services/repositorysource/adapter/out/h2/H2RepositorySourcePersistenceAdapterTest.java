package de.burger.forensics.analytics.services.repositorysource.adapter.out.h2;

import de.burger.forensics.analytics.services.repositorysource.application.port.RepositorySourceIdempotencyRecord;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.ArtifactByteAccess;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.ArtifactByteCustody;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.ArtifactReference;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.BuildOutputPackageDescriptor;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.BuildOutputProducer;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.BuildOutputProducerCandidate;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.BuildOutputProducerStatus;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.BuildOutputResolution;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.CheckoutResult;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.CheckoutStatus;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.Diagnostic;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.PackageAvailability;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryIdentity;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryKey;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryPreparation;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryReference;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspace;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceBranch;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceBranchStatus;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceStatus;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RevisionSelector;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.SourcePackageDescriptor;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.SourceRoot;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.SourceSnapshot;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.SourceSnapshotCompleteness;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.SourceSnapshotId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceBranchId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceTitle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class H2RepositorySourcePersistenceAdapterTest {
    private static final Instant NOW = Instant.parse("2026-05-24T08:30:00Z");
    private static final RepositoryReference REPOSITORY = new RepositoryReference(
        "https://example.com/acme/demo.git",
        "github",
        Map.of("tenant", "demo")
    );
    private static final RevisionSelector REVISION = new RevisionSelector("main", true, "a".repeat(40), true);
    private static final SourceRoot SOURCE_ROOT = new SourceRoot("src/main/java", "java");
    private static final SourceSnapshotId SNAPSHOT_ID = new SourceSnapshotId("source-snapshot-h2");
    private static final WorkspaceId WORKSPACE_ID = new WorkspaceId("workspace-h2");
    private static final WorkspaceBranchId BRANCH_ID = new WorkspaceBranchId("workspace-branch-h2");

    @TempDir
    Path tempDir;

    @Test
    void savesAndLoadsRepositorySourceStateAcrossReopen() {
        var first = adapter();
        first.save(workspace());
        first.save(preparation());
        first.save(new RepositorySourceIdempotencyRecord(
            "key-1",
            "CREATE_WORKSPACE",
            "fingerprint-1",
            "REPOSITORY_WORKSPACE",
            WORKSPACE_ID.value(),
            "payload-1",
            "COMPLETED",
            NOW,
            null
        ));

        var reopened = adapter();
        var loadedWorkspace = reopened.findByRepositoryKey(new RepositoryKey("example.com/acme/demo")).orElseThrow();
        var loadedBranch = reopened.findBranch(WORKSPACE_ID, BRANCH_ID).orElseThrow();
        var loadedPreparation = reopened.findByRunAndSnapshot(new AnalysisRunId("run-1"), SNAPSHOT_ID).orElseThrow();
        var loadedIdempotency = reopened.find("CREATE_WORKSPACE", "key-1").orElseThrow();

        assertEquals(WORKSPACE_ID, loadedWorkspace.workspaceId());
        assertEquals("demo", loadedWorkspace.workspaceTitle().value());
        assertEquals(List.of("src/main/java"), loadedBranch.sourceRoots().stream().map(SourceRoot::relativePath).toList());
        assertEquals(RepositoryWorkspaceBranchStatus.CHECKED_OUT, loadedBranch.status());
        assertEquals(SNAPSHOT_ID, loadedPreparation.sourceSnapshotId());
        assertEquals("github", loadedPreparation.repository().provider());
        assertTrue(loadedPreparation.requestedRevision().branchRequired());
        assertTrue(loadedPreparation.requestedRevision().commitRequired());
        assertEquals("a".repeat(40), loadedPreparation.requestedRevision().commit());
        assertEquals(List.of("src/main/java"), loadedPreparation.checkout().sourceRoots().stream().map(SourceRoot::relativePath).toList());
        assertEquals(List.of("GIT_CHECKOUT_COMPLETED"), loadedPreparation.checkout().diagnostics().stream().map(Diagnostic::code).toList());
        assertEquals(List.of("REPOSITORY_CHECKED_OUT"), loadedPreparation.diagnostics().stream().map(Diagnostic::code).toList());
        assertEquals(List.of("fixture limitation"), loadedPreparation.sourceSnapshot().limitations());
        assertEquals("source-package-custom-v1", loadedPreparation.sourceSnapshot().sourcePackage().schemaVersion());
        assertEquals(SourceSnapshotCompleteness.INCOMPLETE, loadedPreparation.sourceSnapshot().sourcePackage().completeness());
        assertEquals("custom-build-system", loadedPreparation.sourceSnapshot().buildOutputPackage().buildSystem());
        assertEquals("fingerprint-1", loadedIdempotency.fingerprint());
        assertEquals("payload-1", loadedIdempotency.resultPayload());
        assertTrue(loadedPreparation.toString().contains("workspace-h2"));
    }

    private H2RepositorySourcePersistenceAdapter adapter() {
        return new H2RepositorySourcePersistenceAdapter(
            "jdbc:h2:file:" + tempDir.resolve("repository-source").toAbsolutePath().normalize() + ";AUTO_SERVER=FALSE;DB_CLOSE_DELAY=-1",
            "sa",
            ""
        );
    }

    private static RepositoryWorkspace workspace() {
        return new RepositoryWorkspace(
            WORKSPACE_ID,
            new WorkspaceTitle("demo"),
            RepositoryIdentity.from(REPOSITORY, "main"),
            RepositoryWorkspaceStatus.READY,
            NOW,
            NOW,
            List.of(branch()),
            List.of(Diagnostic.info("READY", "Repository workspace is ready")),
            Map.of("tenant", "demo")
        );
    }

    private static RepositoryWorkspaceBranch branch() {
        return new RepositoryWorkspaceBranch(
            BRANCH_ID,
            WORKSPACE_ID,
            "main",
            "",
            "b".repeat(40),
            SNAPSHOT_ID,
            RepositoryWorkspaceBranchStatus.CHECKED_OUT,
            List.of(SOURCE_ROOT),
            NOW,
            NOW,
            List.of(Diagnostic.info("CHECKED_OUT", "Repository checkout completed"))
        );
    }

    private static RepositoryPreparation preparation() {
        var checkout = checkout();
        return new RepositoryPreparation(
            new AnalysisRunId("run-1"),
            SNAPSHOT_ID,
            WORKSPACE_ID,
            REPOSITORY,
            REVISION,
            checkout,
            snapshot(checkout),
            RepositoryWorkspaceStatus.CHECKED_OUT,
            NOW,
            NOW,
            List.of(Diagnostic.info("REPOSITORY_CHECKED_OUT", "Repository checkout completed")),
            Map.of("tenant", "demo")
        );
    }

    private static CheckoutResult checkout() {
        return new CheckoutResult(
            CheckoutStatus.CHECKED_OUT,
            REPOSITORY.remoteUrl(),
            "b".repeat(40),
            "main",
            "a".repeat(40),
            true,
            15,
            List.of(Diagnostic.info("GIT_CHECKOUT_COMPLETED", "Repository checkout completed")),
            false,
            false,
            List.of(SOURCE_ROOT)
        );
    }

    private static SourceSnapshot snapshot(CheckoutResult checkout) {
        var manifest = new ArtifactReference("snapshots/source-snapshot-h2/manifest.json", "application/json", "a".repeat(64), 100);
        return new SourceSnapshot(
            SNAPSHOT_ID,
            SourceSnapshotCompleteness.COMPLETE,
            checkout.sourceRoots(),
            manifest,
            List.of("fixture limitation"),
            new SourcePackageDescriptor(
                PackageAvailability.PENDING,
                manifest,
                null,
                "source-package-custom-v1",
                "repository-source-service",
                new ArtifactByteAccess(
                    "repository-source-service",
                    "repository-source.v1.SourcePackage",
                    "source-snapshot/source-snapshot-h2",
                    ArtifactByteCustody.PRODUCER_RETAINED
                ),
                SourceSnapshotCompleteness.INCOMPLETE
            ),
            new BuildOutputPackageDescriptor(
                PackageAvailability.PENDING,
                null,
                null,
                "build-output-package-descriptor-v1",
                "build-artifact-worker-service",
                new ArtifactByteAccess(
                    "build-artifact-worker-service",
                    "build-artifact-worker.v1.BuildOutputPackage",
                    "source-snapshot/source-snapshot-h2",
                    ArtifactByteCustody.PRODUCER_RETAINED
                ),
                SourceSnapshotCompleteness.UNKNOWN,
                new BuildOutputResolution(
                    List.of(
                        new BuildOutputProducerCandidate(BuildOutputProducer.ARTIFACT_STORE, BuildOutputProducerStatus.NOT_CONFIGURED, "", List.of()),
                        new BuildOutputProducerCandidate(BuildOutputProducer.ARTIFACTORY, BuildOutputProducerStatus.NOT_CONFIGURED, "", List.of()),
                        new BuildOutputProducerCandidate(BuildOutputProducer.JENKINS, BuildOutputProducerStatus.NOT_CONFIGURED, "", List.of()),
                        new BuildOutputProducerCandidate(
                            BuildOutputProducer.BUILD_ARTIFACT_WORKER,
                            BuildOutputProducerStatus.FALLBACK_PLANNED,
                            "source-snapshot/source-snapshot-h2",
                            List.of(Diagnostic.info("BUILD_ARTIFACT_WORKER_FALLBACK_PLANNED", "Build artifact worker fallback is planned"))
                        )
                    ),
                    BuildOutputProducer.UNSPECIFIED,
                    false,
                    List.of()
                ),
                "custom-build-system"
            )
        );
    }
}
