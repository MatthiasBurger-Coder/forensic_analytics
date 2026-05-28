package de.burger.forensics.analytics.services.analysisorchestrator.domain;

import java.util.Objects;

public record AnalysisArtifactReference(
    ArtifactReference artifact,
    AnalysisArtifactCategory category,
    String producerService,
    String schemaVersion,
    AnalysisCompleteness completeness,
    ArtifactByteAccess byteAccess
) {
    public AnalysisArtifactReference {
        Objects.requireNonNull(artifact, "artifact must not be null");
        Objects.requireNonNull(category, "category must not be null");
        producerService = AnalysisOrchestratorArtifactOwnership.requireExternalProducer(producerService);
        schemaVersion = RequiredText.require(schemaVersion, "schemaVersion");
        Objects.requireNonNull(completeness, "completeness must not be null");
        Objects.requireNonNull(byteAccess, "byteAccess must not be null");
    }

    public String path() {
        return artifact.path();
    }
}
