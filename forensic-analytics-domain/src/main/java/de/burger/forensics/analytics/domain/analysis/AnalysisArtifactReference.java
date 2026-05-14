package de.burger.forensics.analytics.domain.analysis;

import de.burger.forensics.analytics.domain.artifact.ArtifactReference;

import java.util.Objects;

public record AnalysisArtifactReference(
    ArtifactReference artifact,
    AnalysisArtifactCategory category
) {
    public AnalysisArtifactReference {
        Objects.requireNonNull(artifact, "artifact must not be null");
        Objects.requireNonNull(category, "category must not be null");
    }
}
