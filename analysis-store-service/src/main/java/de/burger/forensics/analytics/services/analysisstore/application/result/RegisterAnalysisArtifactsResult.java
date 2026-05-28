package de.burger.forensics.analytics.services.analysisstore.application.result;

import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactReference;

import java.util.List;

public record RegisterAnalysisArtifactsResult(List<AnalysisArtifactReference> artifacts, OperationOutcome status) {
    public RegisterAnalysisArtifactsResult {
        artifacts = List.copyOf(artifacts);
    }
}
