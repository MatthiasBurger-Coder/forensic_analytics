package de.burger.forensics.analytics.services.analysisstore.domain;

import java.util.List;
import java.util.Objects;

public record AnalysisJobFailure(
    AnalysisJobId jobId,
    AnalysisWorkerKind workerKind,
    int attempt,
    String reason,
    List<String> diagnostics,
    AnalysisCompleteness completeness,
    boolean retryable
) {
    public AnalysisJobFailure {
        Objects.requireNonNull(jobId, "jobId must not be null");
        Objects.requireNonNull(workerKind, "workerKind must not be null");
        if (attempt < 1) {
            throw new IllegalArgumentException("attempt must be positive");
        }
        reason = RequiredText.require(reason, "reason");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics must not be null"));
        Objects.requireNonNull(completeness, "completeness must not be null");
    }
}
