package de.burger.forensics.analytics.domain.analysis;

import de.burger.forensics.analytics.domain.artifact.ArtifactReference;
import de.burger.forensics.analytics.domain.repository.SourceSnapshotId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisJobLifecycleTest {
    @Test
    void lifecycleStatesUseTheApprovedQueueNeutralNames() {
        assertEquals(
            List.of("ACCEPTED", "DISPATCHABLE", "RUNNING", "RETRYABLE", "FAILED", "DEAD_LETTERED", "COMPLETED"),
            List.of(AnalysisJobState.values()).stream().map(Enum::name).toList()
        );
    }

    @Test
    void transitionsAcceptTheDocumentedLifecyclePath() {
        var accepted = job();
        var dispatchable = accepted.dispatchable();
        var running = dispatchable.running(1);
        var completed = running.completed();

        assertEquals(AnalysisJobState.ACCEPTED, accepted.state());
        assertEquals(AnalysisJobState.DISPATCHABLE, dispatchable.state());
        assertEquals(AnalysisJobState.RUNNING, running.state());
        assertEquals(1, running.attempt());
        assertEquals(AnalysisJobState.COMPLETED, completed.state());
        assertTrue(AnalysisJobState.RUNNING.canTransitionTo(AnalysisJobState.COMPLETED));
        assertFalse(AnalysisJobState.COMPLETED.canTransitionTo(AnalysisJobState.RUNNING));
    }

    @Test
    void invalidTransitionsAreRejected() {
        var accepted = job();

        assertThrows(NullPointerException.class, () -> AnalysisJobState.ACCEPTED.canTransitionTo(null));
        assertThrows(IllegalStateException.class, accepted::completed);
        assertThrows(IllegalStateException.class, () -> accepted.running(1));
        assertThrows(IllegalArgumentException.class, () -> accepted.dispatchable().running(0));
        assertThrows(IllegalStateException.class, () -> accepted.dispatchable().running(1).completed().dispatchable());
    }

    @Test
    void retryableJobsPreserveFailureProvenanceAndCanBeDispatchedAgain() {
        var running = job().dispatchable().running(1);
        var failure = failure(1, "joern output missing required call graph artifact");

        var retryable = running.retryable(failure);
        var nextAttempt = retryable.dispatchable().running(2);

        assertEquals(AnalysisJobState.RETRYABLE, retryable.state());
        assertEquals(List.of(failure), retryable.failures());
        assertEquals(2, nextAttempt.attempt());
        assertEquals(AnalysisJobState.RUNNING, nextAttempt.state());
    }

    @Test
    void deadLetteredJobsPreserveFinalFailureInputReferencesAndCompleteness() {
        var failed = job()
            .dispatchable()
            .running(1)
            .failed(failure(1, "worker failed without producing canonical artifact"));
        var deadLetter = new DeadLetterProvenance(
            jobId(),
            AnalysisWorkerKind.JOERN_ANALYSIS,
            1,
            failed.failures().getFirst(),
            inputArtifacts(),
            AnalysisCompleteness.INCOMPLETE
        );

        var deadLettered = failed.deadLettered(deadLetter);

        assertEquals(AnalysisJobState.DEAD_LETTERED, deadLettered.state());
        assertEquals(deadLetter, deadLettered.deadLetterProvenance().orElseThrow());
        assertThrows(IllegalStateException.class, deadLettered::dispatchable);
    }

    @Test
    void failureAndDeadLetterProvenanceMustMatchTheJob() {
        var running = job().dispatchable().running(1);
        var failed = running.failed(failure(1, "worker failed"));
        var otherJobFailure = new AnalysisJobFailure(
            new AnalysisJobId("other-job"),
            AnalysisWorkerKind.JOERN_ANALYSIS,
            1,
            "other job failed",
            List.of("diagnostic"),
            AnalysisCompleteness.INCOMPLETE
        );
        var otherWorkerFailure = new AnalysisJobFailure(
            jobId(),
            AnalysisWorkerKind.AST_ANALYSIS,
            1,
            "other worker failed",
            List.of("diagnostic"),
            AnalysisCompleteness.INCOMPLETE
        );
        var otherAttemptFailure = failure(2, "other attempt failed");

        assertThrows(IllegalArgumentException.class, () -> running.retryable(otherJobFailure));
        assertThrows(IllegalArgumentException.class, () -> running.retryable(otherWorkerFailure));
        assertThrows(IllegalArgumentException.class, () -> running.retryable(otherAttemptFailure));
        assertThrows(NullPointerException.class, () -> running.failed(null));
        assertThrows(NullPointerException.class, () -> failed.deadLettered(null));
        assertThrows(
            IllegalArgumentException.class,
            () -> failed.deadLettered(new DeadLetterProvenance(
                new AnalysisJobId("other-job"),
                AnalysisWorkerKind.JOERN_ANALYSIS,
                1,
                failed.failures().getFirst(),
                inputArtifacts(),
                AnalysisCompleteness.INCOMPLETE
            ))
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> failed.deadLettered(new DeadLetterProvenance(
                jobId(),
                AnalysisWorkerKind.AST_ANALYSIS,
                1,
                failed.failures().getFirst(),
                inputArtifacts(),
                AnalysisCompleteness.INCOMPLETE
            ))
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> failed.deadLettered(new DeadLetterProvenance(
                jobId(),
                AnalysisWorkerKind.JOERN_ANALYSIS,
                1,
                failed.failures().getFirst(),
                List.of(new AnalysisArtifactReference(
                    new ArtifactReference("other.tar", "source-snapshot", "sha256:other", 128L),
                    AnalysisArtifactCategory.STATIC
                )),
                AnalysisCompleteness.INCOMPLETE
            ))
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> failed.deadLettered(new DeadLetterProvenance(
                jobId(),
                AnalysisWorkerKind.JOERN_ANALYSIS,
                1,
                failed.failures().getFirst(),
                inputArtifacts(),
                AnalysisCompleteness.COMPLETE
            ))
        );
    }

    @Test
    void lifecycleRecordsRejectInconsistentStateProvenance() {
        var failure = failure(1, "worker failed");
        var deadLetter = new DeadLetterProvenance(
            jobId(),
            AnalysisWorkerKind.JOERN_ANALYSIS,
            1,
            failure,
            inputArtifacts(),
            AnalysisCompleteness.INCOMPLETE
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> new AnalysisJob(
                analysisRunId(),
                jobId(),
                AnalysisWorkerKind.JOERN_ANALYSIS,
                sourceSnapshotId(),
                inputArtifacts(),
                AnalysisCompleteness.INCOMPLETE,
                AnalysisJobState.DEAD_LETTERED,
                1,
                List.of(failure),
                java.util.Optional.empty()
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new AnalysisJob(
                analysisRunId(),
                jobId(),
                AnalysisWorkerKind.JOERN_ANALYSIS,
                sourceSnapshotId(),
                inputArtifacts(),
                AnalysisCompleteness.INCOMPLETE,
                AnalysisJobState.ACCEPTED,
                0,
                List.of(),
                java.util.Optional.of(deadLetter)
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new AnalysisJob(
                analysisRunId(),
                jobId(),
                AnalysisWorkerKind.JOERN_ANALYSIS,
                sourceSnapshotId(),
                inputArtifacts(),
                AnalysisCompleteness.INCOMPLETE,
                AnalysisJobState.RETRYABLE,
                1,
                List.of(),
                java.util.Optional.empty()
            )
        );
    }

    @Test
    void deadLetterRecordsRejectInvalidFinalFailureAndInputReferences() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new DeadLetterProvenance(
                jobId(),
                AnalysisWorkerKind.JOERN_ANALYSIS,
                0,
                failure(1, "invalid attempt count"),
                inputArtifacts(),
                AnalysisCompleteness.INCOMPLETE
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new DeadLetterProvenance(
                jobId(),
                AnalysisWorkerKind.JOERN_ANALYSIS,
                1,
                failure(2, "final failure exceeds attempt count"),
                inputArtifacts(),
                AnalysisCompleteness.INCOMPLETE
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new DeadLetterProvenance(
                jobId(),
                AnalysisWorkerKind.JOERN_ANALYSIS,
                1,
                failure(1, "empty inputs"),
                List.of(),
                AnalysisCompleteness.INCOMPLETE
            )
        );
    }

    @Test
    void artifactRecordsKeepStoreProvenanceAndRejectInvalidValues() {
        var record = new AnalysisArtifactRecord(
            analysisRunId(),
            jobId(),
            AnalysisWorkerKind.JOERN_ANALYSIS,
            1,
            de.burger.forensics.analytics.domain.workspace.ProjectStorageArea.ANALYSIS_RESULTS,
            AnalysisArtifactPurpose.SEMANTIC_FACTS,
            AnalysisArtifactSensitivity.INTERNAL,
            inputArtifacts().getFirst()
        );

        assertEquals(AnalysisArtifactPurpose.SEMANTIC_FACTS, record.purpose());
        assertThrows(NullPointerException.class, () -> new AnalysisArtifactReference(null, AnalysisArtifactCategory.STATIC));
        assertThrows(NullPointerException.class, () -> new AnalysisArtifactReference(inputArtifacts().getFirst().artifact(), null));
        assertThrows(
            IllegalArgumentException.class,
            () -> new AnalysisArtifactRecord(
                analysisRunId(),
                jobId(),
                AnalysisWorkerKind.JOERN_ANALYSIS,
                0,
                de.burger.forensics.analytics.domain.workspace.ProjectStorageArea.ANALYSIS_RESULTS,
                AnalysisArtifactPurpose.SEMANTIC_FACTS,
                AnalysisArtifactSensitivity.INTERNAL,
                inputArtifacts().getFirst()
            )
        );
        assertThrows(
            NullPointerException.class,
            () -> new AnalysisArtifactRecord(
                analysisRunId(),
                jobId(),
                AnalysisWorkerKind.JOERN_ANALYSIS,
                1,
                de.burger.forensics.analytics.domain.workspace.ProjectStorageArea.ANALYSIS_RESULTS,
                null,
                AnalysisArtifactSensitivity.INTERNAL,
                inputArtifacts().getFirst()
            )
        );
    }

    @Test
    void jobContractsRejectMissingRequiredReferences() {
        assertThrows(
            IllegalArgumentException.class,
            () -> AnalysisJob.accepted(analysisRunId(), jobId(), AnalysisWorkerKind.AST_ANALYSIS, sourceSnapshotId(), List.of(), AnalysisCompleteness.COMPLETE)
        );
        assertThrows(
            NullPointerException.class,
            () -> AnalysisJob.accepted(analysisRunId(), jobId(), AnalysisWorkerKind.AST_ANALYSIS, null, inputArtifacts(), AnalysisCompleteness.COMPLETE)
        );
        assertThrows(IllegalArgumentException.class, () -> new AnalysisJobId(" "));
        assertThrows(IllegalArgumentException.class, () -> failure(0, "invalid attempt"));
        assertThrows(IllegalArgumentException.class, () -> failure(1, " "));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisJobFailure(
            jobId(),
            AnalysisWorkerKind.JOERN_ANALYSIS,
            1,
            "worker failed",
            List.of(" "),
            AnalysisCompleteness.INCOMPLETE
        ));
    }

    private static AnalysisJob job() {
        return AnalysisJob.accepted(
            analysisRunId(),
            jobId(),
            AnalysisWorkerKind.JOERN_ANALYSIS,
            sourceSnapshotId(),
            inputArtifacts(),
            AnalysisCompleteness.INCOMPLETE
        );
    }

    private static AnalysisRunId analysisRunId() {
        return new AnalysisRunId("analysis-1");
    }

    private static AnalysisJobId jobId() {
        return new AnalysisJobId("job-1");
    }

    private static SourceSnapshotId sourceSnapshotId() {
        return new SourceSnapshotId("snapshot-1");
    }

    private static List<AnalysisArtifactReference> inputArtifacts() {
        return List.of(new AnalysisArtifactReference(
            new ArtifactReference("source.tar", "source-snapshot", "sha256:source", 128L),
            AnalysisArtifactCategory.STATIC
        ));
    }

    private static AnalysisJobFailure failure(int attempt, String reason) {
        return new AnalysisJobFailure(
            jobId(),
            AnalysisWorkerKind.JOERN_ANALYSIS,
            attempt,
            reason,
            List.of("synthetic fixture diagnostic"),
            AnalysisCompleteness.INCOMPLETE
        );
    }
}
