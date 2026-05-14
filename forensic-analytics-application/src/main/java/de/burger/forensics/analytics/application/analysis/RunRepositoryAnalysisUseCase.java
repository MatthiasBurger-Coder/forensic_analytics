package de.burger.forensics.analytics.application.analysis;

import de.burger.forensics.analytics.application.analysis.command.RunRepositoryAnalysisCommand;
import de.burger.forensics.analytics.application.analysis.result.RunRepositoryAnalysisResult;

public interface RunRepositoryAnalysisUseCase {
    RunRepositoryAnalysisResult run(RunRepositoryAnalysisCommand command);
}
