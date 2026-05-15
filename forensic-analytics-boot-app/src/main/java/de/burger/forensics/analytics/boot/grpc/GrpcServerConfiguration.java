package de.burger.forensics.analytics.boot.grpc;

import de.burger.forensics.analytics.application.ingestion.ForensicIngestionUseCase;
import de.burger.forensics.analytics.application.ingestion.RepositoryAnalysisIngestionUseCase;
import de.burger.forensics.analytics.boot.config.ForensicAnalyticsProperties;
import de.burger.forensics.analytics.ingestion.grpc.ForensicIngestionGrpcService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class GrpcServerConfiguration {
    @Bean
    public ForensicIngestionGrpcService forensicIngestionGrpcService(
        ForensicIngestionUseCase forensicIngestionUseCase,
        RepositoryAnalysisIngestionUseCase repositoryAnalysisIngestionUseCase
    ) {
        return new ForensicIngestionGrpcService(forensicIngestionUseCase, repositoryAnalysisIngestionUseCase);
    }

    @Bean
    public GrpcServerLifecycle grpcServerLifecycle(
        ForensicAnalyticsProperties properties,
        ForensicIngestionGrpcService service
    ) {
        return new GrpcServerLifecycle(properties, service);
    }
}
