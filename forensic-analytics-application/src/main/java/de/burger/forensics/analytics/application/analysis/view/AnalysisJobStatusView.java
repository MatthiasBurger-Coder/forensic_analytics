package de.burger.forensics.analytics.application.analysis.view;

import de.burger.forensics.analytics.domain.analysis.AnalysisJob;
import de.burger.forensics.analytics.domain.analysis.AnalysisJobId;
import de.burger.forensics.analytics.domain.analysis.AnalysisJobState;
import de.burger.forensics.analytics.domain.analysis.AnalysisWorkerKind;

import java.util.List;
import java.util.Objects;

public record AnalysisJobStatusView(
    AnalysisJobId jobId,
    AnalysisWorkerKind workerKind,
    AnalysisJobState state,
    int attempt,
    List<String> diagnostics
) {
    public AnalysisJobStatusView {
        Objects.requireNonNull(jobId, "jobId must not be null");
        Objects.requireNonNull(workerKind, "workerKind must not be null");
        Objects.requireNonNull(state, "state must not be null");
        if (attempt < 0) {
            throw new IllegalArgumentException("attempt must not be negative");
        }
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics must not be null")).stream()
            .peek(diagnostic -> ServerAnalysisRequest.requireText(diagnostic, "diagnostic"))
            .toList();
    }

    public static AnalysisJobStatusView from(AnalysisJob job) {
        Objects.requireNonNull(job, "job must not be null");
        return new AnalysisJobStatusView(
            job.id(),
            job.workerKind(),
            job.state(),
            job.attempt(),
            job.failures().stream()
                .flatMap(failure -> failure.diagnostics().stream())
                .toList()
        );
    }
}
