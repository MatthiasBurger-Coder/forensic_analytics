package de.burger.forensics.analytics.application.analysis.command;

import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.repository.RepositorySource;
import de.burger.forensics.analytics.domain.source.SourceFact;

import java.util.List;
import java.util.Objects;

public record SemanticAnalysisRequest(
    AnalysisRunId analysisRunId,
    RepositorySource repositorySource,
    List<SourceFact> sourceFacts
) {
    public SemanticAnalysisRequest {
        Objects.requireNonNull(analysisRunId, "analysisRunId must not be null");
        Objects.requireNonNull(repositorySource, "repositorySource must not be null");
        sourceFacts = List.copyOf(Objects.requireNonNull(sourceFacts, "sourceFacts must not be null"));
    }
}
