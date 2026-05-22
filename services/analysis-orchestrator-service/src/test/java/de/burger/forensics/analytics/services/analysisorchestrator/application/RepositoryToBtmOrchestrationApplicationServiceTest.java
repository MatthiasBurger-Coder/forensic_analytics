package de.burger.forensics.analytics.services.analysisorchestrator.application;

import de.burger.forensics.analytics.services.analysisorchestrator.application.RepositoryToBtmOrchestrationApplicationService.RepositoryToBtmStartCommand;
import de.burger.forensics.analytics.services.analysisorchestrator.domain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.analysisorchestrator.domain.AnalysisRunId;
import de.burger.forensics.analytics.services.analysisorchestrator.domain.BtmDeliveryReadiness;
import de.burger.forensics.analytics.services.analysisorchestrator.domain.RepositoryToBtmOrchestrationState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RepositoryToBtmOrchestrationApplicationServiceTest {
    private final RepositoryToBtmOrchestrationApplicationService service = new RepositoryToBtmOrchestrationApplicationService();

    @Test
    void startsRepositoryToBtmAsIncompletePendingStatusWithoutWorkerArtifacts() {
        var status = service.start(command("start-key", "run-1", "main"));
        var sameStatus = service.start(command("start-key", "run-1", "main"));

        assertSame(status, sameStatus);
        assertEquals(new AnalysisRunId("run-1"), status.analysisRunId());
        assertEquals("repository-analysis-", status.repositoryAnalysisJobId().value().substring(0, "repository-analysis-".length()));
        assertEquals("", status.sourceSnapshotId());
        assertEquals(AnalysisCompleteness.INCOMPLETE, status.completeness());
        assertEquals(RepositoryToBtmOrchestrationState.WAITING_FOR_REPOSITORY, status.state());
        assertEquals(BtmDeliveryReadiness.NOT_READY, status.btmDeliveryReadiness());
        assertEquals(true, status.joernSkipped());
        assertEquals("REPOSITORY_TO_BTM_WAITING_FOR_REPOSITORY", status.diagnostics().getFirst().code());
        assertEquals(true, status.diagnostics().getFirst().affectsCompleteness());
        assertEquals(status, service.get(new AnalysisRunId("run-1")));
    }

    @Test
    void rejectsConflictingIdempotentRepositoryToBtmStart() {
        service.start(command("start-key", "run-1", "main"));

        assertThrows(IdempotencyConflictException.class, () -> service.start(command("start-key", "run-1", "feature/parity")));
        assertThrows(
            RepositoryToBtmOrchestrationConflictException.class,
            () -> service.start(command("other-start-key", "run-1", "feature/parity"))
        );
        assertNull(service.get(new AnalysisRunId("missing-run")));
    }

    private static RepositoryToBtmStartCommand command(String idempotencyKey, String analysisRunId, String branch) {
        return new RepositoryToBtmStartCommand(
            idempotencyKey,
            "correlation-1",
            "schema-v1",
            new AnalysisRunId(analysisRunId),
            "https://example.test/repository.git",
            "git",
            branch,
            "",
            60,
            1_000_000,
            "gradle",
            "build-1",
            "demo",
            List.of("app"),
            List.of("REQUESTED_REPOSITORY_TO_BTM_OUTPUT_BTM_RULES"),
            Map.of("repository", "demo")
        );
    }
}
