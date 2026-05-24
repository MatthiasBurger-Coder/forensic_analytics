package de.burger.forensics.analytics.services.repositorysource.application;

import de.burger.forensics.analytics.services.repositorysource.adapter.out.memory.InMemoryRepositoryPreparationRepository;
import de.burger.forensics.analytics.services.repositorysource.adapter.out.memory.InMemoryRepositoryWorkspaceRepository;
import de.burger.forensics.analytics.services.repositorysource.application.port.PreparedWorkspace;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryCheckoutPort;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryWorkspaceIdGenerator;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryWorkspacePort;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryIdentity;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.CheckoutResult;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.CheckoutStatus;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.Diagnostic;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.BuildOutputProducer;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryReference;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceBranchSelector;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceBranchStatus;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceStatus;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RevisionSelector;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.SourceRoot;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.SourceSnapshotId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceBranchId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspacePolicy;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositorySourceApplicationServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-16T10:15:30Z"), ZoneOffset.UTC);
    private final FakeWorkspacePort workspacePort = new FakeWorkspacePort();
    private final RepositorySourceApplicationService service = new RepositorySourceApplicationService(
        new InMemoryRepositoryPreparationRepository(),
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

        assertSame(prepared, samePrepared);
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
        assertSame(cleaned, sameCleaned);
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

        assertSame(prepared, replayed);
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

        assertSame(workspace, replayed);
        assertEquals(workspace, reusedWithNewKey);
        assertEquals("workspace-0001", workspace.workspaceId().value());
        assertEquals("demo", workspace.workspaceTitle().value());
        assertEquals(1, idGenerator.workspaceIds);
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

        assertSame(branch, replayed);
        assertEquals("workspace-branch-0001", branch.workspaceBranchId().value());
        assertEquals("feature/workspace-ui", branch.repositoryBranch());
        assertEquals(RepositoryWorkspaceBranchStatus.CHECKING_OUT, started.status());
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
        return new RepositoryWorkspaceApplicationService(
            new InMemoryRepositoryWorkspaceRepository(),
            idGenerator,
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

        @Override
        public PreparedWorkspace prepare(AnalysisRunId analysisRunId, WorkspacePolicy policy) {
            return new PreparedWorkspace(new WorkspaceId("workspace-" + analysisRunId.value()), Path.of("memory"));
        }

        @Override
        public void cleanup(WorkspaceId workspaceId) {
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
}
