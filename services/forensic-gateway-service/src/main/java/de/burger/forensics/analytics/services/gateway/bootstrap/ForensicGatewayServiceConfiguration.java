package de.burger.forensics.analytics.services.gateway.bootstrap;

import de.burger.forensics.analytics.services.gateway.adapter.in.http.GatewayHttpHandler;
import de.burger.forensics.analytics.services.gateway.adapter.out.grpc.RepositoryAnalysisGrpcClient;
import de.burger.forensics.analytics.services.gateway.application.GatewayRepositoryAnalysisSubmissionService;
import de.burger.forensics.analytics.services.gateway.application.GatewayStatusService;
import de.burger.forensics.analytics.services.gateway.application.port.RepositoryAnalysisPreparationPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ForensicGatewayServiceConfiguration {
    @Bean
    public GatewayStatusService gatewayStatusService() {
        return new GatewayStatusService();
    }

    @Bean
    public GatewayRepositoryAnalysisSubmissionService gatewayRepositoryAnalysisSubmissionService(
        RepositoryAnalysisPreparationPort repositoryAnalysisPreparationPort
    ) {
        return new GatewayRepositoryAnalysisSubmissionService(repositoryAnalysisPreparationPort);
    }

    @Bean(destroyMethod = "close")
    public RepositoryAnalysisPreparationPort repositoryAnalysisPreparationPort(
        ForensicGatewayServiceProperties properties
    ) {
        var grpc = properties.repositoryAnalysis().grpc();
        return new RepositoryAnalysisGrpcClient(grpc.host(), grpc.port(), grpc.deadlineSeconds());
    }

    @Bean
    public GatewayHttpHandler gatewayHttpHandler(
        GatewayStatusService gatewayStatusService,
        GatewayRepositoryAnalysisSubmissionService repositoryAnalysisSubmissionService
    ) {
        return new GatewayHttpHandler(gatewayStatusService, repositoryAnalysisSubmissionService);
    }

    @Bean
    public GatewayHttpServerLifecycle gatewayHttpServerLifecycle(
        ForensicGatewayServiceProperties properties,
        GatewayHttpHandler gatewayHttpHandler
    ) {
        return new GatewayHttpServerLifecycle(properties, gatewayHttpHandler);
    }
}
