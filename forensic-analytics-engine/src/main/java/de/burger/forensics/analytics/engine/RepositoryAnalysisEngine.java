package de.burger.forensics.analytics.engine;

import de.burger.forensics.analytics.application.analysis.RunRepositoryAnalysisUseCase;
import de.burger.forensics.analytics.application.analysis.command.RunRepositoryAnalysisCommand;
import de.burger.forensics.analytics.application.analysis.result.RunRepositoryAnalysisResult;
import de.burger.forensics.analytics.observability.OperationLogger;

import java.util.Objects;

public final class RepositoryAnalysisEngine {
    private final RunRepositoryAnalysisUseCase useCase;
    private final OperationLogger operationLogger;

    public RepositoryAnalysisEngine(RunRepositoryAnalysisUseCase useCase) {
        this(useCase, OperationLogger.system(RepositoryAnalysisEngine.class));
    }

    RepositoryAnalysisEngine(RunRepositoryAnalysisUseCase useCase, OperationLogger operationLogger) {
        this.useCase = Objects.requireNonNull(useCase, "useCase must not be null");
        this.operationLogger = Objects.requireNonNull(operationLogger, "operationLogger must not be null");
    }

    public RunRepositoryAnalysisResult run(RunRepositoryAnalysisCommand command) {
        var verifiedCommand = Objects.requireNonNull(command, "command must not be null");
        return operationLogger.logged("engine.repository-analysis", () -> useCase.run(verifiedCommand));
    }
}
