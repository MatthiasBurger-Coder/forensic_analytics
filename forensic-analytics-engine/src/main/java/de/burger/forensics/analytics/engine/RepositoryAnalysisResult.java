package de.burger.forensics.analytics.engine;

import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.repository.RepositoryMetadata;
import de.burger.forensics.analytics.domain.source.SourceFact;

import java.util.List;
import java.util.Objects;

public record RepositoryAnalysisResult(
    AnalysisRunId analysisRunId,
    RepositoryMetadata repositoryMetadata,
    String analysisProfile,
    RepositoryAnalysisStatus status,
    List<SourceFact> sourceFacts
) {
    public RepositoryAnalysisResult {
        Objects.requireNonNull(analysisRunId, "analysisRunId must not be null");
        Objects.requireNonNull(repositoryMetadata, "repositoryMetadata must not be null");
        Objects.requireNonNull(analysisProfile, "analysisProfile must not be null");
        Objects.requireNonNull(status, "status must not be null");
        sourceFacts = List.copyOf(Objects.requireNonNull(sourceFacts, "sourceFacts must not be null"));
    }

    public static RepositoryAnalysisResult completed(
        AnalysisRunId analysisRunId,
        RepositoryMetadata repositoryMetadata,
        String analysisProfile,
        List<SourceFact> sourceFacts
    ) {
        return new RepositoryAnalysisResult(
            analysisRunId,
            repositoryMetadata,
            analysisProfile,
            RepositoryAnalysisStatus.COMPLETED,
            sourceFacts
        );
    }
}
