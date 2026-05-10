package de.burger.forensics.analytics.application.analysis.command;

import de.burger.forensics.analytics.application.analysis.result.SemanticAnalysisResult;
import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.repository.RepositorySource;
import de.burger.forensics.analytics.domain.source.SourceFact;

import java.util.List;
import java.util.Objects;

public record RuleGenerationRequest(
    AnalysisRunId analysisRunId,
    RepositorySource repositorySource,
    List<SourceFact> sourceFacts,
    SemanticAnalysisResult semanticAnalysis
) {
    public RuleGenerationRequest {
        Objects.requireNonNull(analysisRunId, "analysisRunId must not be null");
        Objects.requireNonNull(repositorySource, "repositorySource must not be null");
        sourceFacts = List.copyOf(Objects.requireNonNull(sourceFacts, "sourceFacts must not be null"));
        Objects.requireNonNull(semanticAnalysis, "semanticAnalysis must not be null");
    }
}
