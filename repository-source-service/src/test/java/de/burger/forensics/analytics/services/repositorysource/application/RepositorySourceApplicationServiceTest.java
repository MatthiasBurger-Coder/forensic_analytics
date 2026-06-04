package de.burger.forensics.analytics.services.repositorysource.application;

import de.burger.forensics.analytics.services.repositorysource.adapter.out.memory.InMemoryRepositoryPreparationRepository;
import de.burger.forensics.analytics.services.repositorysource.adapter.out.memory.InMemoryRepositorySourceIdempotencyRepository;
import de.burger.forensics.analytics.services.repositorysource.adapter.out.memory.InMemoryRepositoryWorkspaceRepository;
import de.burger.forensics.analytics.services.repositorysource.application.port.PreparedWorkspace;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryCheckoutPort;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryMetadataPort;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryMetadataPreviewPolicy;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryMetadataResolution;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositorySourceIdempotencyRecord;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryWorkspaceIdGenerator;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryWorkspacePort;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryIdentity;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.CheckoutResult;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.CheckoutStatus;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.Diagnostic;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.BuildOutputProducer;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryReference;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspace;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceBranchSelector;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceBranch;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceBranchStatus;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceStatus;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RevisionSelector;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.SourceRoot;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.SourceSnapshotId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceBranchId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspacePolicy;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceTitle;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositorySourceApplicationServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-16T10:15:30Z"), ZoneOffset.UTC);
    private final FakeWorkspacePort workspacePort = new FakeWorkspacePort();
    private final RepositorySourceApplicationService service = new RepositorySourceApplicationService(
        new InMemoryRepositoryPreparationRepository(),
        new InMemoryRepositorySourceIdempotencyRepository(),
        workspacePort,
        new FakeCheckoutPort(),
        CLOCK
    );

    @Test
    void preparesGetsAndCleansRepositoryWorkspacesIdempotently() {
        var prepared = service.prepare(
            "prepare-key",
            "schema-v1",
            "correlation-1",
            runId(),
            repository(),
            revision(),
            policy(),
            Map.of("tenant", "demo")
        );
        var samePrepared = service.prepare(
            "prepare-key",
            "schema-v1",
            "correlation-1",
            runId(),
            repository(),
            revision(),
            policy(),
            Map.of("tenant", "demo")
        );
        var loaded = service.get(runId(), prepared.sourceSnapshotId());
        var cleaned = service.cleanup("cleanup-key", "correlation-1", runId(), prepared.workspaceId());
        var sameCleaned = service.cleanup("cleanup-key", "correlation-1", runId(), prepared.workspaceId());

        assertEquals(prepared, samePrepared);
        assertEquals(prepared, loaded);
        assertEquals("source-snapshot-", prepared.sourceSnapshotId().value().substring(0, 16));
        assertEquals("snapshots/" + prepared.sourceSnapshotId().value() + "/manifest.json", prepared.sourceSnapshot().manifestArtifact().reference());
        assertEquals("repository-source-service", prepared.sourceSnapshot().sourcePackage().byteAccess().ownerService());
        assertEquals("build-artifact-worker-service", prepared.sourceSnapshot().buildOutputPackage().byteAccess().ownerService());
        assertEquals(List.of("ARTIFACT_STORE", "ARTIFACTORY", "JENKINS", "BUILD_ARTIFACT_WORKER"), prepared.sourceSnapshot().buildOutputPackage().resolution().candidates().stream()
            .map(candidate -> candidate.producer().name())
            .toList());
        assertEquals(BuildOutputProducer.UNSPECIFIED, prepared.sourceSnapshot().buildOutputPackage().resolution().selectedProducer());
        assertEquals("auto-detect", prepared.sourceSnapshot().buildOutputPackage().buildSystem());
        assertEquals(RepositoryWorkspaceStatus.CLEANED, cleaned.workspaceStatus());
        assertEquals(cleaned, sameCleaned);
        assertEquals(1, workspacePort.cleaned);
    }

    @Test
    void prepareFingerprintUsesDeterministicSafeAttributeOrdering() {
        var firstAttributes = new LinkedHashMap<String, String>();
        firstAttributes.put("z", "last");
        firstAttributes.put("a", "first");
        var secondAttributes = new LinkedHashMap<String, String>();
        secondAttributes.put("a", "first");
        secondAttributes.put("z", "last");

        var prepared = service.prepare(
            "prepare-key",
            "schema-v1",
            "correlation-1",
            runId(),
            repository(),
            revision(),
            policy(),
            firstAttributes
        );
        var replayed = service.prepare(
            "prepare-key",
            "schema-v1",
            "correlation-1",
            runId(),
            repository(),
            revision(),
            policy(),
            secondAttributes
        );

        assertEquals(prepared, replayed);
        assertEquals(List.of("a", "z"), List.copyOf(prepared.safeAttributes().keySet()));
    }

    @Test
    void rejectsConflictingIdempotencyAndMissingPreparations() {
        var prepared = service.prepare(
            "prepare-key",
            "schema-v1",
            "correlation-1",
            runId(),
            repository(),
            revision(),
            policy(),
            Map.of()
        );

        assertThrows(IdempotencyConflictException.class, () -> service.prepare(
            "prepare-key",
            "schema-v2",
            "correlation-1",
            runId(),
            repository(),
            revision(),
            policy(),
            Map.of()
        ));
        assertThrows(RepositoryPreparationNotFoundException.class, () -> service.get(runId(), new SourceSnapshotId("missing")));
        assertThrows(RepositoryPreparationNotFoundException.class, () -> service.cleanup(
            "cleanup-missing",
            "correlation-1",
            runId(),
            new WorkspaceId("workspace-missing")
        ));
        assertThrows(IdempotencyConflictException.class, () -> {
            service.cleanup("cleanup-key", "correlation-1", runId(), prepared.workspaceId());
            service.cleanup("cleanup-key", "other-correlation", runId(), prepared.workspaceId());
        });
    }

    @Test
    void validatesPublicInputsBeforeSideEffects() {
        assertThrows(IllegalArgumentException.class, () -> service.prepare(
            " ",
            "schema-v1",
            "correlation-1",
            runId(),
            repository(),
            revision(),
            policy(),
            Map.of()
        ));
        assertThrows(IllegalArgumentException.class, () -> service.cleanup(" ", "correlation-1", runId(), new WorkspaceId("workspace-1")));
        assertTrue(workspacePort.cleaned >= 0);
    }

    @Test
    void cleansPreparedWorkspaceWhenCheckoutFails() {
        var failingWorkspacePort = new FakeWorkspacePort();
        var failingService = new RepositorySourceApplicationService(
            new InMemoryRepositoryPreparationRepository(),
            new InMemoryRepositorySourceIdempotencyRepository(),
            failingWorkspacePort,
            (workspace, repository, revision, policy) -> {
                throw new IllegalStateException("checkout failed");
            },
            CLOCK
        );

        assertThrows(IllegalStateException.class, () -> failingService.prepare(
            "prepare-key",
            "schema-v1",
            "correlation-1",
            runId(),
            repository(),
            revision(),
            policy(),
            Map.of()
        ));
        assertEquals(1, failingWorkspacePort.cleaned);
    }

    @Test
    void createsRepositoryWorkspaceIdempotentlyForSameFingerprint() {
        var idGenerator = new FixedRepositoryWorkspaceIdGenerator();
        var workspaceService = workspaceService(idGenerator);

        var workspace = workspaceService.createOrReuseRepositoryWorkspace(
            "workspace-key",
            repositoryIdentity(),
            Map.of("tenant", "demo")
        );
        var replayed = workspaceService.createOrReuseRepositoryWorkspace(
            "workspace-key",
            repositoryIdentity(),
            Map.of("tenant", "demo")
        );
        var reusedWithNewKey = workspaceService.createOrReuseRepositoryWorkspace(
            "workspace-key-2",
            repositoryIdentity(),
            Map.of("tenant", "demo")
        );

        assertEquals(workspace, replayed);
        assertEquals(workspace, reusedWithNewKey);
        assertEquals("workspace-0001", workspace.workspaceId().value());
        assertEquals("demo", workspace.workspaceTitle().value());
        assertEquals(1, idGenerator.workspaceIds);
    }

    @Test
    void listsRepositoryWorkspacesDeterministicallyAndExcludesCleanedByDefault() {
        var repository = new InMemoryRepositoryWorkspaceRepository();
        var workspaceService = workspaceService(
            repository,
            new FixedRepositoryWorkspaceIdGenerator(),
            new InMemoryRepositorySourceIdempotencyRepository(),
            new FakeWorkspacePort(),
            new SequencedCheckoutPort("b".repeat(40)),
            new FakeMetadataPort("main", true)
        );
        repository.save(workspaceFixture(
            "workspace-0003",
            "gamma",
            RepositoryWorkspaceStatus.READY,
            List.of(
                branchFixture("workspace-0003", "workspace-branch-0002", "release/1.0", RepositoryWorkspaceBranchStatus.CHECKED_OUT),
                branchFixture("workspace-0003", "workspace-branch-0001", "main", RepositoryWorkspaceBranchStatus.CHECKED_OUT)
            )
        ));
        repository.save(workspaceFixture("workspace-0001", "alpha", RepositoryWorkspaceStatus.READY, List.of()));
        repository.save(workspaceFixture("workspace-0002", "beta", RepositoryWorkspaceStatus.CLEANED, List.of()));

        var visible = workspaceService.listRepositoryWorkspaces("schema-v1", "correlation-1", false);
        var all = workspaceService.listRepositoryWorkspaces("schema-v1", "correlation-1", true);

        assertEquals(List.of("workspace-0001", "workspace-0003"), workspaceIds(visible));
        assertEquals(List.of("workspace-0001", "workspace-0002", "workspace-0003"), workspaceIds(all));
        assertEquals(List.of("main", "release/1.0"), visible.get(1).branches().stream()
            .map(RepositoryWorkspaceBranch::repositoryBranch)
            .toList());
        assertThrows(IllegalArgumentException.class, () -> workspaceService.listRepositoryWorkspaces(" ", "correlation-1", false));
    }

    @Test
    void rejectsBlankResolvedDefaultBranchBeforeWorkspaceSideEffects() {
        var idGenerator = new FixedRepositoryWorkspaceIdGenerator();
        var workspacePort = new FakeWorkspacePort();
        var workspaceService = workspaceService(
            idGenerator,
            workspacePort,
            new SequencedCheckoutPort("b".repeat(40)),
            new FakeMetadataPort("", true)
        );

        assertThrows(IllegalArgumentException.class, () -> workspaceService.createOrReuseRepositoryWorkspaceWithCheckout(
            "checkout-key",
            "schema-v1",
            "correlation-1",
            repository(),
            new RepositoryWorkspaceBranchSelector("", ""),
            policy(),
            Map.of()
        ));
        assertEquals(0, idGenerator.workspaceIds);
        assertEquals(0, idGenerator.branchIds);
        assertEquals(0, workspacePort.branchCheckouts);
    }

    @Test
    void cleanupByWorkspaceIdMarksCleanedRetainsMetadataAndIsIdempotent() {
        var repository = new InMemoryRepositoryWorkspaceRepository();
        var idGenerator = new FixedRepositoryWorkspaceIdGenerator();
        var workspacePort = new FakeWorkspacePort();
        var workspaceService = workspaceService(
            repository,
            idGenerator,
            new InMemoryRepositorySourceIdempotencyRepository(),
            workspacePort,
            new SequencedCheckoutPort("b".repeat(40)),
            new FakeMetadataPort("main", true)
        );
        var workspace = workspaceService.createOrReuseRepositoryWorkspaceWithCheckout(
            "checkout-key",
            "schema-v1",
            "correlation-1",
            repository(),
            new RepositoryWorkspaceBranchSelector("main", ""),
            policy(),
            Map.of("tenant", "demo")
        );
        var branch = workspace.branches().getFirst();

        var cleaned = workspaceService.cleanupRepositoryWorkspaceById(
            "cleanup-key",
            "schema-v1",
            "correlation-1",
            workspace.workspaceId(),
            Map.of("tenant", "demo")
        );
        var replayed = workspaceService.cleanupRepositoryWorkspaceById(
            "cleanup-key",
            "schema-v1",
            "correlation-1",
            workspace.workspaceId(),
            Map.of("tenant", "demo")
        );
        var alreadyCleaned = workspaceService.cleanupRepositoryWorkspaceById(
            "cleanup-key-2",
            "schema-v1",
            "correlation-1",
            workspace.workspaceId(),
            Map.of("tenant", "demo")
        );
        var loaded = workspaceService.getRepositoryWorkspace(workspace.workspaceId());

        assertEquals(RepositoryWorkspaceStatus.CLEANED, cleaned.workspaceStatus());
        assertEquals(cleaned, replayed);
        assertEquals(RepositoryWorkspaceStatus.CLEANED, alreadyCleaned.workspaceStatus());
        assertEquals(RepositoryWorkspaceStatus.CLEANED, loaded.status());
        assertEquals(workspace.workspaceTitle(), loaded.workspaceTitle());
        assertEquals(workspace.repository(), loaded.repository());
        assertEquals(branch.workspaceBranchId(), loaded.branches().getFirst().workspaceBranchId());
        assertEquals(branch.sourceSnapshotId(), loaded.branches().getFirst().sourceSnapshotId());
        assertEquals(List.of(), workspaceService.listRepositoryWorkspaces("schema-v1", "correlation-1", false));
        assertEquals(List.of(workspace.workspaceId().value()), workspaceIds(workspaceService
            .listRepositoryWorkspaces("schema-v1", "correlation-1", true)));
        assertEquals(1, workspacePort.cleaned);
        assertThrows(IdempotencyConflictException.class, () -> workspaceService.cleanupRepositoryWorkspaceById(
            "cleanup-key",
            "schema-v1",
            "correlation-1",
            workspace.workspaceId(),
            Map.of("tenant", "other")
        ));
    }

    @Test
    void cleanupByWorkspaceIdRejectsInProgressBranchesBeforeSideEffects() {
        var repository = new InMemoryRepositoryWorkspaceRepository();
        var workspacePort = new FakeWorkspacePort();
        var workspaceService = workspaceService(
            repository,
            new FixedRepositoryWorkspaceIdGenerator(),
            new InMemoryRepositorySourceIdempotencyRepository(),
            workspacePort,
            new SequencedCheckoutPort("b".repeat(40)),
            new FakeMetadataPort("main", true)
        );
        var checkingOut = workspaceFixture(
            "workspace-0001",
            "checking-out",
            RepositoryWorkspaceStatus.READY,
            List.of(branchFixture(
                "workspace-0001",
                "workspace-branch-0001",
                "main",
                RepositoryWorkspaceBranchStatus.CHECKING_OUT
            ))
        );
        var updating = workspaceFixture(
            "workspace-0002",
            "updating",
            RepositoryWorkspaceStatus.READY,
            List.of(branchFixture(
                "workspace-0002",
                "workspace-branch-0002",
                "main",
                RepositoryWorkspaceBranchStatus.UPDATING
            ))
        );
        repository.save(checkingOut);
        repository.save(updating);

        assertThrows(IllegalStateException.class, () -> workspaceService.cleanupRepositoryWorkspaceById(
            "cleanup-checking-out",
            "schema-v1",
            "correlation-1",
            checkingOut.workspaceId(),
            Map.of()
        ));
        assertThrows(IllegalStateException.class, () -> workspaceService.cleanupRepositoryWorkspaceById(
            "cleanup-updating",
            "schema-v1",
            "correlation-1",
            updating.workspaceId(),
            Map.of()
        ));
        assertEquals(0, workspacePort.cleaned);
        assertEquals(RepositoryWorkspaceStatus.READY, workspaceService.getRepositoryWorkspace(checkingOut.workspaceId()).status());
        assertEquals(RepositoryWorkspaceStatus.READY, workspaceService.getRepositoryWorkspace(updating.workspaceId()).status());
    }

    @Test
    void rejectsCreateWorkspaceIdempotencyConflictWithoutSavingOrCheckingOutAgain() {
        var idGenerator = new FixedRepositoryWorkspaceIdGenerator();
        var workspaceService = workspaceService(idGenerator);

        workspaceService.createOrReuseRepositoryWorkspace("workspace-key", repositoryIdentity(), Map.of("tenant", "demo"));

        assertThrows(IdempotencyConflictException.class, () -> workspaceService.createOrReuseRepositoryWorkspace(
            "workspace-key",
            repositoryIdentity(),
            Map.of("tenant", "other")
        ));
        assertEquals(1, idGenerator.workspaceIds);
    }

    @Test
    void getsRepositoryWorkspaceByOpaqueIdAndReportsMissingWorkspaceExplicitly() {
        var workspaceService = workspaceService(new FixedRepositoryWorkspaceIdGenerator());
        var workspace = workspaceService.createOrReuseRepositoryWorkspace(
            "workspace-key",
            repositoryIdentity(),
            Map.of()
        );

        assertEquals(workspace, workspaceService.getRepositoryWorkspace(workspace.workspaceId()));
        assertThrows(RepositoryWorkspaceNotFoundException.class, () -> workspaceService.getRepositoryWorkspace(
            new WorkspaceId("workspace-missing")
        ));
    }

    @Test
    void keepsBranchNamesAsDataWhenCreatingWorkspaceBranch() {
        var idGenerator = new FixedRepositoryWorkspaceIdGenerator();
        var workspaceService = workspaceService(idGenerator);
        var workspace = workspaceService.createOrReuseRepositoryWorkspace(
            "workspace-key",
            repositoryIdentity(),
            Map.of()
        );
        var branchSelector = new RepositoryWorkspaceBranchSelector("feature/workspace-ui", "");

        var branch = workspaceService.createOrReuseRepositoryWorkspaceBranch(
            "branch-key",
            workspace.workspaceId(),
            branchSelector
        );
        var replayed = workspaceService.createOrReuseRepositoryWorkspaceBranch(
            "branch-key",
            workspace.workspaceId(),
            branchSelector
        );
        var started = workspaceService.markBranchCheckoutStarted(workspace.workspaceId(), branch.workspaceBranchId());
        var checkedOut = workspaceService.markBranchCheckoutCompleted(
            workspace.workspaceId(),
            branch.workspaceBranchId(),
            checkoutResult("c".repeat(40)),
            new SourceSnapshotId("source-snapshot-branch"),
            List.of(Diagnostic.info("CHECKED_OUT", "Repository checkout completed"))
        );

        assertEquals(branch, replayed);
        assertEquals("workspace-branch-0001", branch.workspaceBranchId().value());
        assertEquals("feature/workspace-ui", branch.repositoryBranch());
        assertEquals(RepositoryWorkspaceBranchStatus.CHECKING_OUT, started.status());
        assertEquals("REPOSITORY_CHECKOUT_STARTED", started.diagnostics().getFirst().code());
        assertEquals(RepositoryWorkspaceBranchStatus.CHECKED_OUT, checkedOut.status());
        assertEquals("c".repeat(40), checkedOut.resolvedCommit());
        assertEquals(1, idGenerator.branchIds);
        assertThrows(IllegalArgumentException.class, () -> workspaceService.createOrReuseRepositoryWorkspaceBranch(
            "branch-missing",
            workspace.workspaceId(),
            new RepositoryWorkspaceBranchSelector("", "")
        ));
    }

    @Test
    void rejectsBranchCommitAndCheckoutProvenanceMismatches() {
        var workspaceService = workspaceService(new FixedRepositoryWorkspaceIdGenerator());
        var workspace = workspaceService.createOrReuseRepositoryWorkspace(
            "workspace-key",
            repositoryIdentity(),
            Map.of()
        );
        var branch = workspaceService.createOrReuseRepositoryWorkspaceBranch(
            "branch-key",
            workspace.workspaceId(),
            new RepositoryWorkspaceBranchSelector("feature/workspace-ui", "b".repeat(40))
        );

        assertThrows(IllegalArgumentException.class, () -> workspaceService.createOrReuseRepositoryWorkspaceBranch(
            "branch-key-2",
            workspace.workspaceId(),
            new RepositoryWorkspaceBranchSelector("feature/workspace-ui", "c".repeat(40))
        ));
        assertThrows(IllegalArgumentException.class, () -> workspaceService.markBranchCheckoutCompleted(
            workspace.workspaceId(),
            branch.workspaceBranchId(),
            checkoutResult("other", "b".repeat(40), CheckoutStatus.CHECKED_OUT, "c".repeat(40)),
            new SourceSnapshotId("source-snapshot-branch"),
            List.of()
        ));
        assertThrows(IllegalArgumentException.class, () -> workspaceService.markBranchCheckoutCompleted(
            workspace.workspaceId(),
            branch.workspaceBranchId(),
            checkoutResult("feature/workspace-ui", "c".repeat(40), CheckoutStatus.CHECKED_OUT, "c".repeat(40)),
            new SourceSnapshotId("source-snapshot-branch"),
            List.of()
        ));
        assertThrows(IllegalArgumentException.class, () -> workspaceService.markBranchCheckoutCompleted(
            workspace.workspaceId(),
            branch.workspaceBranchId(),
            checkoutResult("feature/workspace-ui", "b".repeat(40), CheckoutStatus.FAILED, "c".repeat(40)),
            new SourceSnapshotId("source-snapshot-branch"),
            List.of()
        ));
    }

    @Test
    void previewsRepositoryMetadataWithoutPreparingCheckoutWorkspace() {
        var workspacePort = new FakeWorkspacePort();
        var checkoutPort = new SequencedCheckoutPort("b".repeat(40));
        var metadataPort = new FakeMetadataPort("main", true);
        var workspaceService = workspaceService(new FixedRepositoryWorkspaceIdGenerator(), workspacePort, checkoutPort, metadataPort);

        var preview = workspaceService.previewRepositoryWorkspaceMetadata(
            "schema-v1",
            "correlation-1",
            repository(),
            new RepositoryMetadataPreviewPolicy(30),
            Map.of("tenant", "demo")
        );

        assertEquals("example.com/acme/demo", preview.repository().repositoryKey().value());
        assertEquals("main", preview.repository().defaultBranch());
        assertEquals(List.of("main"), preview.repositoryBranches());
        assertEquals("demo", preview.workspaceTitle().value());
        assertEquals(1, metadataPort.calls);
        assertEquals(0, workspacePort.branchCheckouts);
        assertEquals(0, checkoutPort.calls);
    }

    @Test
    void createsWorkspaceAndChecksOutResolvedDefaultBranchIdempotently() {
        var idGenerator = new FixedRepositoryWorkspaceIdGenerator();
        var workspacePort = new FakeWorkspacePort();
        var checkoutPort = new SequencedCheckoutPort("b".repeat(40));
        var workspaceService = workspaceService(idGenerator, workspacePort, checkoutPort, new FakeMetadataPort("main", true));

        var workspace = workspaceService.createOrReuseRepositoryWorkspaceWithCheckout(
            "checkout-key",
            "schema-v1",
            "correlation-1",
            repository(),
            new RepositoryWorkspaceBranchSelector("", ""),
            policy(),
            Map.of("tenant", "demo")
        );
        var replayed = workspaceService.createOrReuseRepositoryWorkspaceWithCheckout(
            "checkout-key",
            "schema-v1",
            "correlation-1",
            repository(),
            new RepositoryWorkspaceBranchSelector("", ""),
            policy(),
            Map.of("tenant", "demo")
        );

        assertEquals(workspace, replayed);
        assertEquals("main", workspace.branches().getFirst().repositoryBranch());
        assertEquals(RepositoryWorkspaceBranchStatus.CHECKED_OUT, workspace.branches().getFirst().status());
        assertEquals("b".repeat(40), workspace.branches().getFirst().resolvedCommit());
        assertEquals("source-snapshot-", workspace.branches().getFirst().sourceSnapshotId().value().substring(0, 16));
        assertEquals(1, idGenerator.workspaceIds);
        assertEquals(1, idGenerator.branchIds);
        assertEquals(1, workspacePort.branchCheckouts);
        assertEquals(1, checkoutPort.calls);
    }

    @Test
    void reusesCompletedWorkspaceBranchWithoutRepeatingCheckoutForNewIdempotencyKey() {
        var idGenerator = new FixedRepositoryWorkspaceIdGenerator();
        var workspacePort = new FakeWorkspacePort();
        var checkoutPort = new SequencedCheckoutPort("b".repeat(40), "c".repeat(40));
        var workspaceService = workspaceService(idGenerator, workspacePort, checkoutPort, new FakeMetadataPort("main", true));

        var workspace = workspaceService.createOrReuseRepositoryWorkspaceWithCheckout(
            "checkout-key",
            "schema-v1",
            "correlation-1",
            repository(),
            new RepositoryWorkspaceBranchSelector("main", ""),
            policy(),
            Map.of()
        );
        var reused = workspaceService.createOrReuseRepositoryWorkspaceWithCheckout(
            "checkout-key-new",
            "schema-v1",
            "correlation-1",
            repository(),
            new RepositoryWorkspaceBranchSelector("main", ""),
            policy(),
            Map.of()
        );

        assertEquals(workspace, reused);
        assertEquals(1, workspacePort.branchCheckouts);
        assertEquals(1, checkoutPort.calls);
    }

    @Test
    void reactivatesCleanedWorkspaceAndRepeatsBranchCheckoutForSameRepository() {
        var idGenerator = new FixedRepositoryWorkspaceIdGenerator();
        var workspacePort = new FakeWorkspacePort();
        var checkoutPort = new SequencedCheckoutPort("b".repeat(40), "c".repeat(40));
        var workspaceService = workspaceService(idGenerator, workspacePort, checkoutPort, new FakeMetadataPort("main", true));

        var workspace = workspaceService.createOrReuseRepositoryWorkspaceWithCheckout(
            "checkout-key",
            "schema-v1",
            "correlation-1",
            repository(),
            new RepositoryWorkspaceBranchSelector("main", ""),
            policy(),
            Map.of("tenant", "demo")
        );
        workspaceService.cleanupRepositoryWorkspaceById(
            "cleanup-key",
            "schema-v1",
            "correlation-1",
            workspace.workspaceId(),
            Map.of("tenant", "demo")
        );

        var recreated = workspaceService.createOrReuseRepositoryWorkspaceWithCheckout(
            "checkout-key-after-cleanup",
            "schema-v1",
            "correlation-1",
            repository(),
            new RepositoryWorkspaceBranchSelector("main", ""),
            policy(),
            Map.of("tenant", "demo")
        );

        assertEquals(workspace.workspaceId(), recreated.workspaceId());
        assertEquals(RepositoryWorkspaceStatus.READY, recreated.status());
        assertEquals(RepositoryWorkspaceBranchStatus.CHECKED_OUT, recreated.branches().getFirst().status());
        assertEquals("c".repeat(40), recreated.branches().getFirst().resolvedCommit());
        assertEquals(List.of(workspace.workspaceId().value()), workspaceIds(workspaceService
            .listRepositoryWorkspaces("schema-v1", "correlation-1", false)));
        assertTrue(recreated.diagnostics().stream()
            .anyMatch(diagnostic -> "REPOSITORY_WORKSPACE_REACTIVATED".equals(diagnostic.code())));
        assertEquals(1, idGenerator.workspaceIds);
        assertEquals(1, idGenerator.branchIds);
        assertEquals(2, workspacePort.branchCheckouts);
        assertEquals(2, checkoutPort.calls);
    }

    @Test
    void rejectsUnresolvedDefaultBranchBeforeWorkspaceMutation() {
        var idGenerator = new FixedRepositoryWorkspaceIdGenerator();
        var workspacePort = new FakeWorkspacePort();
        var checkoutPort = new SequencedCheckoutPort("b".repeat(40));
        var workspaceService = workspaceService(idGenerator, workspacePort, checkoutPort, new FakeMetadataPort("", false));

        assertThrows(IllegalArgumentException.class, () -> workspaceService.createOrReuseRepositoryWorkspaceWithCheckout(
            "checkout-key",
            "schema-v1",
            "correlation-1",
            repository(),
            new RepositoryWorkspaceBranchSelector("", ""),
            policy(),
            Map.of()
        ));
        assertEquals(0, idGenerator.workspaceIds);
        assertEquals(0, workspacePort.branchCheckouts);
        assertEquals(0, checkoutPort.calls);
    }

    @Test
    void marksWorkspaceBranchFailedAndCleansCheckoutDirectoryWhenCheckoutFails() {
        var idGenerator = new FixedRepositoryWorkspaceIdGenerator();
        var workspacePort = new FakeWorkspacePort();
        var workspaceService = workspaceService(
            idGenerator,
            workspacePort,
            (workspace, repository, revision, policy) -> {
                throw new IllegalStateException("checkout failed");
            },
            new FakeMetadataPort("main", true)
        );

        var workspace = workspaceService.createOrReuseRepositoryWorkspaceWithCheckout(
            "checkout-key",
            "schema-v1",
            "correlation-1",
            repository(),
            new RepositoryWorkspaceBranchSelector("main", ""),
            policy(),
            Map.of()
        );

        var failed = workspace.branches().getFirst();
        assertEquals(RepositoryWorkspaceBranchStatus.FAILED, failed.status());
        assertEquals(1, workspacePort.cleaned);
    }

    @Test
    void retriesFailedWorkspaceBranchCheckoutForNewSemanticOperation() {
        var idGenerator = new FixedRepositoryWorkspaceIdGenerator();
        var workspacePort = new FakeWorkspacePort();
        var calls = new int[1];
        var workspaceService = workspaceService(
            idGenerator,
            workspacePort,
            (workspace, repository, revision, policy) -> {
                calls[0]++;
                if (calls[0] == 1) {
                    throw new IllegalStateException("checkout failed");
                }
                return checkoutResult("main", "", CheckoutStatus.CHECKED_OUT, "b".repeat(40));
            },
            new FakeMetadataPort("main", true)
        );

        var failed = workspaceService.createOrReuseRepositoryWorkspaceWithCheckout(
            "checkout-key",
            "schema-v1",
            "correlation-1",
            repository(),
            new RepositoryWorkspaceBranchSelector("main", ""),
            policy(),
            Map.of()
        );
        var retried = workspaceService.createOrReuseRepositoryWorkspaceWithCheckout(
            "checkout-key-retry",
            "schema-v1",
            "correlation-1",
            repository(),
            new RepositoryWorkspaceBranchSelector("main", ""),
            policy(),
            Map.of()
        );

        assertEquals(RepositoryWorkspaceBranchStatus.FAILED, failed.branches().getFirst().status());
        assertEquals(RepositoryWorkspaceBranchStatus.CHECKED_OUT, retried.branches().getFirst().status());
        assertEquals("b".repeat(40), retried.branches().getFirst().resolvedCommit());
        assertEquals(1, idGenerator.workspaceIds);
        assertEquals(1, idGenerator.branchIds);
        assertEquals(2, workspacePort.branchCheckouts);
        assertEquals(1, workspacePort.cleaned);
        assertEquals(2, calls[0]);
    }

    @Test
    void returnsWorkspaceBeforeScheduledCheckoutCompletesAndUpdatesBranchLater() {
        var repository = new InMemoryRepositoryWorkspaceRepository();
        var idGenerator = new FixedRepositoryWorkspaceIdGenerator();
        var workspacePort = new FakeWorkspacePort();
        var checkoutPort = new SequencedCheckoutPort("b".repeat(40));
        var executor = new CapturingExecutor();
        var workspaceService = new RepositoryWorkspaceApplicationService(
            repository,
            idGenerator,
            new InMemoryRepositorySourceIdempotencyRepository(),
            workspacePort,
            checkoutPort,
            new FakeMetadataPort("main", true),
            CLOCK,
            executor
        );

        var workspace = workspaceService.createOrReuseRepositoryWorkspaceWithCheckout(
            "checkout-key",
            "schema-v1",
            "correlation-1",
            repository(),
            new RepositoryWorkspaceBranchSelector("main", ""),
            policy(),
            Map.of()
        );
        var replayedBeforeCheckout = workspaceService.createOrReuseRepositoryWorkspaceWithCheckout(
            "checkout-key",
            "schema-v1",
            "correlation-1",
            repository(),
            new RepositoryWorkspaceBranchSelector("main", ""),
            policy(),
            Map.of()
        );

        assertEquals(RepositoryWorkspaceBranchStatus.CHECKING_OUT, workspace.branches().getFirst().status());
        assertEquals(RepositoryWorkspaceBranchStatus.CHECKING_OUT, replayedBeforeCheckout.branches().getFirst().status());
        assertEquals("REPOSITORY_CHECKOUT_STARTED", workspace.branches().getFirst().diagnostics().getFirst().code());
        assertEquals(1, executor.tasks.size());
        assertEquals(0, workspacePort.branchCheckouts);
        assertEquals(0, checkoutPort.calls);

        executor.runNext();

        var checkedOut = workspaceService.getRepositoryWorkspace(workspace.workspaceId()).branches().getFirst();
        assertEquals(RepositoryWorkspaceBranchStatus.CHECKED_OUT, checkedOut.status());
        assertEquals("b".repeat(40), checkedOut.resolvedCommit());
        assertEquals(1, workspacePort.branchCheckouts);
        assertEquals(1, checkoutPort.calls);
    }

    @Test
    void refreshesWorkspaceBranchAsUpToDateOrUpdatedIdempotently() {
        var idGenerator = new FixedRepositoryWorkspaceIdGenerator();
        var workspacePort = new FakeWorkspacePort();
        var checkoutPort = new SequencedCheckoutPort("b".repeat(40), "b".repeat(40), "c".repeat(40));
        var workspaceService = workspaceService(idGenerator, workspacePort, checkoutPort, new FakeMetadataPort("main", true));
        var workspace = workspaceService.createOrReuseRepositoryWorkspaceWithCheckout(
            "checkout-key",
            "schema-v1",
            "correlation-1",
            repository(),
            new RepositoryWorkspaceBranchSelector("main", ""),
            policy(),
            Map.of()
        );
        var checkedOut = workspace.branches().getFirst();

        var upToDate = workspaceService.refreshRepositoryWorkspaceBranch(
            "refresh-same",
            "schema-v1",
            "correlation-1",
            workspace.workspaceId(),
            checkedOut.workspaceBranchId(),
            policy(),
            Map.of()
        );
        var replayed = workspaceService.refreshRepositoryWorkspaceBranch(
            "refresh-same",
            "schema-v1",
            "correlation-1",
            workspace.workspaceId(),
            checkedOut.workspaceBranchId(),
            policy(),
            Map.of()
        );
        var updated = workspaceService.refreshRepositoryWorkspaceBranch(
            "refresh-new",
            "schema-v1",
            "correlation-1",
            workspace.workspaceId(),
            checkedOut.workspaceBranchId(),
            policy(),
            Map.of()
        );

        assertEquals(upToDate.changed(), replayed.changed());
        assertEquals(upToDate.branch(), replayed.branch());
        assertFalse(upToDate.changed());
        assertEquals(RepositoryWorkspaceBranchStatus.UP_TO_DATE, upToDate.branch().status());
        assertEquals(checkedOut.sourceSnapshotId(), upToDate.branch().sourceSnapshotId());
        assertTrue(updated.changed());
        assertEquals(RepositoryWorkspaceBranchStatus.UPDATED, updated.branch().status());
        assertEquals("b".repeat(40), updated.previousCommit());
        assertEquals(checkedOut.sourceSnapshotId(), updated.previousSourceSnapshotId());
        assertEquals("c".repeat(40), updated.branch().resolvedCommit());
        assertNotEquals(checkedOut.sourceSnapshotId(), updated.branch().sourceSnapshotId());
        assertEquals(3, checkoutPort.calls);
    }

    @Test
    void marksRefreshFailedWithoutFetchingWhenRemoteBranchNoLongerExists() {
        var idGenerator = new FixedRepositoryWorkspaceIdGenerator();
        var workspacePort = new FakeWorkspacePort();
        var checkoutPort = new SequencedCheckoutPort("b".repeat(40), "c".repeat(40));
        var workspaceService = workspaceService(
            idGenerator,
            workspacePort,
            checkoutPort,
            new FakeMetadataPort("main", true, List.of("main"))
        );
        var workspace = workspaceService.createOrReuseRepositoryWorkspaceWithCheckout(
            "checkout-key",
            "schema-v1",
            "correlation-1",
            repository(),
            new RepositoryWorkspaceBranchSelector("release/1.0", ""),
            policy(),
            Map.of()
        );
        var checkedOut = workspace.branches().getFirst();

        var result = workspaceService.refreshRepositoryWorkspaceBranch(
            "refresh-missing-remote",
            "schema-v1",
            "correlation-1",
            workspace.workspaceId(),
            checkedOut.workspaceBranchId(),
            policy(),
            Map.of()
        );

        assertFalse(result.changed());
        assertEquals(RepositoryWorkspaceBranchStatus.FAILED, result.branch().status());
        assertEquals("REMOTE_BRANCH_NOT_FOUND", result.diagnostics().getFirst().code());
        assertEquals(1, checkoutPort.calls);
        assertEquals(1, workspacePort.branchCheckouts);
    }

    @Test
    void rejectsRefreshBeforeCheckoutAndMarksFailedRefreshWithoutCleanupWhenCheckoutFails() {
        var repository = new InMemoryRepositoryWorkspaceRepository();
        var idempotencyRepository = new InMemoryRepositorySourceIdempotencyRepository();
        var idGenerator = new FixedRepositoryWorkspaceIdGenerator();
        var workspacePort = new FakeWorkspacePort();
        var checkoutPort = new SequencedCheckoutPort("b".repeat(40));
        var workspaceService = workspaceService(
            repository,
            idGenerator,
            idempotencyRepository,
            workspacePort,
            checkoutPort,
            new FakeMetadataPort("main", true)
        );
        var workspace = workspaceService.createOrReuseRepositoryWorkspace(
            "workspace-key",
            repositoryIdentity(),
            Map.of()
        );
        var branch = workspaceService.createOrReuseRepositoryWorkspaceBranch(
            "branch-key",
            workspace.workspaceId(),
            new RepositoryWorkspaceBranchSelector("main", "")
        );

        assertThrows(IllegalArgumentException.class, () -> workspaceService.refreshRepositoryWorkspaceBranch(
            "refresh-before-checkout",
            "schema-v1",
            "correlation-1",
            workspace.workspaceId(),
            branch.workspaceBranchId(),
            policy(),
            Map.of()
        ));

        var checkedOut = workspaceService.markBranchCheckoutCompleted(
            workspace.workspaceId(),
            branch.workspaceBranchId(),
            checkoutResult("main", "", CheckoutStatus.CHECKED_OUT, "b".repeat(40)),
            new SourceSnapshotId("source-snapshot-branch"),
            List.of(Diagnostic.info("CHECKED_OUT", "Repository checkout completed"))
        );
        var failingRefreshService = workspaceService(
            repository,
            idGenerator,
            idempotencyRepository,
            workspacePort,
            (preparedWorkspace, repositoryReference, revision, policy) -> {
                throw new IllegalStateException("refresh failed");
            },
            new FakeMetadataPort("main", true)
        );

        assertThrows(IllegalStateException.class, () -> failingRefreshService.refreshRepositoryWorkspaceBranch(
            "refresh-fails",
            "schema-v1",
            "correlation-1",
            workspace.workspaceId(),
            checkedOut.workspaceBranchId(),
            policy(),
            Map.of()
        ));

        assertEquals(RepositoryWorkspaceBranchStatus.FAILED, failingRefreshService
            .getRepositoryWorkspace(workspace.workspaceId())
            .branches()
            .getFirst()
            .status());
        assertEquals(0, workspacePort.cleaned);
    }

    @Test
    void rejectsRefreshIdempotencyConflictBeforeNewCheckout() {
        var workspacePort = new FakeWorkspacePort();
        var checkoutPort = new SequencedCheckoutPort("b".repeat(40), "b".repeat(40));
        var workspaceService = workspaceService(new FixedRepositoryWorkspaceIdGenerator(), workspacePort, checkoutPort, new FakeMetadataPort("main", true));
        var workspace = workspaceService.createOrReuseRepositoryWorkspaceWithCheckout(
            "checkout-key",
            "schema-v1",
            "correlation-1",
            repository(),
            new RepositoryWorkspaceBranchSelector("main", ""),
            policy(),
            Map.of()
        );
        var branch = workspace.branches().getFirst();
        workspaceService.refreshRepositoryWorkspaceBranch(
            "refresh-key",
            "schema-v1",
            "correlation-1",
            workspace.workspaceId(),
            branch.workspaceBranchId(),
            policy(),
            Map.of()
        );

        assertThrows(IdempotencyConflictException.class, () -> workspaceService.refreshRepositoryWorkspaceBranch(
            "refresh-key",
            "schema-v1",
            "correlation-1",
            workspace.workspaceId(),
            branch.workspaceBranchId(),
            new WorkspacePolicy(true, true, false, false, 30, 100_000),
            Map.of()
        ));
        assertThrows(IdempotencyConflictException.class, () -> workspaceService.refreshRepositoryWorkspaceBranch(
            "refresh-key",
            "schema-v1",
            "correlation-1",
            workspace.workspaceId(),
            branch.workspaceBranchId(),
            policy(),
            Map.of("tenant", "other")
        ));
        assertEquals(2, checkoutPort.calls);

        var otherBranch = workspaceService.createOrReuseRepositoryWorkspaceBranch(
            "branch-other",
            workspace.workspaceId(),
            new RepositoryWorkspaceBranchSelector("release/1.0", "")
        );

        assertThrows(IdempotencyConflictException.class, () -> workspaceService.refreshRepositoryWorkspaceBranch(
            "refresh-key",
            "schema-v1",
            "correlation-1",
            workspace.workspaceId(),
            otherBranch.workspaceBranchId(),
            policy(),
            Map.of()
        ));
        assertEquals(2, checkoutPort.calls);
    }

    @Test
    void replaysWorkspaceBranchAndRefreshFromLegacyIdempotencyRecordsWithoutPayload() {
        var repository = new InMemoryRepositoryWorkspaceRepository();
        var idempotencyRepository = new InMemoryRepositorySourceIdempotencyRepository();
        var service = workspaceService(
            repository,
            new FixedRepositoryWorkspaceIdGenerator(),
            idempotencyRepository,
            new FakeWorkspacePort(),
            new SequencedCheckoutPort("b".repeat(40)),
            new FakeMetadataPort("main", true)
        );
        var workspace = service.createOrReuseRepositoryWorkspace("workspace-key", repositoryIdentity(), Map.of());
        var branch = service.createOrReuseRepositoryWorkspaceBranch(
            "branch-key",
            workspace.workspaceId(),
            new RepositoryWorkspaceBranchSelector("main", "")
        );
        var checkedOut = service.markBranchCheckoutCompleted(
            workspace.workspaceId(),
            branch.workspaceBranchId(),
            checkoutResult("main", "", CheckoutStatus.CHECKED_OUT, "b".repeat(40)),
            new SourceSnapshotId("source-snapshot-branch"),
            List.of(Diagnostic.info("CHECKED_OUT", "Repository checkout completed"))
        );
        idempotencyRepository.save(record(
            "CREATE_WORKSPACE",
            "workspace-legacy",
            "example.com/acme/demo|{}",
            "REPOSITORY_WORKSPACE",
            workspace.workspaceId().value()
        ));
        idempotencyRepository.save(record(
            "CREATE_WORKSPACE_BRANCH",
            "branch-legacy",
            workspace.workspaceId().value() + "|main|",
            "REPOSITORY_WORKSPACE_BRANCH",
            branch.workspaceBranchId().value()
        ));
        idempotencyRepository.save(record(
            "REFRESH_WORKSPACE_BRANCH",
            "refresh-legacy",
            workspace.workspaceId().value() + "|" + branch.workspaceBranchId().value()
                + "|main|true|true|false|false|60|100000|{}",
            "REPOSITORY_WORKSPACE_BRANCH_REFRESH",
            "false|" + "b".repeat(40) + "|source-snapshot-branch|" + branch.workspaceBranchId().value()
        ));

        assertEquals(
            service.getRepositoryWorkspace(workspace.workspaceId()),
            service.createOrReuseRepositoryWorkspace("workspace-legacy", repositoryIdentity(), Map.of())
        );
        assertEquals(checkedOut, service.createOrReuseRepositoryWorkspaceBranch(
            "branch-legacy",
            workspace.workspaceId(),
            new RepositoryWorkspaceBranchSelector("main", "")
        ));
        var replayedRefresh = service.refreshRepositoryWorkspaceBranch(
            "refresh-legacy",
            "schema-v1",
            "correlation-1",
            workspace.workspaceId(),
            branch.workspaceBranchId(),
            policy(),
            Map.of()
        );

        assertFalse(replayedRefresh.changed());
        assertEquals(checkedOut, replayedRefresh.branch());
        assertEquals("b".repeat(40), replayedRefresh.previousCommit());
    }

    @Test
    void rejectsMalformedIdempotencyPayloadsAndNormalizesOptionalPayloadFields() {
        var uncheckedBranch = new RepositoryWorkspaceBranch(
            new WorkspaceBranchId("workspace-branch-0001"),
            new WorkspaceId("workspace-0001"),
            "main",
            "",
            "",
            null,
            RepositoryWorkspaceBranchStatus.CHECKING_OUT,
            null,
            null,
            CLOCK.instant(),
            null
        );
        var checkedBranch = uncheckedBranch.checkedOut(
            "b".repeat(40),
            new SourceSnapshotId("source-snapshot-branch"),
            List.of(new SourceRoot("src/main/java", "java")),
            CLOCK.instant(),
            null
        );
        var cleanup = new CleanupRepositoryWorkspaceResult(
            new WorkspaceId("workspace-0001"),
            RepositoryWorkspaceStatus.CLEANED,
            null
        );
        var refresh = new RefreshRepositoryWorkspaceBranchResult(checkedBranch, true, null, null, null, null);

        var replayedBranch = RepositorySourceIdempotencyPayloads.branch(RepositorySourceIdempotencyPayloads.branch(uncheckedBranch));
        var replayedCleanup = RepositorySourceIdempotencyPayloads.cleanup(RepositorySourceIdempotencyPayloads.cleanup(cleanup));
        var replayedRefresh = RepositorySourceIdempotencyPayloads.refresh(RepositorySourceIdempotencyPayloads.refresh(refresh));

        assertEquals(null, replayedBranch.sourceSnapshotId());
        assertEquals(null, replayedBranch.lastCheckedAt());
        assertEquals(List.of(), replayedBranch.sourceRoots());
        assertEquals(List.of(), replayedBranch.diagnostics());
        assertEquals(List.of(), replayedCleanup.diagnostics());
        assertEquals("", replayedRefresh.previousCommit());
        assertEquals(null, replayedRefresh.previousSourceSnapshotId());
        assertEquals(List.of(), replayedRefresh.diagnostics());
        assertEquals(Map.of(), replayedRefresh.safeAttributes());
        assertThrows(IllegalStateException.class, () -> RepositorySourceIdempotencyPayloads.cleanup(""));
        assertThrows(IllegalStateException.class, () -> RepositorySourceIdempotencyPayloads.cleanup("cleanup\ttoo-short"));
        assertThrows(IllegalStateException.class, () -> RepositorySourceIdempotencyPayloads.workspace(""));
        assertThrows(IllegalStateException.class, () -> RepositorySourceIdempotencyPayloads.workspace("workspace\ttoo-short"));
        assertThrows(IllegalStateException.class, () -> RepositorySourceIdempotencyPayloads.branch("branch\ttoo-short"));
        assertThrows(IllegalStateException.class, () -> RepositorySourceIdempotencyPayloads.refresh("refresh\ttrue"));
        assertThrows(IllegalStateException.class, () -> RepositorySourceIdempotencyPayloads.refresh(
            "refresh\ttrue\t\t\t\t\nbranch\ttoo-short"
        ));
    }

    private static AnalysisRunId runId() {
        return new AnalysisRunId("run-1");
    }

    private static RepositoryReference repository() {
        return new RepositoryReference("https://example.com/acme/demo.git", "github", Map.of());
    }

    private static RepositoryIdentity repositoryIdentity() {
        return RepositoryIdentity.from(repository(), "main");
    }

    private static RevisionSelector revision() {
        return new RevisionSelector("main", true, "", false);
    }

    private static WorkspacePolicy policy() {
        return new WorkspacePolicy(true, true, false, false, 60, 100_000);
    }

    private static RepositoryWorkspaceApplicationService workspaceService(
        FixedRepositoryWorkspaceIdGenerator idGenerator
    ) {
        return workspaceService(idGenerator, new FakeWorkspacePort(), new SequencedCheckoutPort("b".repeat(40)), new FakeMetadataPort("main", true));
    }

    private static RepositoryWorkspaceApplicationService workspaceService(
        FixedRepositoryWorkspaceIdGenerator idGenerator,
        FakeWorkspacePort workspacePort,
        RepositoryCheckoutPort checkoutPort,
        RepositoryMetadataPort metadataPort
    ) {
        return workspaceService(
            new InMemoryRepositoryWorkspaceRepository(),
            idGenerator,
            new InMemoryRepositorySourceIdempotencyRepository(),
            workspacePort,
            checkoutPort,
            metadataPort
        );
    }

    private static RepositoryWorkspaceApplicationService workspaceService(
        InMemoryRepositoryWorkspaceRepository repository,
        FixedRepositoryWorkspaceIdGenerator idGenerator,
        InMemoryRepositorySourceIdempotencyRepository idempotencyRepository,
        FakeWorkspacePort workspacePort,
        RepositoryCheckoutPort checkoutPort,
        RepositoryMetadataPort metadataPort
    ) {
        return new RepositoryWorkspaceApplicationService(
            repository,
            idGenerator,
            idempotencyRepository,
            workspacePort,
            checkoutPort,
            metadataPort,
            CLOCK
        );
    }

    private static CheckoutResult checkoutResult(String resolvedCommit) {
        return checkoutResult("feature/workspace-ui", "", CheckoutStatus.CHECKED_OUT, resolvedCommit);
    }

    private static CheckoutResult checkoutResult(
        String requestedBranch,
        String requestedCommit,
        CheckoutStatus status,
        String resolvedCommit
    ) {
        return new CheckoutResult(
            status,
            repository().remoteUrl(),
            resolvedCommit,
            requestedBranch,
            requestedCommit,
            true,
            5,
            List.of(Diagnostic.info("OK", "checkout")),
            false,
            false,
            List.of(new SourceRoot("src/main/java", "java"))
        );
    }

    private static List<String> workspaceIds(List<RepositoryWorkspace> workspaces) {
        return workspaces.stream()
            .map(workspace -> workspace.workspaceId().value())
            .toList();
    }

    private static RepositoryWorkspace workspaceFixture(
        String workspaceId,
        String repositoryName,
        RepositoryWorkspaceStatus status,
        List<RepositoryWorkspaceBranch> branches
    ) {
        var repository = RepositoryIdentity.from(
            new RepositoryReference("https://example.com/acme/" + repositoryName + ".git", "github", Map.of()),
            "main"
        );
        return new RepositoryWorkspace(
            new WorkspaceId(workspaceId),
            new WorkspaceTitle(repositoryName),
            repository,
            status,
            CLOCK.instant(),
            CLOCK.instant(),
            branches,
            List.of(Diagnostic.info("REPOSITORY_WORKSPACE_READY", "Repository workspace is ready")),
            Map.of("tenant", "demo")
        );
    }

    private static RepositoryWorkspaceBranch branchFixture(
        String workspaceId,
        String workspaceBranchId,
        String repositoryBranch,
        RepositoryWorkspaceBranchStatus status
    ) {
        var checkedOut = status == RepositoryWorkspaceBranchStatus.CHECKED_OUT
            || status == RepositoryWorkspaceBranchStatus.UP_TO_DATE
            || status == RepositoryWorkspaceBranchStatus.UPDATED;
        return new RepositoryWorkspaceBranch(
            new WorkspaceBranchId(workspaceBranchId),
            new WorkspaceId(workspaceId),
            repositoryBranch,
            "",
            checkedOut ? "b".repeat(40) : "",
            checkedOut ? new SourceSnapshotId("source-snapshot-" + workspaceBranchId) : null,
            status,
            checkedOut ? List.of(new SourceRoot("src/main/java", "java")) : List.of(),
            checkedOut ? CLOCK.instant() : null,
            CLOCK.instant(),
            List.of(Diagnostic.info("BRANCH_STATE", "Repository branch state fixture"))
        );
    }

    private static final class FixedRepositoryWorkspaceIdGenerator implements RepositoryWorkspaceIdGenerator {
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

    private static final class FakeWorkspacePort implements RepositoryWorkspacePort {
        private int cleaned;
        private int branchCheckouts;

        @Override
        public PreparedWorkspace prepare(AnalysisRunId analysisRunId, WorkspacePolicy policy) {
            return new PreparedWorkspace(new WorkspaceId("workspace-" + analysisRunId.value()), Path.of("memory"));
        }

        @Override
        public PreparedWorkspace prepareBranchCheckout(
            WorkspaceId workspaceId,
            WorkspaceBranchId workspaceBranchId,
            WorkspacePolicy policy
        ) {
            branchCheckouts++;
            return new PreparedWorkspace(workspaceId, Path.of("memory", workspaceId.value(), "branches", workspaceBranchId.value()));
        }

        @Override
        public void cleanup(WorkspaceId workspaceId) {
            cleaned++;
        }

        @Override
        public void cleanupBranchCheckout(WorkspaceId workspaceId, WorkspaceBranchId workspaceBranchId) {
            cleaned++;
        }
    }

    private static final class FakeCheckoutPort implements RepositoryCheckoutPort {
        @Override
        public CheckoutResult checkout(
            PreparedWorkspace workspace,
            RepositoryReference repository,
            RevisionSelector revision,
            WorkspacePolicy policy
        ) {
            return new CheckoutResult(
                CheckoutStatus.CHECKED_OUT,
                repository.remoteUrl(),
                "b".repeat(40),
                revision.branch(),
                revision.commit(),
                true,
                5,
                List.of(Diagnostic.info("OK", "checkout")),
                false,
                false,
                List.of(new SourceRoot("src/main/java", "java"))
            );
        }
    }

    private static final class SequencedCheckoutPort implements RepositoryCheckoutPort {
        private final List<String> commits;
        private int calls;

        private SequencedCheckoutPort(String... commits) {
            this.commits = new ArrayList<>(List.of(commits));
        }

        @Override
        public CheckoutResult checkout(
            PreparedWorkspace workspace,
            RepositoryReference repository,
            RevisionSelector revision,
            WorkspacePolicy policy
        ) {
            calls++;
            var commit = commits.isEmpty() ? "b".repeat(40) : commits.removeFirst();
            return new CheckoutResult(
                CheckoutStatus.CHECKED_OUT,
                repository.remoteUrl(),
                commit,
                revision.branch(),
                revision.commit(),
                true,
                5,
                List.of(Diagnostic.info("OK", "checkout")),
                false,
                false,
                List.of(new SourceRoot("src/main/java", "java"))
            );
        }
    }

    private static final class CapturingExecutor implements Executor {
        private final List<Runnable> tasks = new ArrayList<>();

        @Override
        public void execute(Runnable command) {
            tasks.add(command);
        }

        private void runNext() {
            tasks.removeFirst().run();
        }
    }

    private static final class FakeMetadataPort implements RepositoryMetadataPort {
        private final String defaultBranch;
        private final boolean resolved;
        private final List<String> repositoryBranches;
        private int calls;

        private FakeMetadataPort(String defaultBranch, boolean resolved) {
            this(defaultBranch, resolved, resolved ? List.of(defaultBranch) : List.of());
        }

        private FakeMetadataPort(String defaultBranch, boolean resolved, List<String> repositoryBranches) {
            this.defaultBranch = defaultBranch;
            this.resolved = resolved;
            this.repositoryBranches = repositoryBranches;
        }

        @Override
        public RepositoryMetadataResolution resolveMetadata(
            RepositoryReference repository,
            RepositoryMetadataPreviewPolicy policy
        ) {
            calls++;
            return new RepositoryMetadataResolution(
                RepositoryIdentity.from(repository, defaultBranch),
                resolved,
                repositoryBranches,
                List.of(Diagnostic.info("DEFAULT_BRANCH_RESOLVED", "Repository default branch resolved"))
            );
        }
    }

    private static RepositorySourceIdempotencyRecord record(
        String operation,
        String idempotencyKey,
        String fingerprint,
        String resultType,
        String resultReference
    ) {
        return new RepositorySourceIdempotencyRecord(
            idempotencyKey,
            operation,
            fingerprint,
            resultType,
            resultReference,
            "",
            "COMPLETED",
            CLOCK.instant(),
            null
        );
    }
}
