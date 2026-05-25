package de.burger.forensics.analytics.services.repositorysource.application;

import de.burger.forensics.analytics.services.repositorysource.adapter.out.h2.H2RepositorySourcePersistenceAdapter;
import de.burger.forensics.analytics.services.repositorysource.application.port.PreparedWorkspace;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryCheckoutPort;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryMetadataPort;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryMetadataPreviewPolicy;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryMetadataResolution;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositorySourceIdempotencyRecord;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryWorkspaceIdGenerator;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryWorkspacePort;
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
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceBranchSelector;
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
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspacePolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositorySourceH2PersistenceApplicationTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-24T09:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path tempDir;

    @Test
    void prepareReplaySurvivesServiceReinstantiationAndConflictPreventsSideEffects() {
        var firstPorts = new FakeWorkspacePort();
        var firstCheckout = new SequencedCheckoutPort("b".repeat(40));
        var prepared = preparationService(adapter(), firstPorts, firstCheckout).prepare(
            "prepare-key",
            "schema-v1",
            "correlation-1",
            new AnalysisRunId("run-1"),
            repository(),
            revision(),
            policy(),
            Map.of("tenant", "demo")
        );
        var replayPorts = new FakeWorkspacePort();
        var replayCheckout = new SequencedCheckoutPort("c".repeat(40));
        var replayService = preparationService(adapter(), replayPorts, replayCheckout);

        var replayed = replayService.prepare(
            "prepare-key",
            "schema-v1",
            "correlation-1",
            new AnalysisRunId("run-1"),
            repository(),
            revision(),
            policy(),
            Map.of("tenant", "demo")
        );

        assertEquals(prepared.sourceSnapshotId(), replayed.sourceSnapshotId());
        assertEquals(0, replayPorts.prepared);
        assertEquals(0, replayCheckout.calls);
        var cleaned = replayService.cleanup("cleanup-key", "correlation-1", new AnalysisRunId("run-1"), prepared.workspaceId());
        var replayedAfterCleanup = replayService.prepare(
            "prepare-key",
            "schema-v1",
            "correlation-1",
            new AnalysisRunId("run-1"),
            repository(),
            revision(),
            policy(),
            Map.of("tenant", "demo")
        );
        assertEquals(RepositoryWorkspaceStatus.CLEANED, cleaned.workspaceStatus());
        assertEquals(RepositoryWorkspaceStatus.CHECKED_OUT, replayedAfterCleanup.workspaceStatus());
        assertThrows(IdempotencyConflictException.class, () -> replayService.prepare(
            "prepare-key",
            "schema-v2",
            "correlation-1",
            new AnalysisRunId("run-1"),
            repository(),
            revision(),
            policy(),
            Map.of("tenant", "demo")
        ));
        assertEquals(0, replayPorts.prepared);
        assertEquals(0, replayCheckout.calls);
    }

    @Test
    void sameIdempotencyKeyCanBeUsedAcrossPrepareAndWorkspaceCheckoutOperations() {
        var adapter = adapter();
        preparationService(adapter, new FakeWorkspacePort(), new SequencedCheckoutPort("b".repeat(40))).prepare(
            "shared-key",
            "schema-v1",
            "correlation-1",
            new AnalysisRunId("run-1"),
            repository(),
            revision(),
            policy(),
            Map.of()
        );

        var workspace = workspaceService(adapter, new FakeWorkspacePort(), new SequencedCheckoutPort("c".repeat(40)))
            .createOrReuseRepositoryWorkspaceWithCheckout(
                "shared-key",
                "schema-v1",
                "correlation-1",
                repository(),
                new RepositoryWorkspaceBranchSelector("main", ""),
                policy(),
                Map.of()
            );

        assertEquals(new WorkspaceId("workspace-0001"), workspace.workspaceId());
        assertEquals("c".repeat(40), workspace.branches().getFirst().resolvedCommit());
    }

    @Test
    void workspaceCheckoutAndRefreshReplaySurviveServiceReinstantiation() {
        var firstWorkspacePort = new FakeWorkspacePort();
        var firstCheckout = new SequencedCheckoutPort("b".repeat(40), "b".repeat(40));
        var workspace = workspaceService(adapter(), firstWorkspacePort, firstCheckout).createOrReuseRepositoryWorkspaceWithCheckout(
            "checkout-key",
            "schema-v1",
            "correlation-1",
            repository(),
            new RepositoryWorkspaceBranchSelector("main", ""),
            policy(),
            Map.of("tenant", "demo")
        );
        var branch = workspace.branches().getFirst();
        var refreshed = workspaceService(adapter(), firstWorkspacePort, firstCheckout).refreshRepositoryWorkspaceBranch(
            "refresh-key",
            "schema-v1",
            "correlation-1",
            workspace.workspaceId(),
            branch.workspaceBranchId(),
            policy(),
            Map.of()
        );

        var replayWorkspacePort = new FakeWorkspacePort();
        var replayCheckout = new SequencedCheckoutPort("c".repeat(40));
        var replayService = workspaceService(adapter(), replayWorkspacePort, replayCheckout);
        var replayedWorkspace = replayService.createOrReuseRepositoryWorkspaceWithCheckout(
            "checkout-key",
            "schema-v1",
            "correlation-1",
            repository(),
            new RepositoryWorkspaceBranchSelector("main", ""),
            policy(),
            Map.of("tenant", "demo")
        );
        var replayedRefresh = replayService.refreshRepositoryWorkspaceBranch(
            "refresh-key",
            "schema-v1",
            "correlation-1",
            workspace.workspaceId(),
            branch.workspaceBranchId(),
            policy(),
            Map.of()
        );

        assertEquals(workspace.workspaceId(), replayedWorkspace.workspaceId());
        assertFalse(refreshed.changed());
        assertEquals(refreshed.changed(), replayedRefresh.changed());
        assertEquals(0, replayWorkspacePort.branchCheckouts);
        assertEquals(0, replayCheckout.calls);
        assertThrows(IdempotencyConflictException.class, () -> replayService.refreshRepositoryWorkspaceBranch(
            "refresh-key",
            "schema-v1",
            "correlation-1",
            workspace.workspaceId(),
            branch.workspaceBranchId(),
            new WorkspacePolicy(true, true, false, false, 30, 100_000),
            Map.of()
        ));
        assertEquals(0, replayWorkspacePort.branchCheckouts);
        assertEquals(0, replayCheckout.calls);
    }

    @Test
    void workspaceCheckoutReplaySurvivesH2ShutdownAndReopen() throws Exception {
        var firstWorkspacePort = new FakeWorkspacePort();
        var firstCheckout = new SequencedCheckoutPort("b".repeat(40));
        var workspace = workspaceService(adapter(), firstWorkspacePort, firstCheckout).createOrReuseRepositoryWorkspaceWithCheckout(
            "checkout-key",
            "schema-v1",
            "correlation-1",
            repository(),
            new RepositoryWorkspaceBranchSelector("main", ""),
            policy(),
            Map.of("tenant", "demo")
        );

        shutdownH2Database();

        var replayWorkspacePort = new FakeWorkspacePort();
        var replayCheckout = new SequencedCheckoutPort("c".repeat(40));
        var replayService = workspaceService(adapter(), replayWorkspacePort, replayCheckout);
        var loaded = replayService.getRepositoryWorkspace(workspace.workspaceId());
        var replayed = replayService.createOrReuseRepositoryWorkspaceWithCheckout(
            "checkout-key",
            "schema-v1",
            "correlation-1",
            repository(),
            new RepositoryWorkspaceBranchSelector("main", ""),
            policy(),
            Map.of("tenant", "demo")
        );

        assertEquals(workspace, loaded);
        assertEquals(workspace, replayed);
        assertEquals(0, replayWorkspacePort.branchCheckouts);
        assertEquals(0, replayCheckout.calls);
    }

    @Test
    void workspaceListOrderingAndCleanedExclusionSurviveH2ShutdownAndReopen() throws Exception {
        var firstWorkspacePort = new FakeWorkspacePort();
        var firstService = workspaceService(
            adapter(),
            new SequentialRepositoryWorkspaceIdGenerator(),
            firstWorkspacePort,
            new SequencedCheckoutPort("b".repeat(40), "c".repeat(40))
        );
        var beta = firstService.createOrReuseRepositoryWorkspaceWithCheckout(
            "checkout-beta",
            "schema-v1",
            "correlation-1",
            repository("beta"),
            new RepositoryWorkspaceBranchSelector("main", ""),
            policy(),
            Map.of("tenant", "demo")
        );
        var alpha = firstService.createOrReuseRepositoryWorkspaceWithCheckout(
            "checkout-alpha",
            "schema-v1",
            "correlation-1",
            repository("alpha"),
            new RepositoryWorkspaceBranchSelector("main", ""),
            policy(),
            Map.of("tenant", "demo")
        );
        var cleaned = firstService.cleanupRepositoryWorkspaceById(
            "cleanup-beta",
            "schema-v1",
            "correlation-1",
            beta.workspaceId(),
            Map.of("tenant", "demo")
        );

        shutdownH2Database();

        var replayWorkspacePort = new FakeWorkspacePort();
        var replayService = workspaceService(adapter(), replayWorkspacePort, new SequencedCheckoutPort("d".repeat(40)));
        var visible = replayService.listRepositoryWorkspaces("schema-v1", "correlation-1", false);
        var all = replayService.listRepositoryWorkspaces("schema-v1", "correlation-1", true);
        var loadedCleaned = replayService.getRepositoryWorkspace(beta.workspaceId());

        assertEquals(RepositoryWorkspaceStatus.CLEANED, cleaned.workspaceStatus());
        assertEquals(List.of(alpha.workspaceId().value()), workspaceIds(visible));
        assertEquals(List.of(beta.workspaceId().value(), alpha.workspaceId().value()), workspaceIds(all));
        assertEquals(RepositoryWorkspaceStatus.CLEANED, loadedCleaned.status());
        assertEquals(beta.workspaceTitle(), loadedCleaned.workspaceTitle());
        assertEquals(beta.repository(), loadedCleaned.repository());
        assertEquals(beta.branches().getFirst().workspaceBranchId(), loadedCleaned.branches().getFirst().workspaceBranchId());
        assertEquals(beta.branches().getFirst().sourceSnapshotId(), loadedCleaned.branches().getFirst().sourceSnapshotId());
        assertEquals(1, firstWorkspacePort.cleaned);
        assertEquals(0, replayWorkspacePort.cleaned);
    }

    @Test
    void createCheckoutConflictAfterH2ShutdownDoesNotMutatePersistedWorkspaceOrBranchState() throws Exception {
        workspaceService(adapter(), new FakeWorkspacePort(), new SequencedCheckoutPort("b".repeat(40)))
            .createOrReuseRepositoryWorkspaceWithCheckout(
                "checkout-key",
                "schema-v1",
                "correlation-1",
                repository(),
                new RepositoryWorkspaceBranchSelector("main", ""),
                policy(),
                Map.of("tenant", "demo")
            );
        var before = adapter().findByRepositoryKey(RepositoryIdentity.from(repository(), "main").repositoryKey()).orElseThrow();

        shutdownH2Database();

        var replayWorkspacePort = new FakeWorkspacePort();
        var replayCheckout = new SequencedCheckoutPort("c".repeat(40));
        var replayService = workspaceService(adapter(), replayWorkspacePort, replayCheckout);

        assertThrows(IdempotencyConflictException.class, () -> replayService.createOrReuseRepositoryWorkspaceWithCheckout(
            "checkout-key",
            "schema-v1",
            "correlation-1",
            repository(),
            new RepositoryWorkspaceBranchSelector("main", ""),
            policy(),
            Map.of("tenant", "other")
        ));
        var after = adapter().findByRepositoryKey(RepositoryIdentity.from(repository(), "main").repositoryKey()).orElseThrow();
        assertEquals(before, after);
        assertEquals(before.branches().getFirst(), after.branches().getFirst());
        assertEquals(0, replayWorkspacePort.branchCheckouts);
        assertEquals(0, replayCheckout.calls);
    }

    @Test
    void sameRepositoryAndBranchReuseWorkspaceAcrossH2ShutdownWithNewIdempotencyKey() throws Exception {
        var workspace = workspaceService(adapter(), new FakeWorkspacePort(), new SequencedCheckoutPort("b".repeat(40)))
            .createOrReuseRepositoryWorkspaceWithCheckout(
                "checkout-key",
                "schema-v1",
                "correlation-1",
                repository(),
                new RepositoryWorkspaceBranchSelector("main", ""),
                policy(),
                Map.of("tenant", "demo")
            );
        var branch = workspace.branches().getFirst();

        shutdownH2Database();

        var replayWorkspacePort = new FakeWorkspacePort();
        var replayCheckout = new SequencedCheckoutPort("c".repeat(40));
        var reused = workspaceService(
            adapter(),
            new FailingRepositoryWorkspaceIdGenerator(),
            replayWorkspacePort,
            replayCheckout
        ).createOrReuseRepositoryWorkspaceWithCheckout(
            "checkout-key-after-reopen",
            "schema-v1",
            "correlation-1",
            repository(),
            new RepositoryWorkspaceBranchSelector("main", ""),
            policy(),
            Map.of("tenant", "demo")
        );

        assertEquals(workspace.workspaceId(), reused.workspaceId());
        assertEquals(1, reused.branches().size());
        assertEquals(branch.workspaceBranchId(), reused.branches().getFirst().workspaceBranchId());
        assertEquals(branch.sourceSnapshotId(), reused.branches().getFirst().sourceSnapshotId());
        assertEquals(0, replayWorkspacePort.branchCheckouts);
        assertEquals(0, replayCheckout.calls);
    }

    @Test
    void updatedRefreshReplaySurvivesLaterBranchMutationAndServiceReinstantiation() {
        var firstWorkspacePort = new FakeWorkspacePort();
        var firstCheckout = new SequencedCheckoutPort("b".repeat(40), "c".repeat(40), "d".repeat(40));
        var firstService = workspaceService(adapter(), firstWorkspacePort, firstCheckout);
        var workspace = firstService.createOrReuseRepositoryWorkspaceWithCheckout(
            "checkout-key",
            "schema-v1",
            "correlation-1",
            repository(),
            new RepositoryWorkspaceBranchSelector("main", ""),
            policy(),
            Map.of()
        );
        var checkedOut = workspace.branches().getFirst();
        var firstRefresh = firstService.refreshRepositoryWorkspaceBranch(
            "refresh-to-c",
            "schema-v1",
            "correlation-1",
            workspace.workspaceId(),
            checkedOut.workspaceBranchId(),
            policy(),
            Map.of()
        );
        firstService.refreshRepositoryWorkspaceBranch(
            "refresh-to-d",
            "schema-v1",
            "correlation-1",
            workspace.workspaceId(),
            checkedOut.workspaceBranchId(),
            policy(),
            Map.of()
        );

        var replayWorkspacePort = new FakeWorkspacePort();
        var replayCheckout = new SequencedCheckoutPort("e".repeat(40));
        var replayedRefresh = workspaceService(adapter(), replayWorkspacePort, replayCheckout).refreshRepositoryWorkspaceBranch(
            "refresh-to-c",
            "schema-v1",
            "correlation-1",
            workspace.workspaceId(),
            checkedOut.workspaceBranchId(),
            policy(),
            Map.of()
        );

        assertTrue(firstRefresh.changed());
        assertEquals("c".repeat(40), firstRefresh.branch().resolvedCommit());
        assertEquals(firstRefresh.changed(), replayedRefresh.changed());
        assertEquals(firstRefresh.previousCommit(), replayedRefresh.previousCommit());
        assertEquals(firstRefresh.previousSourceSnapshotId(), replayedRefresh.previousSourceSnapshotId());
        assertEquals(firstRefresh.branch(), replayedRefresh.branch());
        assertEquals(0, replayWorkspacePort.branchCheckouts);
        assertEquals(0, replayCheckout.calls);
    }

    @Test
    void emptyLookupsAndExpiringIdempotencyRecordsRoundTrip() {
        var adapter = adapter();
        var expiresAt = Instant.parse("2026-05-24T10:00:00Z");

        assertTrue(adapter.findByRunAndSnapshot(new AnalysisRunId("missing-run"), new SourceSnapshotId("source-snapshot-missing")).isEmpty());
        assertTrue(adapter.findByRunAndWorkspace(new AnalysisRunId("missing-run"), new WorkspaceId("workspace-missing")).isEmpty());
        assertTrue(adapter.findById(new WorkspaceId("workspace-missing")).isEmpty());
        assertTrue(adapter.findByRepositoryKey(new RepositoryKey("example.com/acme/missing")).isEmpty());
        assertTrue(adapter.findBranch(new WorkspaceId("workspace-missing"), new WorkspaceBranchId("workspace-branch-missing")).isEmpty());
        assertTrue(adapter.find("CREATE_WORKSPACE", "missing-key").isEmpty());

        adapter.save(new RepositorySourceIdempotencyRecord(
            "idem-expiring",
            "CREATE_WORKSPACE",
            "fingerprint",
            "REPOSITORY_WORKSPACE",
            "workspace-0001",
            "",
            "COMPLETED",
            CLOCK.instant(),
            expiresAt
        ));

        var loaded = adapter.find("CREATE_WORKSPACE", "idem-expiring").orElseThrow();
        assertEquals(expiresAt, loaded.expiresAt());
        assertEquals("", loaded.resultPayload());
    }

    @Test
    void workspaceBranchBeforeCheckoutPersistsNullableSnapshotAndLastCheckedAt() {
        var adapter = adapter();
        var service = workspaceService(adapter, new FakeWorkspacePort(), new SequencedCheckoutPort("b".repeat(40)));
        var workspace = service.createOrReuseRepositoryWorkspace("workspace-key", RepositoryIdentity.from(repository(), "main"), Map.of());
        var branch = service.createOrReuseRepositoryWorkspaceBranch(
            "branch-key",
            workspace.workspaceId(),
            new RepositoryWorkspaceBranchSelector("main", "")
        );

        var loaded = adapter.findBranch(workspace.workspaceId(), branch.workspaceBranchId()).orElseThrow();

        assertEquals(RepositoryWorkspaceBranchStatus.CHECKING_OUT, loaded.status());
        assertEquals(null, loaded.sourceSnapshotId());
        assertEquals(null, loaded.lastCheckedAt());
        assertEquals(List.of(), loaded.sourceRoots());
    }

    @Test
    void legacyPreparationRowsWithoutSnapshotJsonRebuildSnapshotDescriptor() throws Exception {
        var adapter = adapter();
        var prepared = preparationService(adapter, new FakeWorkspacePort(), new SequencedCheckoutPort("b".repeat(40))).prepare(
            "prepare-key",
            "schema-v1",
            "correlation-1",
            new AnalysisRunId("run-1"),
            repository(),
            revision(),
            policy(),
            Map.of()
        );

        clearSourceSnapshotJson(prepared.analysisRunId(), prepared.sourceSnapshotId());

        var loaded = adapter().findByRunAndSnapshot(prepared.analysisRunId(), prepared.sourceSnapshotId()).orElseThrow();
        assertEquals(prepared.sourceSnapshotId(), loaded.sourceSnapshot().sourceSnapshotId());
        assertEquals(
            "snapshots/" + prepared.sourceSnapshotId().value() + "/manifest.json",
            loaded.sourceSnapshot().manifestArtifact().reference()
        );
        assertEquals("repository-source-service", loaded.sourceSnapshot().sourcePackage().producerService());
        assertEquals("build-artifact-worker-service", loaded.sourceSnapshot().buildOutputPackage().producerService());
    }

    @Test
    void sourceSnapshotPackageArtifactsRoundTripThroughH2SnapshotJson() {
        var adapter = adapter();
        var sourceSnapshotId = new SourceSnapshotId("source-snapshot-artifacts");
        var preparation = new RepositoryPreparation(
            new AnalysisRunId("run-artifacts"),
            sourceSnapshotId,
            new WorkspaceId("workspace-artifacts"),
            repository(),
            revision(),
            checkoutResult(),
            sourceSnapshotWithPackageArtifacts(sourceSnapshotId),
            RepositoryWorkspaceStatus.CHECKED_OUT,
            CLOCK.instant(),
            CLOCK.instant(),
            List.of(Diagnostic.info("CHECKED_OUT", "Repository checkout completed")),
            Map.of("tenant", "demo")
        );

        adapter.save(preparation);

        var loaded = adapter.findByRunAndSnapshot(preparation.analysisRunId(), sourceSnapshotId).orElseThrow();
        assertEquals(
            "snapshots/source-snapshot-artifacts/source-package.zip",
            loaded.sourceSnapshot().sourcePackage().packageArtifact().reference()
        );
        assertEquals(
            "snapshots/source-snapshot-artifacts/build-output-manifest.json",
            loaded.sourceSnapshot().buildOutputPackage().manifestArtifact().reference()
        );
        assertEquals(
            "snapshots/source-snapshot-artifacts/build-output.zip",
            loaded.sourceSnapshot().buildOutputPackage().packageArtifact().reference()
        );
        assertEquals(BuildOutputProducer.BUILD_ARTIFACT_WORKER, loaded.sourceSnapshot().buildOutputPackage().resolution().selectedProducer());
    }

    private RepositorySourceApplicationService preparationService(
        H2RepositorySourcePersistenceAdapter adapter,
        FakeWorkspacePort workspacePort,
        RepositoryCheckoutPort checkoutPort
    ) {
        return new RepositorySourceApplicationService(adapter, adapter, workspacePort, checkoutPort, CLOCK);
    }

    private RepositoryWorkspaceApplicationService workspaceService(
        H2RepositorySourcePersistenceAdapter adapter,
        FakeWorkspacePort workspacePort,
        RepositoryCheckoutPort checkoutPort
    ) {
        return workspaceService(adapter, new FixedRepositoryWorkspaceIdGenerator(), workspacePort, checkoutPort);
    }

    private RepositoryWorkspaceApplicationService workspaceService(
        H2RepositorySourcePersistenceAdapter adapter,
        RepositoryWorkspaceIdGenerator idGenerator,
        FakeWorkspacePort workspacePort,
        RepositoryCheckoutPort checkoutPort
    ) {
        return new RepositoryWorkspaceApplicationService(
            adapter,
            idGenerator,
            adapter,
            workspacePort,
            checkoutPort,
            new FakeMetadataPort(),
            CLOCK
        );
    }

    private H2RepositorySourcePersistenceAdapter adapter() {
        return new H2RepositorySourcePersistenceAdapter(h2JdbcUrl(), "sa", "");
    }

    private String h2JdbcUrl() {
        return "jdbc:h2:file:" + tempDir.resolve("repository-source-application").toAbsolutePath().normalize()
            + ";AUTO_SERVER=FALSE;DB_CLOSE_DELAY=-1";
    }

    private void shutdownH2Database() throws Exception {
        try (var connection = DriverManager.getConnection(h2JdbcUrl(), "sa", "");
             var statement = connection.createStatement()) {
            statement.execute("SHUTDOWN");
        }
    }

    private void clearSourceSnapshotJson(AnalysisRunId analysisRunId, SourceSnapshotId sourceSnapshotId) throws Exception {
        try (var connection = DriverManager.getConnection(h2JdbcUrl(), "sa", "");
             var statement = connection.prepareStatement("""
                 UPDATE repository_preparation
                 SET source_snapshot_json = ''
                 WHERE analysis_run_id = ? AND source_snapshot_id = ?
                 """)) {
            statement.setString(1, analysisRunId.value());
            statement.setString(2, sourceSnapshotId.value());
            statement.executeUpdate();
        }
    }

    private static final class FailingRepositoryWorkspaceIdGenerator implements RepositoryWorkspaceIdGenerator {
        @Override
        public WorkspaceId newWorkspaceId() {
            throw new AssertionError("existing repository workspace should be reused after H2 reopen");
        }

        @Override
        public WorkspaceBranchId newWorkspaceBranchId() {
            throw new AssertionError("existing repository workspace branch should be reused after H2 reopen");
        }
    }

    private static RepositoryReference repository() {
        return new RepositoryReference("https://example.com/acme/demo.git", "github", Map.of());
    }

    private static RepositoryReference repository(String repositoryName) {
        return new RepositoryReference("https://example.com/acme/" + repositoryName + ".git", "github", Map.of());
    }

    private static List<String> workspaceIds(List<RepositoryWorkspace> workspaces) {
        return workspaces.stream()
            .map(workspace -> workspace.workspaceId().value())
            .toList();
    }

    private static RevisionSelector revision() {
        return new RevisionSelector("main", true, "", false);
    }

    private static WorkspacePolicy policy() {
        return new WorkspacePolicy(true, true, false, false, 60, 100_000);
    }

    private static CheckoutResult checkoutResult() {
        return new CheckoutResult(
            CheckoutStatus.CHECKED_OUT,
            repository().remoteUrl(),
            "b".repeat(40),
            "main",
            "",
            true,
            5,
            List.of(Diagnostic.info("GIT_CHECKOUT_COMPLETED", "Repository checkout completed")),
            false,
            false,
            List.of(new SourceRoot("src/main/java", "java"))
        );
    }

    private static SourceSnapshot sourceSnapshotWithPackageArtifacts(SourceSnapshotId sourceSnapshotId) {
        return new SourceSnapshot(
            sourceSnapshotId,
            SourceSnapshotCompleteness.COMPLETE,
            List.of(new SourceRoot("src/main/java", "java")),
            artifact("snapshots/source-snapshot-artifacts/manifest.json", "manifest", "a".repeat(64), 128),
            List.of(),
            new SourcePackageDescriptor(
                PackageAvailability.AVAILABLE,
                artifact("snapshots/source-snapshot-artifacts/source-manifest.json", "manifest", "b".repeat(64), 129),
                artifact("snapshots/source-snapshot-artifacts/source-package.zip", "application/zip", "c".repeat(64), 1024),
                "source-package-descriptor-v1",
                "repository-source-service",
                byteAccess("repository-source-service", "repository-source.v1.SourcePackage"),
                SourceSnapshotCompleteness.COMPLETE
            ),
            new BuildOutputPackageDescriptor(
                PackageAvailability.AVAILABLE,
                artifact("snapshots/source-snapshot-artifacts/build-output-manifest.json", "manifest", "d".repeat(64), 130),
                artifact("snapshots/source-snapshot-artifacts/build-output.zip", "application/zip", "e".repeat(64), 2048),
                "build-output-package-descriptor-v1",
                "build-artifact-worker-service",
                byteAccess("build-artifact-worker-service", "build-artifact-worker.v1.BuildOutputPackage"),
                SourceSnapshotCompleteness.COMPLETE,
                buildOutputResolution(),
                "gradle"
            )
        );
    }

    private static BuildOutputResolution buildOutputResolution() {
        return new BuildOutputResolution(
            List.of(
                new BuildOutputProducerCandidate(BuildOutputProducer.ARTIFACT_STORE, BuildOutputProducerStatus.NOT_CONFIGURED, "", List.of()),
                new BuildOutputProducerCandidate(BuildOutputProducer.ARTIFACTORY, BuildOutputProducerStatus.NOT_CONFIGURED, "", List.of()),
                new BuildOutputProducerCandidate(BuildOutputProducer.JENKINS, BuildOutputProducerStatus.NOT_CONFIGURED, "", List.of()),
                new BuildOutputProducerCandidate(
                    BuildOutputProducer.BUILD_ARTIFACT_WORKER,
                    BuildOutputProducerStatus.AVAILABLE,
                    "snapshots/source-snapshot-artifacts/build-output.zip",
                    List.of(Diagnostic.info("BUILD_OUTPUT_AVAILABLE", "Build output package available"))
                )
            ),
            BuildOutputProducer.BUILD_ARTIFACT_WORKER,
            false,
            List.of()
        );
    }

    private static ArtifactReference artifact(String reference, String type, String sha256, long sizeBytes) {
        return new ArtifactReference(reference, type, sha256, sizeBytes);
    }

    private static ArtifactByteAccess byteAccess(String ownerService, String contract) {
        return new ArtifactByteAccess(
            ownerService,
            contract,
            "source-snapshot/source-snapshot-artifacts",
            ArtifactByteCustody.PRODUCER_RETAINED
        );
    }

    private static final class FakeWorkspacePort implements RepositoryWorkspacePort {
        private int prepared;
        private int branchCheckouts;
        private int cleaned;

        @Override
        public PreparedWorkspace prepare(AnalysisRunId analysisRunId, WorkspacePolicy policy) {
            prepared++;
            return new PreparedWorkspace(new WorkspaceId("workspace-run-1"), Path.of("target", "synthetic-workspace"));
        }

        @Override
        public PreparedWorkspace prepareBranchCheckout(
            WorkspaceId workspaceId,
            WorkspaceBranchId workspaceBranchId,
            WorkspacePolicy policy
        ) {
            branchCheckouts++;
            return new PreparedWorkspace(workspaceId, Path.of("target", workspaceBranchId.value()));
        }

        @Override
        public void cleanup(WorkspaceId workspaceId) {
            cleaned++;
        }

        @Override
        public void cleanupBranchCheckout(WorkspaceId workspaceId, WorkspaceBranchId workspaceBranchId) {
        }
    }

    private static final class SequencedCheckoutPort implements RepositoryCheckoutPort {
        private final List<String> commits;
        private int calls;

        private SequencedCheckoutPort(String... commits) {
            this.commits = List.of(commits);
        }

        @Override
        public CheckoutResult checkout(
            PreparedWorkspace workspace,
            RepositoryReference repository,
            RevisionSelector revision,
            WorkspacePolicy policy
        ) {
            var index = Math.min(calls, commits.size() - 1);
            calls++;
            return new CheckoutResult(
                CheckoutStatus.CHECKED_OUT,
                repository.remoteUrl(),
                commits.get(index),
                revision.branch(),
                revision.commit(),
                policy.allowShallowClone(),
                5,
                List.of(Diagnostic.info("GIT_CHECKOUT_COMPLETED", "Repository checkout completed")),
                false,
                false,
                List.of(new SourceRoot("src/main/java", "java"))
            );
        }
    }

    private static final class FakeMetadataPort implements RepositoryMetadataPort {
        @Override
        public RepositoryMetadataResolution resolveMetadata(
            RepositoryReference repository,
            RepositoryMetadataPreviewPolicy policy
        ) {
            return new RepositoryMetadataResolution(
                RepositoryIdentity.from(repository, "main"),
                true,
                List.of(Diagnostic.info("DEFAULT_BRANCH_RESOLVED", "Repository default branch resolved"))
            );
        }
    }

    private static final class FixedRepositoryWorkspaceIdGenerator implements RepositoryWorkspaceIdGenerator {
        @Override
        public WorkspaceId newWorkspaceId() {
            return new WorkspaceId("workspace-0001");
        }

        @Override
        public WorkspaceBranchId newWorkspaceBranchId() {
            return new WorkspaceBranchId("workspace-branch-0001");
        }
    }

    private static final class SequentialRepositoryWorkspaceIdGenerator implements RepositoryWorkspaceIdGenerator {
        private int workspaceIds;
        private int branchIds;

        @Override
        public WorkspaceId newWorkspaceId() {
            workspaceIds++;
            return new WorkspaceId("workspace-%04d".formatted(workspaceIds));
        }

        @Override
        public WorkspaceBranchId newWorkspaceBranchId() {
            branchIds++;
            return new WorkspaceBranchId("workspace-branch-%04d".formatted(branchIds));
        }
    }
}
