package de.burger.forensics.analytics.services.analysisstore.application.port;

import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJobId;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisRunId;
import de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.InstrumentationTarget;
import de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.InstrumentationTargetSelection;
import de.burger.forensics.analytics.services.analysisstore.domain.RepositoryToBtmOrchestrationDomain.StartRepositoryToBtmCommand;
import de.burger.forensics.analytics.services.analysisstore.domain.SourceSnapshotId;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public interface BtmGenerationWorkerPort {
    BtmGenerationResult generate(
        StartRepositoryToBtmCommand command,
        AnalysisJobId btmGenerationJobId,
        SourceSnapshotId sourceSnapshotId,
        List<AnalysisArtifactReference> sourceFactArtifacts,
        List<AnalysisArtifactReference> semanticArtifacts,
        AnalysisCompleteness inputCompleteness,
        InstrumentationTargetSelection targetSelection,
        List<InstrumentationTarget> targets
    );

    static BtmGenerationWorkerPort unavailable() {
        return (
            command,
            btmGenerationJobId,
            sourceSnapshotId,
            sourceFactArtifacts,
            semanticArtifacts,
            inputCompleteness,
            targetSelection,
            targets
        ) -> {
            throw new WorkerOwnerApiUnavailableException("BTM Generation");
        };
    }

    record BtmGenerationResult(
        AnalysisRunId analysisRunId,
        AnalysisJobId analysisJobId,
        SourceSnapshotId sourceSnapshotId,
        AnalysisCompleteness completeness,
        List<AnalysisArtifactReference> generatedArtifacts,
        List<RepositoryAnalysisWorkerPort.WorkerDiagnostic> diagnostics,
        Map<String, String> attributes
    ) {
        public BtmGenerationResult {
            analysisRunId = Objects.requireNonNull(analysisRunId, "analysisRunId must not be null");
            analysisJobId = Objects.requireNonNull(analysisJobId, "analysisJobId must not be null");
            sourceSnapshotId = Objects.requireNonNull(sourceSnapshotId, "sourceSnapshotId must not be null");
            completeness = Objects.requireNonNull(completeness, "completeness must not be null");
            generatedArtifacts = List.copyOf(Objects.requireNonNullElse(generatedArtifacts, List.of()));
            diagnostics = List.copyOf(Objects.requireNonNullElse(diagnostics, List.of()));
            attributes = Map.copyOf(Objects.requireNonNullElse(attributes, Map.of()));
        }
    }
}
