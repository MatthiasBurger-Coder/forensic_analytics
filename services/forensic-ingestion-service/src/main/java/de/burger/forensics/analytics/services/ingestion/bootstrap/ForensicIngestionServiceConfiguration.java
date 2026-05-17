package de.burger.forensics.analytics.services.ingestion.bootstrap;

import de.burger.forensics.analytics.services.ingestion.adapter.in.grpc.ForensicIngestionGrpcEndpoint;
import de.burger.forensics.analytics.services.ingestion.adapter.out.memory.InMemoryIngestionSessionRepository;
import de.burger.forensics.analytics.services.ingestion.adapter.out.memory.NoOpAcceptedIngestionHandoffPort;
import de.burger.forensics.analytics.services.ingestion.application.IngestionApplicationService;
import de.burger.forensics.analytics.services.ingestion.application.port.AcceptedIngestionHandoffPort;
import de.burger.forensics.analytics.services.ingestion.application.port.IngestionSessionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ForensicIngestionServiceConfiguration {
    @Bean
    public IngestionSessionRepository ingestionSessionRepository() {
        return new InMemoryIngestionSessionRepository();
    }

    @Bean
    public AcceptedIngestionHandoffPort acceptedIngestionHandoffPort() {
        return new NoOpAcceptedIngestionHandoffPort();
    }

    @Bean
    public IngestionApplicationService ingestionApplicationService(
        IngestionSessionRepository ingestionSessionRepository,
        AcceptedIngestionHandoffPort acceptedIngestionHandoffPort
    ) {
        return new IngestionApplicationService(ingestionSessionRepository, acceptedIngestionHandoffPort);
    }

    @Bean
    public ForensicIngestionGrpcEndpoint forensicIngestionGrpcEndpoint(
        IngestionApplicationService ingestionApplicationService
    ) {
        return new ForensicIngestionGrpcEndpoint(ingestionApplicationService);
    }

    @Bean
    public GrpcServerLifecycle grpcServerLifecycle(
        ForensicIngestionServiceProperties properties,
        ForensicIngestionGrpcEndpoint endpoint
    ) {
        return new GrpcServerLifecycle(properties, endpoint);
    }

    @Bean
    public HealthHttpServerLifecycle healthHttpServerLifecycle(
        ForensicIngestionServiceProperties properties,
        GrpcServerLifecycle grpcServerLifecycle
    ) {
        return new HealthHttpServerLifecycle(properties, grpcServerLifecycle);
    }
}
