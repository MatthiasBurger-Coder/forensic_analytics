package de.burger.forensics.analytics.application.analysis.result;

import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.repository.RepositoryMetadata;
import de.burger.forensics.analytics.domain.source.SourceFact;

import java.util.List;
import java.util.Objects;

public record RunRepositoryAnalysisResult(
    AnalysisRunId analysisRunId,
    RepositoryMetadata repositoryMetadata,
    String analysisProfile,
    RepositoryAnalysisStatus status,
    List<SourceFact> sourceFacts,
    SemanticAnalysisResult semanticAnalysis,
    RuleGenerationResult ruleGeneration
) {
    public RunRepositoryAnalysisResult {
        Objects.requireNonNull(analysisRunId, "analysisRunId must not be null");
        Objects.requireNonNull(repositoryMetadata, "repositoryMetadata must not be null");
        Objects.requireNonNull(analysisProfile, "analysisProfile must not be null");
        Objects.requireNonNull(status, "status must not be null");
        sourceFacts = List.copyOf(Objects.requireNonNull(sourceFacts, "sourceFacts must not be null"));
        Objects.requireNonNull(semanticAnalysis, "semanticAnalysis must not be null");
        Objects.requireNonNull(ruleGeneration, "ruleGeneration must not be null");
    }

    public static RunRepositoryAnalysisResult completed(
        AnalysisRunId analysisRunId,
        RepositoryMetadata repositoryMetadata,
        String analysisProfile,
        List<SourceFact> sourceFacts,
        SemanticAnalysisResult semanticAnalysis,
        RuleGenerationResult ruleGeneration
    ) {
        return new RunRepositoryAnalysisResult(
            analysisRunId,
            repositoryMetadata,
            analysisProfile,
            RepositoryAnalysisStatus.COMPLETED,
            sourceFacts,
            semanticAnalysis,
            ruleGeneration
        );
    }
}
