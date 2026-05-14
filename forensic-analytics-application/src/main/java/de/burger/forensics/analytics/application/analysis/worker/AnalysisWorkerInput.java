package de.burger.forensics.analytics.application.analysis.worker;

import de.burger.forensics.analytics.domain.analysis.AnalysisArtifactReference;
import de.burger.forensics.analytics.domain.analysis.AnalysisCompleteness;
import de.burger.forensics.analytics.domain.analysis.AnalysisJobId;
import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.analysis.AnalysisWorkerKind;
import de.burger.forensics.analytics.domain.repository.SourceSnapshotId;

import java.util.List;
import java.util.Objects;

public record AnalysisWorkerInput(
    AnalysisRunId analysisRunId,
    AnalysisJobId jobId,
    AnalysisWorkerKind workerKind,
    SourceSnapshotId sourceSnapshotId,
    List<AnalysisArtifactReference> inputArtifacts,
    String workerVersion,
    List<String> diagnostics,
    AnalysisCompleteness completeness
) {
    public AnalysisWorkerInput {
        Objects.requireNonNull(analysisRunId, "analysisRunId must not be null");
        Objects.requireNonNull(jobId, "jobId must not be null");
        Objects.requireNonNull(workerKind, "workerKind must not be null");
        Objects.requireNonNull(sourceSnapshotId, "sourceSnapshotId must not be null");
        inputArtifacts = copyRequiredArtifacts(inputArtifacts, "inputArtifacts");
        requireText(workerVersion, "workerVersion");
        diagnostics = copyDiagnostics(diagnostics);
        Objects.requireNonNull(completeness, "completeness must not be null");
    }

    static List<AnalysisArtifactReference> copyRequiredArtifacts(
        List<AnalysisArtifactReference> artifacts,
        String fieldName
    ) {
        var copied = List.copyOf(Objects.requireNonNull(artifacts, fieldName + " must not be null"));
        if (copied.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be empty");
        }
        return copied;
    }

    static List<String> copyDiagnostics(List<String> diagnostics) {
        return List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics must not be null")).stream()
            .peek(diagnostic -> requireText(diagnostic, "diagnostic"))
            .toList();
    }

    static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
