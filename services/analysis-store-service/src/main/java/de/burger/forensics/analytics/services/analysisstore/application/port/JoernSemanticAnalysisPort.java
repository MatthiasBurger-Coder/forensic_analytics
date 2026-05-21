package de.burger.forensics.analytics.services.analysisstore.application.port;

import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJobId;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisRunId;
import de.burger.forensics.analytics.services.analysisstore.domain.RepositoryToBtmOrchestrationDomain.StartRepositoryToBtmCommand;
import de.burger.forensics.analytics.services.analysisstore.domain.SourceSnapshotId;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public interface JoernSemanticAnalysisPort {
    JoernAnalysisResult analyze(
        StartRepositoryToBtmCommand command,
        AnalysisJobId joernAnalysisJobId,
        RepositoryAnalysisWorkerPort.RepositoryAnalysisResult repositoryAnalysis
    );

    static JoernSemanticAnalysisPort unavailable() {
        return (command, joernAnalysisJobId, repositoryAnalysis) -> {
            throw new WorkerOwnerApiUnavailableException("Joern CPG");
        };
    }

    record JoernAnalysisResult(
        AnalysisRunId analysisRunId,
        AnalysisJobId analysisJobId,
        SourceSnapshotId sourceSnapshotId,
        AnalysisCompleteness completeness,
        List<AnalysisArtifactReference> semanticArtifacts,
        List<RepositoryAnalysisWorkerPort.WorkerDiagnostic> diagnostics,
        Map<String, String> attributes
    ) {
        public JoernAnalysisResult {
            analysisRunId = Objects.requireNonNull(analysisRunId, "analysisRunId must not be null");
            analysisJobId = Objects.requireNonNull(analysisJobId, "analysisJobId must not be null");
            sourceSnapshotId = Objects.requireNonNull(sourceSnapshotId, "sourceSnapshotId must not be null");
            completeness = Objects.requireNonNull(completeness, "completeness must not be null");
            semanticArtifacts = List.copyOf(Objects.requireNonNullElse(semanticArtifacts, List.of()));
            diagnostics = List.copyOf(Objects.requireNonNullElse(diagnostics, List.of()));
            attributes = Map.copyOf(Objects.requireNonNullElse(attributes, Map.of()));
        }
    }
}
