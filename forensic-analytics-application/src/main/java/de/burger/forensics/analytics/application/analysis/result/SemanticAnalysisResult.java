package de.burger.forensics.analytics.application.analysis.result;

import de.burger.forensics.analytics.domain.artifact.ArtifactReference;

import java.util.List;
import java.util.Objects;

public record SemanticAnalysisResult(String providerName, List<ArtifactReference> artifacts) {
    public SemanticAnalysisResult {
        Objects.requireNonNull(providerName, "providerName must not be null");
        artifacts = List.copyOf(Objects.requireNonNull(artifacts, "artifacts must not be null"));
    }
}
