package de.burger.forensics.analytics.application.analysis.port;

import de.burger.forensics.analytics.application.analysis.result.RunRepositoryAnalysisResult;

public interface RepositoryAnalysisResultStore {
    void store(RunRepositoryAnalysisResult result);
}
