package de.burger.forensics.analytics.engine;

import de.burger.forensics.analytics.application.analysis.RunRepositoryAnalysisUseCase;
import de.burger.forensics.analytics.application.analysis.command.RunRepositoryAnalysisCommand;
import de.burger.forensics.analytics.application.analysis.result.RunRepositoryAnalysisResult;

import java.util.Objects;

public final class RepositoryAnalysisEngine {
    private final RunRepositoryAnalysisUseCase useCase;

    public RepositoryAnalysisEngine(RunRepositoryAnalysisUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "useCase must not be null");
    }

    public RunRepositoryAnalysisResult run(RunRepositoryAnalysisCommand command) {
        return useCase.run(Objects.requireNonNull(command, "command must not be null"));
    }
}
