package de.burger.forensics.analytics.application.analysis;

import de.burger.forensics.analytics.application.analysis.command.RunRepositoryAnalysisCommand;
import de.burger.forensics.analytics.application.analysis.command.RuleGenerationRequest;
import de.burger.forensics.analytics.application.analysis.command.SemanticAnalysisRequest;
import de.burger.forensics.analytics.application.analysis.port.RepositoryAnalysisResultStore;
import de.burger.forensics.analytics.application.analysis.port.RepositorySourcePort;
import de.burger.forensics.analytics.application.analysis.port.RuleGenerationPort;
import de.burger.forensics.analytics.application.analysis.port.SemanticAnalysisPort;
import de.burger.forensics.analytics.application.analysis.port.SourceScannerPort;
import de.burger.forensics.analytics.application.analysis.result.RunRepositoryAnalysisResult;

import java.util.List;
import java.util.Objects;

public final class DefaultRunRepositoryAnalysisUseCase implements RunRepositoryAnalysisUseCase {
    private final RepositorySourcePort repositorySourcePort;
    private final SourceScannerPort sourceScannerPort;
    private final SemanticAnalysisPort semanticAnalysisPort;
    private final RuleGenerationPort ruleGenerationPort;
    private final RepositoryAnalysisResultStore resultStore;

    public DefaultRunRepositoryAnalysisUseCase(
        RepositorySourcePort repositorySourcePort,
        SourceScannerPort sourceScannerPort,
        SemanticAnalysisPort semanticAnalysisPort,
        RuleGenerationPort ruleGenerationPort,
        RepositoryAnalysisResultStore resultStore
    ) {
        this.repositorySourcePort = Objects.requireNonNull(repositorySourcePort, "repositorySourcePort must not be null");
        this.sourceScannerPort = Objects.requireNonNull(sourceScannerPort, "sourceScannerPort must not be null");
        this.semanticAnalysisPort = Objects.requireNonNull(semanticAnalysisPort, "semanticAnalysisPort must not be null");
        this.ruleGenerationPort = Objects.requireNonNull(ruleGenerationPort, "ruleGenerationPort must not be null");
        this.resultStore = Objects.requireNonNull(resultStore, "resultStore must not be null");
    }

    @Override
    public RunRepositoryAnalysisResult run(RunRepositoryAnalysisCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        var source = Objects.requireNonNull(
            repositorySourcePort.resolve(command.repositoryMetadata()),
            "repository source must not be null"
        );
        var sourceFacts = List.copyOf(
            Objects.requireNonNull(sourceScannerPort.scan(source), "source facts must not be null")
        );
        var semanticAnalysis = Objects.requireNonNull(
            semanticAnalysisPort.analyze(new SemanticAnalysisRequest(command.analysisRunId(), source, sourceFacts)),
            "semantic analysis result must not be null"
        );
        var ruleGeneration = Objects.requireNonNull(
            ruleGenerationPort.generate(
                new RuleGenerationRequest(command.analysisRunId(), source, sourceFacts, semanticAnalysis)
            ),
            "rule generation result must not be null"
        );
        var result = RunRepositoryAnalysisResult.completed(
            command.analysisRunId(),
            source.metadata(),
            command.analysisProfile(),
            sourceFacts,
            semanticAnalysis,
            ruleGeneration
        );

        resultStore.store(result);
        return result;
    }
}
