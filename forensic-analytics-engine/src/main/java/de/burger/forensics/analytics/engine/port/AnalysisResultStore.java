package de.burger.forensics.analytics.engine.port;

import de.burger.forensics.analytics.engine.RepositoryAnalysisResult;

public interface AnalysisResultStore {
    void store(RepositoryAnalysisResult result);
}
