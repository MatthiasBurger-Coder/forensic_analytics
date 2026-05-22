package de.burger.forensics.analytics.services.queryreportapi.bootstrap;

import de.burger.forensics.analytics.services.queryreportapi.adapter.in.http.QueryReportApiHttpHandler;
import de.burger.forensics.analytics.services.queryreportapi.adapter.out.grpc.AnalysisOrchestratorRepositoryToBtmGrpcClient;
import de.burger.forensics.analytics.services.queryreportapi.application.QueryReportApiRepositoryAnalysisSubmissionService;
import de.burger.forensics.analytics.services.queryreportapi.application.QueryReportApiStatusService;
import de.burger.forensics.analytics.services.queryreportapi.application.port.RepositoryAnalysisOwnerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class QueryReportApiServiceConfiguration {
    @Bean
    public QueryReportApiStatusService queryReportApiStatusService() {
        return new QueryReportApiStatusService();
    }

    @Bean
    public QueryReportApiRepositoryAnalysisSubmissionService queryReportApiRepositoryAnalysisSubmissionService(
        RepositoryAnalysisOwnerPort repositoryAnalysisOwnerPort
    ) {
        return new QueryReportApiRepositoryAnalysisSubmissionService(repositoryAnalysisOwnerPort);
    }

    @Bean(destroyMethod = "close")
    public RepositoryAnalysisOwnerPort repositoryAnalysisOwnerPort(
        QueryReportApiServiceProperties properties
    ) {
        var grpc = properties.analysisOrchestrator().grpc();
        return new AnalysisOrchestratorRepositoryToBtmGrpcClient(grpc.host(), grpc.port(), grpc.deadlineSeconds());
    }

    @Bean
    public QueryReportApiHttpHandler queryReportApiHttpHandler(
        QueryReportApiStatusService queryReportApiStatusService,
        QueryReportApiRepositoryAnalysisSubmissionService repositoryAnalysisSubmissionService
    ) {
        return new QueryReportApiHttpHandler(queryReportApiStatusService, repositoryAnalysisSubmissionService);
    }

    @Bean
    public QueryReportApiHttpServerLifecycle queryReportApiHttpServerLifecycle(
        QueryReportApiServiceProperties properties,
        QueryReportApiHttpHandler queryReportApiHttpHandler
    ) {
        return new QueryReportApiHttpServerLifecycle(properties, queryReportApiHttpHandler);
    }
}
