package de.burger.forensics.analytics.services.analysisstore.domain;

import java.util.Objects;

public record AnalysisArtifactReference(
    ArtifactReference artifact,
    AnalysisArtifactCategory category,
    String producerService,
    String schemaVersion,
    AnalysisCompleteness completeness
) {
    public AnalysisArtifactReference {
        Objects.requireNonNull(artifact, "artifact must not be null");
        Objects.requireNonNull(category, "category must not be null");
        producerService = RequiredText.require(producerService, "producerService");
        schemaVersion = RequiredText.require(schemaVersion, "schemaVersion");
        Objects.requireNonNull(completeness, "completeness must not be null");
    }

    public String path() {
        return artifact.path();
    }
}
