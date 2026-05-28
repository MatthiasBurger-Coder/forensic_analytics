package de.burger.forensics.analytics.services.analysisstore.application.port;

import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJobId;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisRunId;
import de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.AcceptedStaticSourceFact;
import de.burger.forensics.analytics.services.analysisstore.domain.SourceSnapshotId;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public interface SourceFactArtifactReaderPort {
    SourceFactArtifact readFacts(
        AnalysisRunId analysisRunId,
        AnalysisJobId analysisJobId,
        SourceSnapshotId sourceSnapshotId,
        String requestId,
        String correlationId,
        AnalysisArtifactReference artifact,
        Map<String, String> safeAttributes
    );

    static SourceFactArtifactReaderPort unavailable() {
        return (analysisRunId, analysisJobId, sourceSnapshotId, requestId, correlationId, artifact, safeAttributes) -> {
            throw new WorkerOwnerApiUnavailableException("Java AST source fact artifact reader");
        };
    }

    record SourceFactArtifact(
        AnalysisArtifactReference artifact,
        List<AcceptedStaticSourceFact> facts,
        AnalysisCompleteness completeness,
        List<SourceFactDiagnostic> diagnostics
    ) {
        public SourceFactArtifact {
            artifact = Objects.requireNonNull(artifact, "artifact must not be null");
            facts = List.copyOf(Objects.requireNonNullElse(facts, List.of()));
            completeness = Objects.requireNonNull(completeness, "completeness must not be null");
            diagnostics = List.copyOf(Objects.requireNonNullElse(diagnostics, List.of()));
        }
    }

    record SourceFactDiagnostic(String code, String message, boolean affectsCompleteness) {
        public SourceFactDiagnostic {
            if (code == null || code.isBlank()) {
                throw new IllegalArgumentException("diagnostic code must not be blank");
            }
            if (message == null || message.isBlank()) {
                throw new IllegalArgumentException("diagnostic message must not be blank");
            }
        }
    }
}
