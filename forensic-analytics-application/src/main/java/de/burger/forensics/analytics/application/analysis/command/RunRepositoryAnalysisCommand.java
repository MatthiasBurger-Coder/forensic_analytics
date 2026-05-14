package de.burger.forensics.analytics.application.analysis.command;

import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.repository.RepositoryMetadata;

import java.util.Objects;

public record RunRepositoryAnalysisCommand(
    AnalysisRunId analysisRunId,
    RepositoryMetadata repositoryMetadata,
    String analysisProfile
) {
    public RunRepositoryAnalysisCommand {
        Objects.requireNonNull(analysisRunId, "analysisRunId must not be null");
        Objects.requireNonNull(repositoryMetadata, "repositoryMetadata must not be null");
        Objects.requireNonNull(analysisProfile, "analysisProfile must not be null");
    }
}
