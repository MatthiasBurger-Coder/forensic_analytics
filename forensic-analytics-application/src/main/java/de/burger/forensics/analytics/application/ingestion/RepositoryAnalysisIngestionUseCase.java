package de.burger.forensics.analytics.application.ingestion;

import de.burger.forensics.analytics.application.ingestion.command.AnalyzeRepositoryCommand;
import de.burger.forensics.analytics.application.ingestion.result.AnalyzeRepositoryResult;

public interface RepositoryAnalysisIngestionUseCase {
    AnalyzeRepositoryResult analyze(AnalyzeRepositoryCommand command);
}
