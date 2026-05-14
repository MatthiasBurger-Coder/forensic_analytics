package de.burger.forensics.analytics.application.analysis.worker;

import de.burger.forensics.analytics.domain.analysis.AnalysisArtifactReference;
import de.burger.forensics.analytics.domain.analysis.AnalysisCompleteness;
import de.burger.forensics.analytics.domain.analysis.AnalysisJobId;
import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.analysis.AnalysisWorkerKind;
import de.burger.forensics.analytics.domain.repository.SourceSnapshotId;

import java.util.List;
import java.util.Objects;

public record AnalysisWorkerOutput(
    AnalysisRunId analysisRunId,
    AnalysisJobId jobId,
    AnalysisWorkerKind workerKind,
    SourceSnapshotId sourceSnapshotId,
    List<AnalysisArtifactReference> inputArtifacts,
    List<AnalysisArtifactReference> outputArtifacts,
    String workerVersion,
    List<String> diagnostics,
    AnalysisCompleteness completeness
) {
    public AnalysisWorkerOutput {
        Objects.requireNonNull(analysisRunId, "analysisRunId must not be null");
        Objects.requireNonNull(jobId, "jobId must not be null");
        Objects.requireNonNull(workerKind, "workerKind must not be null");
        Objects.requireNonNull(sourceSnapshotId, "sourceSnapshotId must not be null");
        inputArtifacts = AnalysisWorkerInput.copyRequiredArtifacts(inputArtifacts, "inputArtifacts");
        outputArtifacts = AnalysisWorkerInput.copyRequiredArtifacts(outputArtifacts, "outputArtifacts");
        AnalysisWorkerInput.requireText(workerVersion, "workerVersion");
        diagnostics = AnalysisWorkerInput.copyDiagnostics(diagnostics);
        Objects.requireNonNull(completeness, "completeness must not be null");
    }
}
