package de.burger.forensics.analytics.engine;

import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.repository.RepositoryMetadata;

import java.util.Objects;

public record RepositoryAnalysisRequest(
    AnalysisRunId analysisRunId,
    RepositoryMetadata repositoryMetadata,
    String analysisProfile
) {
    public RepositoryAnalysisRequest {
        Objects.requireNonNull(analysisRunId, "analysisRunId must not be null");
        Objects.requireNonNull(repositoryMetadata, "repositoryMetadata must not be null");
        Objects.requireNonNull(analysisProfile, "analysisProfile must not be null");
    }
}
