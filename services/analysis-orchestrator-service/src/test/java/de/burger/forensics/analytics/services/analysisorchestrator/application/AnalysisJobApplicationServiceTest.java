package de.burger.forensics.analytics.services.analysisorchestrator.application;

import de.burger.forensics.analytics.services.analysisorchestrator.adapter.out.memory.InMemoryAnalysisJobRepository;
import de.burger.forensics.analytics.services.analysisorchestrator.domain.AnalysisArtifactCategory;
import de.burger.forensics.analytics.services.analysisorchestrator.domain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.analysisorchestrator.domain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.analysisorchestrator.domain.AnalysisJobId;
import de.burger.forensics.analytics.services.analysisorchestrator.domain.AnalysisJobState;
import de.burger.forensics.analytics.services.analysisorchestrator.domain.AnalysisRunId;
import de.burger.forensics.analytics.services.analysisorchestrator.domain.AnalysisWorkerKind;
import de.burger.forensics.analytics.services.analysisorchestrator.domain.ArtifactByteAccess;
import de.burger.forensics.analytics.services.analysisorchestrator.domain.ArtifactByteCustody;
import de.burger.forensics.analytics.services.analysisorchestrator.domain.ArtifactReference;
import de.burger.forensics.analytics.services.analysisorchestrator.domain.SourceSnapshotId;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnalysisJobApplicationServiceTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-05-16T10:15:30Z"), ZoneOffset.UTC);

    private final AnalysisJobApplicationService service = new AnalysisJobApplicationService(
        new InMemoryAnalysisJobRepository(),
        FIXED_CLOCK
    );

    @Test
    void submitsListsLeasesProgressesAndCompletesJob() {
        var submit = service.submit(
            "submit-key",
            "correlation-1",
            runId("run-1"),
            jobId("job-1"),
            "schema-v1",
            AnalysisWorkerKind.AST_ANALYSIS,
            snapshotId("snapshot-1"),
            List.of(artifact("input.json", "input-hash", AnalysisArtifactCategory.STATIC)),
            AnalysisCompleteness.UNKNOWN,
            Map.of("repository", "demo")
        );

        var sameSubmit = service.submit(
            "submit-key",
            "correlation-1",
            runId("run-1"),
            jobId("job-1"),
            "schema-v1",
            AnalysisWorkerKind.AST_ANALYSIS,
            snapshotId("snapshot-1"),
            List.of(artifact("input.json", "input-hash", AnalysisArtifactCategory.STATIC)),
            AnalysisCompleteness.UNKNOWN,
            Map.of("repository", "demo")
        );
        var listed = service.list(runId("run-1"), AnalysisWorkerKind.AST_ANALYSIS, AnalysisJobState.DISPATCHABLE);
        var lease = service.lease("lease-key", "correlation-1", "worker-a", AnalysisWorkerKind.AST_ANALYSIS, 60, 1);
        var progressed = service.progress("progress-key", "correlation-1", jobId("job-1"), 1, "worker-a", 50, List.of("halfway"));
        var completed = service.complete(
            "complete-key",
            "correlation-1",
            jobId("job-1"),
            1,
            "worker-a",
            List.of(artifact("output.json", "output-hash", AnalysisArtifactCategory.STATIC)),
            AnalysisCompleteness.COMPLETE,
            List.of("done")
        );

        assertSame(submit, sameSubmit);
        assertEquals("ACCEPTED", submit.status().code());
        assertEquals("schema-v1", submit.job().schemaVersion());
        assertEquals("correlation-1", submit.job().correlationId());
        assertEquals(Map.of("repository", "demo"), submit.job().attributes());
        assertEquals(List.of(submit.job()), listed);
        assertEquals(AnalysisJobState.RUNNING, lease.jobs().getFirst().state());
        assertEquals(1, lease.jobs().getFirst().attempt());
        assertEquals("worker-a", lease.jobs().getFirst().leaseOwner());
        assertEquals(Instant.parse("2026-05-16T10:16:30Z"), lease.jobs().getFirst().leaseExpiresAt());
        assertEquals(List.of("halfway"), progressed.diagnostics());
        assertEquals(50, progressed.percentComplete());
        assertEquals(AnalysisJobState.COMPLETED, completed.state());
        assertEquals(AnalysisCompleteness.COMPLETE, completed.completeness());
        assertEquals(100, completed.percentComplete());
        assertEquals("output.json", completed.outputArtifacts().getFirst().path());
    }

    @Test
    void recordsRetryableFailureAndDeadLetterFailure() {
        submit("job-retry", AnalysisWorkerKind.JOERN_ANALYSIS);
        service.lease("lease-retry", "correlation-1", "worker-a", AnalysisWorkerKind.JOERN_ANALYSIS, 60, 1);

        var retryable = service.fail(
            "fail-retry",
            "correlation-1",
            jobId("job-retry"),
            1,
            "worker-a",
            "temporary downstream timeout",
            List.of("timeout"),
            AnalysisCompleteness.UNKNOWN,
            true
        );
        var leasedAgain = service.lease("lease-again", "correlation-1", "worker-b", AnalysisWorkerKind.JOERN_ANALYSIS, 60, 1);

        submit("job-dead", AnalysisWorkerKind.REPOSITORY_ANALYSIS);
        service.lease("lease-dead", "correlation-1", "worker-c", AnalysisWorkerKind.REPOSITORY_ANALYSIS, 60, 1);
        var deadLettered = service.fail(
            "fail-dead",
            "correlation-1",
            jobId("job-dead"),
            1,
            "worker-c",
            "invalid immutable request",
            List.of("not retryable"),
            AnalysisCompleteness.INCOMPLETE,
            false
        );

        assertEquals(AnalysisJobState.RETRYABLE, retryable.state());
        assertEquals(1, retryable.failures().size());
        assertEquals(2, leasedAgain.jobs().getFirst().attempt());
        assertEquals("worker-b", leasedAgain.jobs().getFirst().leaseOwner());
        assertEquals(AnalysisJobState.DEAD_LETTERED, deadLettered.state());
        assertEquals("invalid immutable request", deadLettered.failures().getFirst().reason());
    }

    @Test
    void expiresTimedOutLeasesAsRetryableFailuresBeforeNextLease() {
        var clock = new MutableClock(Instant.parse("2026-05-16T11:00:00Z"));
        var timedService = new AnalysisJobApplicationService(new InMemoryAnalysisJobRepository(), clock);
        submit(timedService, "job-timeout", AnalysisWorkerKind.JOERN_ANALYSIS);
        var firstLease = timedService.lease("lease-timeout", "correlation-timeout", "worker-a", AnalysisWorkerKind.JOERN_ANALYSIS, 30, 1);

        clock.set(Instant.parse("2026-05-16T11:00:31Z"));
        var secondLease = timedService.lease("lease-after-timeout", "correlation-timeout", "worker-b", AnalysisWorkerKind.JOERN_ANALYSIS, 60, 1);
        var leasedAgain = secondLease.jobs().getFirst();

        assertEquals(Instant.parse("2026-05-16T11:00:30Z"), firstLease.jobs().getFirst().leaseExpiresAt());
        assertEquals(AnalysisJobState.RUNNING, leasedAgain.state());
        assertEquals(2, leasedAgain.attempt());
        assertEquals("worker-b", leasedAgain.leaseOwner());
        assertEquals(1, leasedAgain.failures().size());
        assertEquals(true, leasedAgain.failures().getFirst().retryable());
        assertEquals("worker lease expired before completion", leasedAgain.failures().getFirst().diagnostics().getFirst());
    }

    @Test
    void registersJobToArtifactReferencesWithoutReadingArtifactBytes() {
        submit("job-artifacts", AnalysisWorkerKind.REPORT);

        var registered = service.registerArtifacts(
            "register-key",
            "correlation-1",
            runId("run-1"),
            jobId("job-artifacts"),
            List.of(artifact("reports/run-1.json", "report-hash", AnalysisArtifactCategory.GENERATED))
        );
        var sameRegistered = service.registerArtifacts(
            "register-key",
            "correlation-1",
            runId("run-1"),
            jobId("job-artifacts"),
            List.of(artifact("reports/run-1.json", "report-hash", AnalysisArtifactCategory.GENERATED))
        );

        assertSame(registered, sameRegistered);
        assertEquals("ACCEPTED", registered.status().code());
        assertEquals("query-report-api-service", registered.artifacts().getFirst().byteAccess().ownerService());
        assertEquals("reports/run-1.json", registered.artifacts().getFirst().byteAccess().retrievalReference());
    }

    @Test
    void rejectsConflictingIdempotencyAndIllegalTransitions() {
        submit("job-1", AnalysisWorkerKind.AST_ANALYSIS);

        assertThrows(IdempotencyConflictException.class, () -> service.submit(
            "submit-job-1",
            "correlation-1",
            runId("run-1"),
            jobId("job-2"),
            "schema-v1",
            AnalysisWorkerKind.AST_ANALYSIS,
            snapshotId("snapshot-1"),
            List.of(),
            AnalysisCompleteness.UNKNOWN,
            Map.of()
        ));
        assertThrows(IllegalStateException.class, () -> service.progress(
            "progress-key",
            "correlation-1",
            jobId("job-1"),
            1,
            "worker-a",
            50,
            List.of()
        ));
        assertThrows(IllegalArgumentException.class, () -> service.lease(
            "lease-invalid",
            "correlation-1",
            "worker-a",
            AnalysisWorkerKind.AST_ANALYSIS,
            0,
            1
        ));
    }

    private void submit(String jobId, AnalysisWorkerKind workerKind) {
        submit(service, jobId, workerKind);
    }

    private static void submit(AnalysisJobApplicationService service, String jobId, AnalysisWorkerKind workerKind) {
        service.submit(
            "submit-" + jobId,
            "correlation-1",
            runId("run-1"),
            jobId(jobId),
            "schema-v1",
            workerKind,
            snapshotId("snapshot-1"),
            List.of(),
            AnalysisCompleteness.UNKNOWN,
            Map.of()
        );
    }

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> instant;

        private MutableClock(Instant instant) {
            this.instant = new AtomicReference<>(instant);
        }

        private void set(Instant nextInstant) {
            instant.set(nextInstant);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant.get();
        }
    }

    private static AnalysisRunId runId(String value) {
        return new AnalysisRunId(value);
    }

    private static AnalysisJobId jobId(String value) {
        return new AnalysisJobId(value);
    }

    private static SourceSnapshotId snapshotId(String value) {
        return new SourceSnapshotId(value);
    }

    private static AnalysisArtifactReference artifact(
        String path,
        String hash,
        AnalysisArtifactCategory category
    ) {
        return new AnalysisArtifactReference(
            new ArtifactReference(path, "application/json", hash, 42),
            category,
            "producer-service",
            "schema-v1",
            AnalysisCompleteness.COMPLETE,
            new ArtifactByteAccess(
                "query-report-api-service",
                "query-report-api-service.generated-reports.v1",
                path,
                ArtifactByteCustody.PRODUCER_RETAINED
            )
        );
    }
}
