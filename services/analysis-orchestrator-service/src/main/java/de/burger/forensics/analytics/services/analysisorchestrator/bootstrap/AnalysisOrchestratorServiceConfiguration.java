package de.burger.forensics.analytics.services.analysisorchestrator.bootstrap;

import de.burger.forensics.analytics.services.analysisorchestrator.adapter.in.grpc.AnalysisJobGrpcEndpoint;
import de.burger.forensics.analytics.services.analysisorchestrator.adapter.out.memory.InMemoryAnalysisJobRepository;
import de.burger.forensics.analytics.services.analysisorchestrator.application.AnalysisJobApplicationService;
import de.burger.forensics.analytics.services.analysisorchestrator.application.port.AnalysisJobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class AnalysisOrchestratorServiceConfiguration {
    @Bean
    public Clock analysisOrchestratorClock() {
        return Clock.systemUTC();
    }

    @Bean
    public AnalysisJobRepository analysisJobRepository() {
        return new InMemoryAnalysisJobRepository();
    }

    @Bean
    public AnalysisJobApplicationService analysisJobApplicationService(
        AnalysisJobRepository analysisJobRepository,
        Clock analysisOrchestratorClock
    ) {
        return new AnalysisJobApplicationService(analysisJobRepository, analysisOrchestratorClock);
    }

    @Bean
    public AnalysisJobGrpcEndpoint analysisJobGrpcEndpoint(AnalysisJobApplicationService applicationService) {
        return new AnalysisJobGrpcEndpoint(applicationService);
    }

    @Bean
    public GrpcServerLifecycle grpcServerLifecycle(
        AnalysisOrchestratorServiceProperties properties,
        AnalysisJobGrpcEndpoint endpoint
    ) {
        return new GrpcServerLifecycle(properties, endpoint);
    }

    @Bean
    public HealthHttpServerLifecycle healthHttpServerLifecycle(
        AnalysisOrchestratorServiceProperties properties,
        GrpcServerLifecycle grpcServerLifecycle
    ) {
        return new HealthHttpServerLifecycle(properties, grpcServerLifecycle);
    }
}
