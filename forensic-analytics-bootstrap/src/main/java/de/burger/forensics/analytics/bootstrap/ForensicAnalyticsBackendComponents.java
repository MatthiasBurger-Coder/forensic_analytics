package de.burger.forensics.analytics.bootstrap;

import de.burger.forensics.analytics.adapter.repository.source.FileSystemWorkspacePreparationAdapter;
import de.burger.forensics.analytics.adapter.repository.source.GitRepositoryCheckoutAdapter;
import de.burger.forensics.analytics.application.ingestion.DefaultForensicIngestionUseCase;
import de.burger.forensics.analytics.application.ingestion.DefaultRepositoryAnalysisIngestionUseCase;
import de.burger.forensics.analytics.application.ingestion.DefaultRepositoryAnalysisQueryUseCase;
import de.burger.forensics.analytics.application.ingestion.ForensicIngestionUseCase;
import de.burger.forensics.analytics.application.ingestion.RepositoryAnalysisIngestionUseCase;
import de.burger.forensics.analytics.application.ingestion.RepositoryAnalysisQueryUseCase;
import de.burger.forensics.analytics.application.ingestion.port.AnalysisSessionRepository;
import de.burger.forensics.analytics.application.ingestion.port.IngestionSessionRepository;
import de.burger.forensics.analytics.application.ingestion.port.RepositoryCheckoutPort;
import de.burger.forensics.analytics.application.ingestion.port.WorkspacePreparationPort;
import de.burger.forensics.analytics.persistence.InMemoryAnalysisSessionRepository;
import de.burger.forensics.analytics.persistence.InMemoryIngestionSessionRepository;

import java.nio.file.Path;
import java.util.Objects;

final class ForensicAnalyticsBackendComponents {
    private final ForensicIngestionUseCase ingestionUseCase;
    private final RepositoryAnalysisIngestionUseCase repositoryAnalysisIngestionUseCase;
    private final RepositoryAnalysisQueryUseCase repositoryAnalysisQueryUseCase;

    private ForensicAnalyticsBackendComponents(
        ForensicIngestionUseCase ingestionUseCase,
        RepositoryAnalysisIngestionUseCase repositoryAnalysisIngestionUseCase,
        RepositoryAnalysisQueryUseCase repositoryAnalysisQueryUseCase
    ) {
        this.ingestionUseCase = Objects.requireNonNull(ingestionUseCase, "ingestionUseCase must not be null");
        this.repositoryAnalysisIngestionUseCase = Objects.requireNonNull(
            repositoryAnalysisIngestionUseCase,
            "repositoryAnalysisIngestionUseCase must not be null"
        );
        this.repositoryAnalysisQueryUseCase = Objects.requireNonNull(
            repositoryAnalysisQueryUseCase,
            "repositoryAnalysisQueryUseCase must not be null"
        );
    }

    static ForensicAnalyticsBackendComponents createDefault() {
        return create(
            new InMemoryIngestionSessionRepository(),
            new InMemoryAnalysisSessionRepository(),
            new FileSystemWorkspacePreparationAdapter(defaultWorkspaceRoot()),
            new GitRepositoryCheckoutAdapter()
        );
    }

    static ForensicAnalyticsBackendComponents create(
        IngestionSessionRepository ingestionSessionRepository,
        AnalysisSessionRepository analysisSessionRepository,
        WorkspacePreparationPort workspacePreparationPort,
        RepositoryCheckoutPort repositoryCheckoutPort
    ) {
        Objects.requireNonNull(analysisSessionRepository, "analysisSessionRepository must not be null");
        return new ForensicAnalyticsBackendComponents(
            new DefaultForensicIngestionUseCase(ingestionSessionRepository),
            new DefaultRepositoryAnalysisIngestionUseCase(
                workspacePreparationPort,
                repositoryCheckoutPort,
                analysisSessionRepository
            ),
            new DefaultRepositoryAnalysisQueryUseCase(analysisSessionRepository)
        );
    }

    ForensicIngestionUseCase ingestionUseCase() {
        return ingestionUseCase;
    }

    RepositoryAnalysisIngestionUseCase repositoryAnalysisIngestionUseCase() {
        return repositoryAnalysisIngestionUseCase;
    }

    RepositoryAnalysisQueryUseCase repositoryAnalysisQueryUseCase() {
        return repositoryAnalysisQueryUseCase;
    }

    private static Path defaultWorkspaceRoot() {
        return Path.of(System.getProperty("java.io.tmpdir"), "forensic-analytics-workspaces");
    }
}
