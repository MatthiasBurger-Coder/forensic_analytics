package de.burger.forensics.analytics.persistence;

import de.burger.forensics.analytics.domain.analysis.AnalysisArtifactCategory;
import de.burger.forensics.analytics.domain.analysis.AnalysisArtifactReference;
import de.burger.forensics.analytics.domain.analysis.AnalysisCompleteness;
import de.burger.forensics.analytics.domain.analysis.AnalysisJob;
import de.burger.forensics.analytics.domain.analysis.AnalysisJobId;
import de.burger.forensics.analytics.domain.analysis.AnalysisJobState;
import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.analysis.AnalysisWorkerKind;
import de.burger.forensics.analytics.domain.artifact.ArtifactReference;
import de.burger.forensics.analytics.domain.repository.SourceSnapshotId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryAnalysisStoreTest {
    private final InMemoryAnalysisStore store = new InMemoryAnalysisStore();

    @Test
    void storesAndUpdatesJobRecordsByJobIdWithoutDuplicatingRetries() {
        var accepted = job(jobId("job-1"));
        var retrying = accepted.dispatchable().running(1).retryable(
            new de.burger.forensics.analytics.domain.analysis.AnalysisJobFailure(
                accepted.id(),
                accepted.workerKind(),
                1,
                "synthetic worker failure",
                List.of("retry requested"),
                AnalysisCompleteness.INCOMPLETE
            )
        );

        store.storeJob(accepted);
        store.storeJob(retrying);

        assertEquals(retrying, store.findJob(accepted.id()).orElseThrow());
        assertEquals(List.of(retrying), store.findJobs(analysisRunId()));
        assertEquals(AnalysisJobState.RETRYABLE, store.findJobs(analysisRunId()).getFirst().state());
    }

    @Test
    void returnsJobsForAnalysisRunInDeterministicOrder() {
        var second = job(jobId("job-2"));
        var first = job(jobId("job-1"));

        store.storeJob(second);
        store.storeJob(first);

        assertEquals(List.of(first, second), store.findJobs(analysisRunId()));
        assertTrue(store.findJob(jobId("missing")).isEmpty());
    }

    private static AnalysisJob job(AnalysisJobId jobId) {
        return AnalysisJob.accepted(
            analysisRunId(),
            jobId,
            AnalysisWorkerKind.REPOSITORY_ANALYSIS,
            new SourceSnapshotId("snapshot-1"),
            List.of(new AnalysisArtifactReference(
                new ArtifactReference("source.tar", "source-snapshot", "sha256:source", 128L),
                AnalysisArtifactCategory.STATIC
            )),
            AnalysisCompleteness.COMPLETE
        );
    }

    private static AnalysisRunId analysisRunId() {
        return new AnalysisRunId("analysis-1");
    }

    private static AnalysisJobId jobId(String value) {
        return new AnalysisJobId(value);
    }
}
