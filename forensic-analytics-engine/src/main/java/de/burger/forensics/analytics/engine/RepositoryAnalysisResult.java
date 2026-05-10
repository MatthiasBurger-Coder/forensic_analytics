package de.burger.forensics.analytics.engine;

import java.util.List;
import java.util.Objects;

public record RepositoryAnalysisResult(
    String analysisId,
    String repositoryLocation,
    String analysisProfile,
    RepositoryAnalysisStatus status,
    List<SourceFact> sourceFacts
) {
    public RepositoryAnalysisResult {
        Objects.requireNonNull(analysisId, "analysisId must not be null");
        Objects.requireNonNull(repositoryLocation, "repositoryLocation must not be null");
        Objects.requireNonNull(analysisProfile, "analysisProfile must not be null");
        Objects.requireNonNull(status, "status must not be null");
        sourceFacts = List.copyOf(Objects.requireNonNull(sourceFacts, "sourceFacts must not be null"));
    }

    public static RepositoryAnalysisResult completed(
        String analysisId,
        String repositoryLocation,
        String analysisProfile,
        List<SourceFact> sourceFacts
    ) {
        return new RepositoryAnalysisResult(
            analysisId,
            repositoryLocation,
            analysisProfile,
            RepositoryAnalysisStatus.COMPLETED,
            sourceFacts
        );
    }
}
