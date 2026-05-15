package de.burger.forensics.analytics.boot.rest;

import de.burger.forensics.analytics.application.ingestion.RepositoryAnalysisIngestionUseCase;
import de.burger.forensics.analytics.application.ingestion.RepositoryAnalysisQueryUseCase;
import de.burger.forensics.analytics.boot.config.ForensicAnalyticsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class RestApiServerConfiguration {
    @Bean
    public RestApiServerLifecycle restApiServerLifecycle(
        ForensicAnalyticsProperties properties,
        RepositoryAnalysisIngestionUseCase ingestionUseCase,
        RepositoryAnalysisQueryUseCase queryUseCase
    ) {
        return new RestApiServerLifecycle(properties, ingestionUseCase, queryUseCase);
    }
}
