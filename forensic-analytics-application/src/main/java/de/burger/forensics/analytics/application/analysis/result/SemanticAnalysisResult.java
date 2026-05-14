package de.burger.forensics.analytics.application.analysis.result;

import de.burger.forensics.analytics.domain.artifact.ArtifactReference;
import de.burger.forensics.analytics.domain.semantic.SemanticGraph;

import java.util.List;
import java.util.Objects;

public record SemanticAnalysisResult(
    String providerName,
    String semanticFingerprint,
    List<ArtifactReference> artifacts,
    SemanticGraph semanticGraph
) {
    public SemanticAnalysisResult {
        requireText(providerName, "providerName");
        requireText(semanticFingerprint, "semanticFingerprint");
        artifacts = List.copyOf(Objects.requireNonNull(artifacts, "artifacts must not be null"));
        Objects.requireNonNull(semanticGraph, "semanticGraph must not be null");
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
