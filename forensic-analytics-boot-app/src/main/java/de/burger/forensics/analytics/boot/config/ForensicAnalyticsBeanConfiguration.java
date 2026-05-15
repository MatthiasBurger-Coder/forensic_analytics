package de.burger.forensics.analytics.boot.config;

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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ForensicAnalyticsBeanConfiguration {
    @Bean
    public IngestionSessionRepository ingestionSessionRepository() {
        return new InMemoryIngestionSessionRepository();
    }

    @Bean
    public AnalysisSessionRepository analysisSessionRepository() {
        return new InMemoryAnalysisSessionRepository();
    }

    @Bean
    public WorkspacePreparationPort workspacePreparationPort(ForensicAnalyticsProperties properties) {
        return new FileSystemWorkspacePreparationAdapter(properties.workspace().basePath());
    }

    @Bean
    public RepositoryCheckoutPort repositoryCheckoutPort() {
        return new GitRepositoryCheckoutAdapter();
    }

    @Bean
    public ForensicIngestionUseCase forensicIngestionUseCase(IngestionSessionRepository ingestionSessionRepository) {
        return new DefaultForensicIngestionUseCase(ingestionSessionRepository);
    }

    @Bean
    public RepositoryAnalysisIngestionUseCase repositoryAnalysisIngestionUseCase(
        WorkspacePreparationPort workspacePreparationPort,
        RepositoryCheckoutPort repositoryCheckoutPort,
        AnalysisSessionRepository analysisSessionRepository
    ) {
        return new DefaultRepositoryAnalysisIngestionUseCase(
            workspacePreparationPort,
            repositoryCheckoutPort,
            analysisSessionRepository
        );
    }

    @Bean
    public RepositoryAnalysisQueryUseCase repositoryAnalysisQueryUseCase(
        AnalysisSessionRepository analysisSessionRepository
    ) {
        return new DefaultRepositoryAnalysisQueryUseCase(analysisSessionRepository);
    }
}
