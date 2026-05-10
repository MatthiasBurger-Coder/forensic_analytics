package de.burger.forensics.analytics.engine;

import java.util.Objects;

public record RepositoryAnalysisRequest(
    String analysisId,
    String repositoryLocation,
    String analysisProfile
) {
    public RepositoryAnalysisRequest {
        Objects.requireNonNull(analysisId, "analysisId must not be null");
        Objects.requireNonNull(repositoryLocation, "repositoryLocation must not be null");
        Objects.requireNonNull(analysisProfile, "analysisProfile must not be null");
    }
}
