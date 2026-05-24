package de.burger.forensics.analytics.services.repositorysource.application;

import de.burger.forensics.analytics.services.repositorysource.adapter.out.h2.H2RepositorySourcePersistenceAdapter;
import de.burger.forensics.analytics.services.repositorysource.application.port.PreparedWorkspace;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryCheckoutPort;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryMetadataPort;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryMetadataPreviewPolicy;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryMetadataResolution;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryWorkspaceIdGenerator;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryWorkspacePort;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.CheckoutResult;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.CheckoutStatus;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.Diagnostic;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryIdentity;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryReference;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceBranchSelector;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceStatus;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RevisionSelector;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.SourceRoot;
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

    private static RevisionSelector revision() {
        return new RevisionSelector("main", true, "", false);
    }

    private static WorkspacePolicy policy() {
        return new WorkspacePolicy(true, true, false, false, 60, 100_000);
    }

    private static final class FakeWorkspacePort implements RepositoryWorkspacePort {
        private int prepared;
        private int branchCheckouts;

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
}
