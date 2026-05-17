package de.burger.forensics.analytics.services.analysisstore.bootstrap;

import de.burger.forensics.analytics.services.analysisstore.adapter.in.grpc.AnalysisJobGrpcEndpoint;
import de.burger.forensics.analytics.services.analysisstore.adapter.out.memory.InMemoryAnalysisJobRepository;
import de.burger.forensics.analytics.services.analysisstore.application.AnalysisJobApplicationService;
import de.burger.forensics.analytics.services.analysisstore.application.port.AnalysisJobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class AnalysisStoreServiceConfiguration {
    @Bean
    public Clock analysisStoreClock() {
        return Clock.systemUTC();
    }

    @Bean
    public AnalysisJobRepository analysisJobRepository() {
        return new InMemoryAnalysisJobRepository();
    }

    @Bean
    public AnalysisJobApplicationService analysisJobApplicationService(
        AnalysisJobRepository analysisJobRepository,
        Clock analysisStoreClock
    ) {
        return new AnalysisJobApplicationService(analysisJobRepository, analysisStoreClock);
    }

    @Bean
    public AnalysisJobGrpcEndpoint analysisJobGrpcEndpoint(AnalysisJobApplicationService applicationService) {
        return new AnalysisJobGrpcEndpoint(applicationService);
    }

    @Bean
    public GrpcServerLifecycle grpcServerLifecycle(
        AnalysisStoreServiceProperties properties,
        AnalysisJobGrpcEndpoint endpoint
    ) {
        return new GrpcServerLifecycle(properties, endpoint);
    }

    @Bean
    public HealthHttpServerLifecycle healthHttpServerLifecycle(
        AnalysisStoreServiceProperties properties,
        GrpcServerLifecycle grpcServerLifecycle
    ) {
        return new HealthHttpServerLifecycle(properties, grpcServerLifecycle);
    }
}
