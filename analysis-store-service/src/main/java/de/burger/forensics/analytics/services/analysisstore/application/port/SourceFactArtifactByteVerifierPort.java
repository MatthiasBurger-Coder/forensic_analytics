package de.burger.forensics.analytics.services.analysisstore.application.port;

import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactCategory;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJobId;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisRunId;
import de.burger.forensics.analytics.services.analysisstore.domain.SourceSnapshotId;

import java.util.Map;

public interface SourceFactArtifactByteVerifierPort {
    String JAVA_AST_OWNER_SERVICE = "java-ast-analysis-service";
    String JAVA_AST_SOURCE_FACT_RETRIEVAL_CONTRACT =
        "java-ast-analysis.v1.JavaAstAnalysisService.GetSourceFactArtifactBytes";

    boolean supports(AnalysisArtifactReference artifact);

    AnalysisArtifactReference verify(
        AnalysisRunId analysisRunId,
        AnalysisJobId analysisJobId,
        SourceSnapshotId sourceSnapshotId,
        String requestId,
        String correlationId,
        AnalysisArtifactReference artifact,
        Map<String, String> safeAttributes
    );

    static SourceFactArtifactByteVerifierPort unavailable() {
        return new SourceFactArtifactByteVerifierPort() {
            @Override
            public boolean supports(AnalysisArtifactReference artifact) {
                return artifact.category() == AnalysisArtifactCategory.STATIC
                    && JAVA_AST_OWNER_SERVICE.equals(artifact.byteAccess().ownerService())
                    && JAVA_AST_SOURCE_FACT_RETRIEVAL_CONTRACT.equals(artifact.byteAccess().retrievalContract());
            }

            @Override
            public AnalysisArtifactReference verify(
                AnalysisRunId analysisRunId,
                AnalysisJobId analysisJobId,
                SourceSnapshotId sourceSnapshotId,
                String requestId,
                String correlationId,
                AnalysisArtifactReference artifact,
                Map<String, String> safeAttributes
            ) {
                throw new IllegalStateException("Source fact artifact byte verifier is not available");
            }
        };
    }
}
