package de.burger.forensics.analytics.domain.analysis;

import de.burger.forensics.analytics.domain.workspace.ProjectStorageArea;

import java.util.Objects;

public record AnalysisArtifactRecord(
    AnalysisRunId analysisRunId,
    AnalysisJobId jobId,
    AnalysisWorkerKind workerKind,
    int attempt,
    ProjectStorageArea storageArea,
    AnalysisArtifactPurpose purpose,
    AnalysisArtifactSensitivity sensitivity,
    AnalysisArtifactReference artifact
) {
    public AnalysisArtifactRecord {
        Objects.requireNonNull(analysisRunId, "analysisRunId must not be null");
        Objects.requireNonNull(jobId, "jobId must not be null");
        Objects.requireNonNull(workerKind, "workerKind must not be null");
        if (attempt < 1) {
            throw new IllegalArgumentException("attempt must be positive");
        }
        Objects.requireNonNull(storageArea, "storageArea must not be null");
        Objects.requireNonNull(purpose, "purpose must not be null");
        Objects.requireNonNull(sensitivity, "sensitivity must not be null");
        Objects.requireNonNull(artifact, "artifact must not be null");
    }
}
