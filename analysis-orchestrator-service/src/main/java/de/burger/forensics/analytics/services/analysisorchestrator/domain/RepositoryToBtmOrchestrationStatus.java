package de.burger.forensics.analytics.services.analysisorchestrator.domain;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record RepositoryToBtmOrchestrationStatus(
    String correlationId,
    AnalysisRunId analysisRunId,
    AnalysisJobId repositoryAnalysisJobId,
    String sourceSnapshotId,
    AnalysisCompleteness completeness,
    RepositoryToBtmOrchestrationState state,
    BtmDeliveryReadiness btmDeliveryReadiness,
    boolean joernSkipped,
    List<RepositoryToBtmDiagnostic> diagnostics,
    Map<String, String> attributes
) {
    public RepositoryToBtmOrchestrationStatus {
        correlationId = RequiredText.require(correlationId, "correlationId");
        Objects.requireNonNull(analysisRunId, "analysisRunId must not be null");
        Objects.requireNonNull(repositoryAnalysisJobId, "repositoryAnalysisJobId must not be null");
        sourceSnapshotId = sourceSnapshotId == null || sourceSnapshotId.isBlank()
            ? ""
            : SafeMetadata.requireOpaqueId(sourceSnapshotId, "sourceSnapshotId");
        Objects.requireNonNull(completeness, "completeness must not be null");
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(btmDeliveryReadiness, "btmDeliveryReadiness must not be null");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics must not be null"));
        attributes = SafeMetadata.safeAttributes(attributes);
    }
}
