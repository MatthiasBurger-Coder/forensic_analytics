package de.burger.forensics.analytics.domain.analysis;

import java.util.List;
import java.util.Objects;

public record AnalysisJobFailure(
    AnalysisJobId jobId,
    AnalysisWorkerKind workerKind,
    int attempt,
    String reason,
    List<String> diagnostics,
    AnalysisCompleteness completeness
) {
    public AnalysisJobFailure {
        Objects.requireNonNull(jobId, "jobId must not be null");
        Objects.requireNonNull(workerKind, "workerKind must not be null");
        if (attempt < 1) {
            throw new IllegalArgumentException("attempt must be positive");
        }
        RequiredAnalysisText.requireText(reason, "failure reason");
        diagnostics = copyDiagnostics(diagnostics);
        Objects.requireNonNull(completeness, "completeness must not be null");
    }

    private static List<String> copyDiagnostics(List<String> diagnostics) {
        return List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics must not be null")).stream()
            .peek(diagnostic -> RequiredAnalysisText.requireText(diagnostic, "diagnostic"))
            .toList();
    }
}
