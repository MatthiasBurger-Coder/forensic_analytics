package de.burger.forensics.analytics.services.gateway.bootstrap;

import de.burger.forensics.analytics.services.gateway.adapter.in.http.GatewayHttpHandler;
import de.burger.forensics.analytics.services.gateway.adapter.out.grpc.AnalysisStoreRepositoryToBtmGrpcClient;
import de.burger.forensics.analytics.services.gateway.application.GatewayRepositoryAnalysisSubmissionService;
import de.burger.forensics.analytics.services.gateway.application.GatewayStatusService;
import de.burger.forensics.analytics.services.gateway.application.port.RepositoryToBtmOrchestrationPort;
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
        RepositoryToBtmOrchestrationPort repositoryToBtmOrchestrationPort
    ) {
        return new GatewayRepositoryAnalysisSubmissionService(repositoryToBtmOrchestrationPort);
    }

    @Bean(destroyMethod = "close")
    public RepositoryToBtmOrchestrationPort repositoryToBtmOrchestrationPort(
        ForensicGatewayServiceProperties properties
    ) {
        var grpc = properties.analysisStore().grpc();
        return new AnalysisStoreRepositoryToBtmGrpcClient(grpc.host(), grpc.port(), grpc.deadlineSeconds());
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
