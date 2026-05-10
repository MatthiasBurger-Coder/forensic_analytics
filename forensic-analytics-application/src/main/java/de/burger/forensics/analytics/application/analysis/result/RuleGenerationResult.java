package de.burger.forensics.analytics.application.analysis.result;

import de.burger.forensics.analytics.domain.artifact.ArtifactReference;

import java.util.List;
import java.util.Objects;

public record RuleGenerationResult(List<ArtifactReference> artifacts) {
    public RuleGenerationResult {
        artifacts = List.copyOf(Objects.requireNonNull(artifacts, "artifacts must not be null"));
    }
}
