package de.burger.forensics.analytics.services.analysisstore.application;

import de.burger.forensics.analytics.services.analysisstore.adapter.out.memory.InMemoryAnalysisJobRepository;
import de.burger.forensics.analytics.services.analysisstore.application.port.SourceFactArtifactByteVerifierPort;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactCategory;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJobId;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJobState;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisRunId;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisWorkerKind;
import de.burger.forensics.analytics.services.analysisstore.domain.ArtifactByteAccess;
import de.burger.forensics.analytics.services.analysisstore.domain.ArtifactByteCustody;
import de.burger.forensics.analytics.services.analysisstore.domain.ArtifactReference;
import de.burger.forensics.analytics.services.analysisstore.domain.SourceSnapshotId;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertEquals(List.of("halfway"), progressed.diagnostics());
        assertEquals(50, progressed.percentComplete());
        assertEquals(AnalysisJobState.COMPLETED, completed.state());
        assertEquals(AnalysisCompleteness.COMPLETE, completed.completeness());
        assertEquals(100, completed.percentComplete());
        assertEquals("output.json", completed.outputArtifacts().getFirst().path());
    }

    @Test
    void rejectsConflictingIdempotencyAndIllegalTransitions() {
        service.submit(
            "submit-key",
            "correlation-1",
            runId("run-1"),
            jobId("job-1"),
            "schema-v1",
            AnalysisWorkerKind.AST_ANALYSIS,
            snapshotId("snapshot-1"),
            List.of(),
            AnalysisCompleteness.UNKNOWN,
            Map.of()
        );

        assertThrows(IdempotencyConflictException.class, () -> service.submit(
            "submit-key",
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
        assertThrows(AnalysisJobNotFoundException.class, () -> service.get(jobId("missing")));
        assertThrows(IllegalStateException.class, () -> service.progress(
            "progress-key",
            "correlation-1",
            jobId("job-1"),
            1,
            "worker-a",
            10,
            List.of()
        ));
        assertThrows(IllegalArgumentException.class, () -> service.lease(
            "lease-key",
            "correlation-1",
            "worker-a",
            AnalysisWorkerKind.AST_ANALYSIS,
            0,
            1
        ));
        assertThrows(IllegalArgumentException.class, () -> service.lease(
            " ",
            "correlation-1",
            "worker-a",
            AnalysisWorkerKind.AST_ANALYSIS,
            60,
            1
        ));
    }

    @Test
    void failAndRegisterArtifactsPreserveMetadataAndRejectConflicts() {
        service.submit(
            "submit-key",
            "correlation-1",
            runId("run-1"),
            jobId("job-1"),
            "schema-v1",
            AnalysisWorkerKind.JOERN_ANALYSIS,
            snapshotId("snapshot-1"),
            List.of(),
            AnalysisCompleteness.INCOMPLETE,
            Map.of()
        );
        service.lease("lease-key", "correlation-1", "worker-a", AnalysisWorkerKind.JOERN_ANALYSIS, 60, 1);
        var failed = service.fail(
            "fail-key",
            "correlation-1",
            jobId("job-1"),
            1,
            "worker-a",
            "joern unavailable",
            List.of("container missing"),
            AnalysisCompleteness.INCOMPLETE,
            true
        );
        var registered = service.registerArtifacts(
            "register-key",
            "correlation-1",
            runId("run-1"),
            jobId("job-1"),
            List.of(artifact("diagnostic.json", "diagnostic-hash", AnalysisArtifactCategory.PROJECTION))
        );
        var sameArtifact = service.registerArtifacts(
            "register-same-key",
            "correlation-1",
            runId("run-1"),
            jobId("job-1"),
            List.of(artifact("diagnostic.json", "diagnostic-hash", AnalysisArtifactCategory.PROJECTION))
        );

        assertEquals(AnalysisJobState.RETRYABLE, failed.state());
        assertEquals(1, failed.failures().size());
        assertFalse(failed.failures().getFirst().diagnostics().isEmpty());
        assertEquals("diagnostic.json", registered.artifacts().getFirst().path());
        assertEquals(registered.artifacts(), sameArtifact.artifacts());
        assertThrows(IllegalArgumentException.class, () -> service.registerArtifacts(
            "register-conflict-key",
            "correlation-1",
            runId("run-1"),
            jobId("job-1"),
            List.of(artifact("diagnostic.json", "different-hash", AnalysisArtifactCategory.PROJECTION))
        ));
        assertThrows(IllegalArgumentException.class, () -> service.registerArtifacts(
            "register-wrong-run",
            "correlation-1",
            runId("other-run"),
            jobId("job-1"),
            List.of(artifact("other.json", "other-hash", AnalysisArtifactCategory.PROJECTION))
        ));
    }

    @Test
    void verifiesJavaAstSourceFactBytesBeforeSubmittingInputArtifacts() {
        var verifier = new CapturingSourceFactByteVerifier();
        var verifiedService = new AnalysisJobApplicationService(
            new InMemoryAnalysisJobRepository(),
            FIXED_CLOCK,
            verifier
        );

        var submit = verifiedService.submit(
            "submit-key",
            "correlation-1",
            runId("run-1"),
            jobId("job-1"),
            "schema-v1",
            AnalysisWorkerKind.AST_ANALYSIS,
            snapshotId("snapshot-1"),
            List.of(javaAstSourceFactArtifact()),
            AnalysisCompleteness.UNKNOWN,
            Map.of("tenant", "demo")
        );

        assertEquals("run-1", verifier.analysisRunId.value());
        assertEquals("job-1", verifier.analysisJobId.value());
        assertEquals("snapshot-1", verifier.sourceSnapshotId.value());
        assertEquals("verify-source-fact-bytes:java-ast/source-facts.json", verifier.requestId);
        assertEquals("correlation-1", verifier.correlationId);
        assertEquals("java-ast/source-facts.json", verifier.artifact.path());
        assertEquals(Map.of("tenant", "demo"), verifier.safeAttributes);
        assertEquals("java-ast-owner-schema", submit.job().inputArtifacts().getFirst().schemaVersion());
    }

    @Test
    void verifiesJavaAstSourceFactBytesBeforeCompletingOutputArtifacts() {
        var verifier = new CapturingSourceFactByteVerifier();
        var verifiedService = new AnalysisJobApplicationService(
            new InMemoryAnalysisJobRepository(),
            FIXED_CLOCK,
            verifier
        );
        verifiedService.submit(
            "submit-key",
            "correlation-1",
            runId("run-1"),
            jobId("job-1"),
            "schema-v1",
            AnalysisWorkerKind.AST_ANALYSIS,
            snapshotId("snapshot-1"),
            List.of(),
            AnalysisCompleteness.UNKNOWN,
            Map.of("tenant", "demo")
        );
        verifiedService.lease("lease-key", "correlation-1", "worker-a", AnalysisWorkerKind.AST_ANALYSIS, 60, 1);

        var completed = verifiedService.complete(
            "complete-key",
            "correlation-1",
            jobId("job-1"),
            1,
            "worker-a",
            List.of(javaAstSourceFactArtifact()),
            AnalysisCompleteness.COMPLETE,
            List.of("done")
        );

        assertEquals("run-1", verifier.analysisRunId.value());
        assertEquals("job-1", verifier.analysisJobId.value());
        assertEquals("snapshot-1", verifier.sourceSnapshotId.value());
        assertEquals("verify-source-fact-bytes:java-ast/source-facts.json", verifier.requestId);
        assertEquals("correlation-1", verifier.correlationId);
        assertEquals("java-ast/source-facts.json", verifier.artifact.path());
        assertEquals(Map.of("tenant", "demo"), verifier.safeAttributes);
        assertEquals("java-ast-owner-schema", completed.outputArtifacts().getFirst().schemaVersion());
    }

    @Test
    void verifiesJavaAstSourceFactBytesBeforeRegisteringArtifacts() {
        var verifier = new CapturingSourceFactByteVerifier();
        var verifiedService = new AnalysisJobApplicationService(
            new InMemoryAnalysisJobRepository(),
            FIXED_CLOCK,
            verifier
        );
        verifiedService.submit(
            "submit-key",
            "correlation-1",
            runId("run-1"),
            jobId("job-1"),
            "schema-v1",
            AnalysisWorkerKind.AST_ANALYSIS,
            snapshotId("snapshot-1"),
            List.of(),
            AnalysisCompleteness.UNKNOWN,
            Map.of("tenant", "demo")
        );

        var registered = verifiedService.registerArtifacts(
            "register-key",
            "correlation-1",
            runId("run-1"),
            jobId("job-1"),
            List.of(javaAstSourceFactArtifact())
        );

        assertEquals("run-1", verifier.analysisRunId.value());
        assertEquals("job-1", verifier.analysisJobId.value());
        assertEquals("snapshot-1", verifier.sourceSnapshotId.value());
        assertEquals("verify-source-fact-bytes:java-ast/source-facts.json", verifier.requestId);
        assertEquals("correlation-1", verifier.correlationId);
        assertEquals("java-ast/source-facts.json", verifier.artifact.path());
        assertEquals(Map.of("tenant", "demo"), verifier.safeAttributes);
        assertEquals("java-ast-owner-schema", registered.artifacts().getFirst().schemaVersion());
    }

    @Test
    void unavailableSourceFactByteVerifierRejectsDirectVerification() {
        var unavailable = SourceFactArtifactByteVerifierPort.unavailable();

        var failure = assertThrows(IllegalStateException.class, () -> unavailable.verify(
            runId("run-1"),
            jobId("job-1"),
            snapshotId("snapshot-1"),
            "request-1",
            "correlation-1",
            javaAstSourceFactArtifact(),
            Map.of()
        ));

        assertTrue(unavailable.supports(javaAstSourceFactArtifact()));
        assertFalse(unavailable.supports(artifact("diagnostic.json", "diagnostic-hash", AnalysisArtifactCategory.PROJECTION)));
        assertEquals("Source fact artifact byte verifier is not available", failure.getMessage());
    }

    @Test
    void defaultVerifierRejectsJavaAstSourceFactArtifactsInsteadOfSkippingVerification() {
        assertThrows(IllegalStateException.class, () -> service.submit(
            "submit-source-fact-key",
            "correlation-1",
            runId("run-1"),
            jobId("job-source-fact"),
            "schema-v1",
            AnalysisWorkerKind.AST_ANALYSIS,
            snapshotId("snapshot-1"),
            List.of(javaAstSourceFactArtifact()),
            AnalysisCompleteness.UNKNOWN,
            Map.of()
        ));
        service.submit(
            "submit-complete-key",
            "correlation-1",
            runId("run-1"),
            jobId("job-complete"),
            "schema-v1",
            AnalysisWorkerKind.BTM_GENERATION,
            snapshotId("snapshot-1"),
            List.of(),
            AnalysisCompleteness.UNKNOWN,
            Map.of()
        );
        service.lease("lease-complete-key", "correlation-1", "worker-a", AnalysisWorkerKind.BTM_GENERATION, 60, 1);
        var completeFailure = assertThrows(IllegalStateException.class, () -> service.complete(
            "complete-source-fact-key",
            "correlation-1",
            jobId("job-complete"),
            1,
            "worker-a",
            List.of(javaAstSourceFactArtifact()),
            AnalysisCompleteness.COMPLETE,
            List.of("done")
        ));
        var afterCompleteFailure = service.get(jobId("job-complete"));

        assertEquals("Source fact artifact byte verifier is not available", completeFailure.getMessage());
        assertEquals(AnalysisJobState.RUNNING, afterCompleteFailure.state());
        assertTrue(afterCompleteFailure.outputArtifacts().isEmpty());

        service.submit(
            "submit-key",
            "correlation-1",
            runId("run-1"),
            jobId("job-1"),
            "schema-v1",
            AnalysisWorkerKind.AST_ANALYSIS,
            snapshotId("snapshot-1"),
            List.of(),
            AnalysisCompleteness.UNKNOWN,
            Map.of()
        );

        var failure = assertThrows(IllegalStateException.class, () -> service.registerArtifacts(
            "register-key",
            "correlation-1",
            runId("run-1"),
            jobId("job-1"),
            List.of(javaAstSourceFactArtifact())
        ));

        assertEquals("Source fact artifact byte verifier is not available", failure.getMessage());
    }

    @Test
    void concurrentWorkersLeaseAJobOnlyOnceAndRetryableJobsCanBeLeasedAgain() throws Exception {
        service.submit(
            "submit-key",
            "correlation-1",
            runId("run-1"),
            jobId("job-1"),
            "schema-v1",
            AnalysisWorkerKind.AST_ANALYSIS,
            snapshotId("snapshot-1"),
            List.of(),
            AnalysisCompleteness.UNKNOWN,
            Map.of()
        );

        try (var executor = Executors.newFixedThreadPool(8)) {
            var ready = new CountDownLatch(8);
            var start = new CountDownLatch(1);
            var futures = java.util.stream.IntStream.range(0, 8)
                .mapToObj(index -> executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return service.lease(
                        "lease-key-" + index,
                        "correlation-1",
                        "worker-" + index,
                        AnalysisWorkerKind.AST_ANALYSIS,
                        60,
                        1
                    ).jobs();
                }))
                .toList();

            ready.await();
            start.countDown();

            var leasedJobs = 0;
            for (var future : futures) {
                leasedJobs += future.get().size();
            }
            assertEquals(1, leasedJobs);
        }

        var running = service.get(jobId("job-1"));
        var failed = service.fail(
            "fail-key",
            "correlation-1",
            jobId("job-1"),
            running.attempt(),
            running.leaseOwner(),
            "temporary failure",
            List.of("retry"),
            AnalysisCompleteness.INCOMPLETE,
            true
        );
        var retryLease = service.lease("retry-lease-key", "correlation-1", "worker-retry", AnalysisWorkerKind.AST_ANALYSIS, 60, 1);

        assertEquals(AnalysisJobState.RETRYABLE, failed.state());
        assertEquals(1, retryLease.jobs().size());
        assertEquals(AnalysisJobState.RUNNING, retryLease.jobs().getFirst().state());
        assertTrue(retryLease.jobs().getFirst().attempt() > running.attempt());
    }

    static AnalysisRunId runId(String value) {
        return new AnalysisRunId(value);
    }

    static AnalysisJobId jobId(String value) {
        return new AnalysisJobId(value);
    }

    static SourceSnapshotId snapshotId(String value) {
        return new SourceSnapshotId(value);
    }

    static AnalysisArtifactReference artifact(
        String path,
        String sha256,
        AnalysisArtifactCategory category
    ) {
        return new AnalysisArtifactReference(
            new ArtifactReference(path, "application/json", sha256, 42),
            category,
            "analysis-store-test",
            "schema-v1",
            AnalysisCompleteness.UNKNOWN,
            new ArtifactByteAccess(
                "analysis-store-test",
                "analysis-job.v1.ArtifactBytes",
                "artifacts/" + path,
                ArtifactByteCustody.PRODUCER_RETAINED
            )
        );
    }

    static AnalysisArtifactReference javaAstSourceFactArtifact() {
        return new AnalysisArtifactReference(
            new ArtifactReference(
                "java-ast/source-facts.json",
                "application/vnd.forensic-analytics.java-ast-source-facts.v1+json",
                "a".repeat(64),
                42
            ),
            AnalysisArtifactCategory.STATIC,
            "java-ast-analysis-service",
            "java-ast-analysis-v1",
            AnalysisCompleteness.COMPLETE,
            new ArtifactByteAccess(
                "java-ast-analysis-service",
                "java-ast-analysis.v1.JavaAstAnalysisService.GetSourceFactArtifactBytes",
                "java-ast/source-facts.json",
                ArtifactByteCustody.PRODUCER_RETAINED
            )
        );
    }

    private static final class CapturingSourceFactByteVerifier implements SourceFactArtifactByteVerifierPort {
        private AnalysisRunId analysisRunId;
        private AnalysisJobId analysisJobId;
        private SourceSnapshotId sourceSnapshotId;
        private String requestId;
        private String correlationId;
        private AnalysisArtifactReference artifact;
        private Map<String, String> safeAttributes;

        @Override
        public boolean supports(AnalysisArtifactReference artifact) {
            return "java-ast-analysis-service".equals(artifact.byteAccess().ownerService());
        }

        @Override
        public AnalysisArtifactReference verify(
            AnalysisRunId analysisRunId,
            AnalysisJobId analysisJobId,
            SourceSnapshotId sourceSnapshotId,
            String requestId,
            String correlationId,
            AnalysisArtifactReference artifact,
            Map<String, String> safeAttributes
        ) {
            this.analysisRunId = analysisRunId;
            this.analysisJobId = analysisJobId;
            this.sourceSnapshotId = sourceSnapshotId;
            this.requestId = requestId;
            this.correlationId = correlationId;
            this.artifact = artifact;
            this.safeAttributes = Map.copyOf(safeAttributes);
            return new AnalysisArtifactReference(
                artifact.artifact(),
                artifact.category(),
                artifact.producerService(),
                "java-ast-owner-schema",
                artifact.completeness(),
                artifact.byteAccess()
            );
        }
    }
}
