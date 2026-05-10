package de.burger.forensics.analytics.engine;

import de.burger.forensics.analytics.engine.port.AnalysisResultStore;
import de.burger.forensics.analytics.engine.port.RepositorySourceProvider;
import de.burger.forensics.analytics.engine.port.SourceFactScanner;

import java.util.Objects;

public final class RepositoryAnalysisEngine {
    private final RepositorySourceProvider sourceProvider;
    private final SourceFactScanner sourceFactScanner;
    private final AnalysisResultStore resultStore;

    public RepositoryAnalysisEngine(
        RepositorySourceProvider sourceProvider,
        SourceFactScanner sourceFactScanner,
        AnalysisResultStore resultStore
    ) {
        this.sourceProvider = Objects.requireNonNull(sourceProvider, "sourceProvider must not be null");
        this.sourceFactScanner = Objects.requireNonNull(sourceFactScanner, "sourceFactScanner must not be null");
        this.resultStore = Objects.requireNonNull(resultStore, "resultStore must not be null");
    }

    public RepositoryAnalysisResult run(RepositoryAnalysisRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        var source = Objects.requireNonNull(
            sourceProvider.resolve(request.repositoryMetadata()),
            "repository source must not be null"
        );
        var facts = Objects.requireNonNull(sourceFactScanner.scan(source), "source facts must not be null");
        var result = RepositoryAnalysisResult.completed(
            request.analysisRunId(),
            source.metadata(),
            request.analysisProfile(),
            facts
        );

        resultStore.store(result);
        return result;
    }
}
