package de.burger.forensics.analytics.domain.analysis;

import java.util.List;
import java.util.Objects;

public record DeadLetterProvenance(
    AnalysisJobId jobId,
    AnalysisWorkerKind workerKind,
    int attemptCount,
    AnalysisJobFailure finalFailure,
    List<AnalysisArtifactReference> inputArtifacts,
    AnalysisCompleteness completeness
) {
    public DeadLetterProvenance {
        Objects.requireNonNull(jobId, "jobId must not be null");
        Objects.requireNonNull(workerKind, "workerKind must not be null");
        if (attemptCount < 1) {
            throw new IllegalArgumentException("attempt count must be positive");
        }
        Objects.requireNonNull(finalFailure, "finalFailure must not be null");
        inputArtifacts = copyRequiredArtifacts(inputArtifacts);
        Objects.requireNonNull(completeness, "completeness must not be null");
        requireMatchingFailure(jobId, workerKind, attemptCount, finalFailure);
    }

    private static List<AnalysisArtifactReference> copyRequiredArtifacts(List<AnalysisArtifactReference> artifacts) {
        var copied = List.copyOf(Objects.requireNonNull(artifacts, "inputArtifacts must not be null"));
        if (copied.isEmpty()) {
            throw new IllegalArgumentException("inputArtifacts must not be empty");
        }
        return copied;
    }

    private static void requireMatchingFailure(
        AnalysisJobId jobId,
        AnalysisWorkerKind workerKind,
        int attemptCount,
        AnalysisJobFailure finalFailure
    ) {
        if (!jobId.equals(finalFailure.jobId())) {
            throw new IllegalArgumentException("dead-letter job id must match final failure");
        }
        if (!workerKind.equals(finalFailure.workerKind())) {
            throw new IllegalArgumentException("dead-letter worker kind must match final failure");
        }
        if (finalFailure.attempt() > attemptCount) {
            throw new IllegalArgumentException("final failure attempt must not exceed dead-letter attempt count");
        }
    }
}
