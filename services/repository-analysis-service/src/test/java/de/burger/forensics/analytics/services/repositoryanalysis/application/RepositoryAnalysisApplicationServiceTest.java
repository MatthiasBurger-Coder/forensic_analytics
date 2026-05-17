package de.burger.forensics.analytics.services.repositoryanalysis.application;

import de.burger.forensics.analytics.services.repositoryanalysis.adapter.out.memory.InMemoryRepositoryPreparationRepository;
import de.burger.forensics.analytics.services.repositoryanalysis.application.port.PreparedWorkspace;
import de.burger.forensics.analytics.services.repositoryanalysis.application.port.RepositoryCheckoutPort;
import de.burger.forensics.analytics.services.repositoryanalysis.application.port.RepositoryWorkspacePort;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.CheckoutResult;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.CheckoutStatus;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.Diagnostic;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.RepositoryReference;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.RepositoryWorkspaceStatus;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.RevisionSelector;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceRoot;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceSnapshotId;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.WorkspaceId;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.WorkspacePolicy;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositoryAnalysisApplicationServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-16T10:15:30Z"), ZoneOffset.UTC);
    private final FakeWorkspacePort workspacePort = new FakeWorkspacePort();
    private final RepositoryAnalysisApplicationService service = new RepositoryAnalysisApplicationService(
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
        assertEquals(RepositoryWorkspaceStatus.CLEANED, cleaned.workspaceStatus());
        assertSame(cleaned, sameCleaned);
        assertEquals(1, workspacePort.cleaned);
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
        var failingService = new RepositoryAnalysisApplicationService(
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

    private static AnalysisRunId runId() {
        return new AnalysisRunId("run-1");
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
