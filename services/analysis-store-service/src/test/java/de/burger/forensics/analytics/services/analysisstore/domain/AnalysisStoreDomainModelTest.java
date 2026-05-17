package de.burger.forensics.analytics.services.analysisstore.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisStoreDomainModelTest {
    @Test
    void identifiersAndArtifactsValidateRequiredEvidenceMetadata() {
        assertEquals("run-1", new AnalysisRunId(" run-1 ").value());
        assertEquals("job-1", new AnalysisJobId(" job-1 ").value());
        assertEquals("snapshot-1", new SourceSnapshotId(" snapshot-1 ").value());

        assertThrows(IllegalArgumentException.class, () -> new AnalysisRunId(null));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisRunId(" "));
        assertThrows(IllegalArgumentException.class, () -> new ArtifactReference(" ", "application/json", "sha", 1));
        assertThrows(IllegalArgumentException.class, () -> new ArtifactReference("artifact.json", "application/json", "sha", -1));
        assertThrows(NullPointerException.class, () -> new AnalysisArtifactReference(
            artifactReference("artifact.json", "sha"),
            null,
            "producer",
            "schema-v1",
            AnalysisCompleteness.UNKNOWN
        ));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisJobFailure(
            new AnalysisJobId("job-1"),
            AnalysisWorkerKind.AST_ANALYSIS,
            0,
            "failure",
            List.of(),
            AnalysisCompleteness.INCOMPLETE,
            true
        ));
    }

    @Test
    void jobTransitionsPreserveCompletenessAndRejectInvalidWorkers() {
        var submitted = AnalysisJob.submitted(
            new AnalysisRunId("run-1"),
            new AnalysisJobId("job-1"),
            "schema-v1",
            "correlation-1",
            AnalysisWorkerKind.AST_ANALYSIS,
            new SourceSnapshotId("snapshot-1"),
            List.of(artifact("input.json", "input-sha")),
            AnalysisCompleteness.UNKNOWN,
            Instant.parse("2026-05-16T10:00:00Z"),
            Map.of("repository", "demo")
        );
        var leased = submitted.leased("worker-a", 60, Instant.parse("2026-05-16T10:01:00Z"));
        var failed = leased.failed(
            "worker-a",
            1,
            "temporary failure",
            List.of("retry later"),
            AnalysisCompleteness.INCOMPLETE,
            true,
            Instant.parse("2026-05-16T10:02:00Z")
        );

        assertEquals(AnalysisJobState.DISPATCHABLE, submitted.state());
        assertEquals("schema-v1", submitted.schemaVersion());
        assertEquals("correlation-1", submitted.correlationId());
        assertEquals(Map.of("repository", "demo"), submitted.attributes());
        assertEquals(AnalysisJobState.RUNNING, leased.state());
        assertEquals(AnalysisJobState.RETRYABLE, failed.state());
        assertEquals(AnalysisCompleteness.INCOMPLETE, failed.completeness());
        assertTrue(submitted.matches(new AnalysisRunId("run-1"), AnalysisWorkerKind.AST_ANALYSIS, AnalysisJobState.DISPATCHABLE));
        assertTrue(submitted.matches(null, null, null));
        assertTrue(!submitted.matches(new AnalysisRunId("run-2"), AnalysisWorkerKind.AST_ANALYSIS, AnalysisJobState.DISPATCHABLE));
        assertTrue(!submitted.matches(new AnalysisRunId("run-1"), AnalysisWorkerKind.JOERN_ANALYSIS, AnalysisJobState.DISPATCHABLE));
        assertTrue(!submitted.matches(new AnalysisRunId("run-1"), AnalysisWorkerKind.AST_ANALYSIS, AnalysisJobState.COMPLETED));
        assertThrows(IllegalStateException.class, () -> submitted.completed(
            "worker-a",
            1,
            List.of(),
            AnalysisCompleteness.COMPLETE,
            List.of(),
            Instant.parse("2026-05-16T10:03:00Z")
        ));
        assertThrows(IllegalStateException.class, () -> leased.progressed(
            "worker-b",
            1,
            10,
            List.of(),
            Instant.parse("2026-05-16T10:03:00Z")
        ));
        assertThrows(IllegalStateException.class, () -> leased.progressed(
            "worker-a",
            2,
            10,
            List.of(),
            Instant.parse("2026-05-16T10:03:00Z")
        ));
        assertThrows(IllegalArgumentException.class, () -> leased.progressed(
            "worker-a",
            1,
            101,
            List.of(),
            Instant.parse("2026-05-16T10:03:00Z")
        ));
        assertThrows(IllegalArgumentException.class, () -> leased.progressed(
            "worker-a",
            1,
            -1,
            List.of(),
            Instant.parse("2026-05-16T10:03:00Z")
        ));
        assertThrows(IllegalArgumentException.class, () -> submitted.leased(
            "worker-a",
            0,
            Instant.parse("2026-05-16T10:03:00Z")
        ));
        assertThrows(IllegalStateException.class, () -> leased.leased(
            "worker-a",
            60,
            Instant.parse("2026-05-16T10:03:00Z")
        ));

        var retried = failed.leased("worker-a", 60, Instant.parse("2026-05-16T10:03:00Z"));
        var permanentFailure = retried.failed(
            "worker-a",
            2,
            "permanent failure",
            List.of("manual review"),
            AnalysisCompleteness.INCOMPLETE,
            false,
            Instant.parse("2026-05-16T10:04:00Z")
        );

        assertEquals(AnalysisJobState.RUNNING, retried.state());
        assertEquals(2, retried.attempt());
        assertEquals(AnalysisJobState.FAILED, permanentFailure.state());
    }

    @Test
    void completedJobMergesEquivalentArtifactsAndRejectsConflictingArtifacts() {
        var leased = AnalysisJob.submitted(
            new AnalysisRunId("run-1"),
            new AnalysisJobId("job-1"),
            "schema-v1",
            "correlation-1",
            AnalysisWorkerKind.AST_ANALYSIS,
            new SourceSnapshotId("snapshot-1"),
            List.of(),
            AnalysisCompleteness.UNKNOWN,
            Instant.parse("2026-05-16T10:00:00Z"),
            Map.of()
        ).leased("worker-a", 60, Instant.parse("2026-05-16T10:01:00Z"));
        var artifact = artifact("output.json", "sha");
        var completed = leased.completed(
            "worker-a",
            1,
            List.of(artifact, artifact),
            AnalysisCompleteness.COMPLETE,
            List.of("done"),
            Instant.parse("2026-05-16T10:02:00Z")
        );

        assertEquals(1, completed.outputArtifacts().size());
        assertThrows(IllegalArgumentException.class, () -> leased.completed(
            "worker-a",
            1,
            List.of(artifact, artifact("output.json", "different-sha")),
            AnalysisCompleteness.COMPLETE,
            List.of("done"),
            Instant.parse("2026-05-16T10:02:00Z")
        ));
    }

    @Test
    void jobConstructorNormalizesLeaseOwnerAndRejectsInvalidAttempts() {
        var now = Instant.parse("2026-05-16T10:00:00Z");
        var artifact = artifact("existing.json", "sha");

        var job = new AnalysisJob(
            new AnalysisRunId("run-1"),
            new AnalysisJobId("job-1"),
            "schema-v1",
            "correlation-1",
            AnalysisWorkerKind.REPORT,
            new SourceSnapshotId("snapshot-1"),
            List.of(),
            List.of(artifact),
            AnalysisCompleteness.UNKNOWN,
            AnalysisJobState.RETRYABLE,
            1,
            0,
            List.of(),
            null,
            now.plusSeconds(60),
            now,
            now,
            List.of(),
            Map.of("z", "last", "a", "first")
        );
        var completed = job.leased("worker-a", 60, now.plusSeconds(1)).completed(
            "worker-a",
            2,
            List.of(artifact, artifact("new.json", "new-sha")),
            AnalysisCompleteness.COMPLETE,
            List.of("done"),
            now.plusSeconds(2)
        );

        assertEquals("", job.leaseOwner());
        assertEquals(List.of("a", "z"), List.copyOf(job.attributes().keySet()));
        assertEquals(2, completed.outputArtifacts().size());
        assertThrows(IllegalArgumentException.class, () -> new AnalysisJob(
            new AnalysisRunId("run-1"),
            new AnalysisJobId("job-1"),
            "schema-v1",
            "correlation-1",
            AnalysisWorkerKind.REPORT,
            new SourceSnapshotId("snapshot-1"),
            List.of(),
            List.of(),
            AnalysisCompleteness.UNKNOWN,
            AnalysisJobState.RUNNING,
            -1,
            0,
            List.of(),
            "worker-a",
            now.plusSeconds(60),
            now,
            now,
            List.of(),
            Map.of()
        ));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisJob(
            new AnalysisRunId("run-1"),
            new AnalysisJobId("job-1"),
            "schema-v1",
            "correlation-1",
            AnalysisWorkerKind.REPORT,
            new SourceSnapshotId("snapshot-1"),
            List.of(),
            List.of(),
            AnalysisCompleteness.UNKNOWN,
            AnalysisJobState.RUNNING,
            1,
            -1,
            List.of(),
            "worker-a",
            now.plusSeconds(60),
            now,
            now,
            List.of(),
            Map.of()
        ));
    }

    private static AnalysisArtifactReference artifact(String path, String sha256) {
        return new AnalysisArtifactReference(
            artifactReference(path, sha256),
            AnalysisArtifactCategory.STATIC,
            "java-ast-analysis-service",
            "schema-v1",
            AnalysisCompleteness.UNKNOWN
        );
    }

    private static ArtifactReference artifactReference(String path, String sha256) {
        return new ArtifactReference(path, "application/json", sha256, 42);
    }
}
