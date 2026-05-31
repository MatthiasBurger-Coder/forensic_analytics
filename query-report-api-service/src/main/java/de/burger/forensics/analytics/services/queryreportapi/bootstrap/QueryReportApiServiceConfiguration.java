package de.burger.forensics.analytics.services.queryreportapi.bootstrap;

import de.burger.forensics.analytics.services.queryreportapi.adapter.in.http.QueryReportApiHttpHandler;
import de.burger.forensics.analytics.services.queryreportapi.adapter.out.grpc.AnalysisOrchestratorRepositoryToBtmGrpcClient;
import de.burger.forensics.analytics.services.queryreportapi.adapter.out.grpc.RepositorySourceSettingsGrpcClient;
import de.burger.forensics.analytics.services.queryreportapi.adapter.out.grpc.RepositorySourceWorkspaceGrpcClient;
import de.burger.forensics.analytics.services.queryreportapi.application.QueryReportApiRepositoryAnalysisSubmissionService;
import de.burger.forensics.analytics.services.queryreportapi.application.QueryReportApiSettingsService;
import de.burger.forensics.analytics.services.queryreportapi.application.QueryReportApiStatusService;
import de.burger.forensics.analytics.services.queryreportapi.application.QueryReportApiWorkspaceService;
import de.burger.forensics.analytics.services.queryreportapi.application.port.RepositoryAnalysisOwnerPort;
import de.burger.forensics.analytics.services.queryreportapi.application.port.RepositorySourceSettingsOwnerPort;
import de.burger.forensics.analytics.services.queryreportapi.application.port.RepositoryWorkspaceOwnerPort;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceFacadeConfiguration;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspacePolicy;
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

    @Bean
    public QueryReportApiWorkspaceService queryReportApiWorkspaceService(
        RepositoryWorkspaceOwnerPort repositoryWorkspaceOwnerPort,
        QueryReportApiServiceProperties properties
    ) {
        var workspace = properties.workspaceFacade();
        return new QueryReportApiWorkspaceService(
            repositoryWorkspaceOwnerPort,
            new WorkspaceFacadeConfiguration(
                workspace.schemaVersion(),
                workspace.metadataTimeoutSeconds(),
                new WorkspacePolicy(
                    workspace.refreshEphemeral(),
                    workspace.refreshAllowShallowClone(),
                    workspace.refreshAllowPartialClone(),
                    workspace.refreshAllowSparseCheckout(),
                    workspace.refreshTimeoutSeconds(),
                    workspace.refreshMaxWorkspaceBytes()
                )
            )
        );
    }

    @Bean(destroyMethod = "close")
    public RepositoryAnalysisOwnerPort repositoryAnalysisOwnerPort(
        QueryReportApiServiceProperties properties
    ) {
        var grpc = properties.analysisOrchestrator().grpc();
        return new AnalysisOrchestratorRepositoryToBtmGrpcClient(grpc.host(), grpc.port(), grpc.deadlineSeconds());
    }

    @Bean(destroyMethod = "close")
    public RepositoryWorkspaceOwnerPort repositoryWorkspaceOwnerPort(
        QueryReportApiServiceProperties properties
    ) {
        var grpc = properties.repositorySource().grpc();
        return new RepositorySourceWorkspaceGrpcClient(grpc.host(), grpc.port(), grpc.deadlineSeconds());
    }

    @Bean(destroyMethod = "close")
    public RepositorySourceSettingsOwnerPort repositorySourceSettingsOwnerPort(
        QueryReportApiServiceProperties properties
    ) {
        var grpc = properties.repositorySource().grpc();
        return new RepositorySourceSettingsGrpcClient(grpc.host(), grpc.port(), grpc.deadlineSeconds());
    }

    @Bean
    public QueryReportApiSettingsService queryReportApiSettingsService(
        RepositorySourceSettingsOwnerPort repositorySourceSettingsOwnerPort
    ) {
        return new QueryReportApiSettingsService(repositorySourceSettingsOwnerPort);
    }

    @Bean
    public QueryReportApiHttpHandler queryReportApiHttpHandler(
        QueryReportApiStatusService queryReportApiStatusService,
        QueryReportApiRepositoryAnalysisSubmissionService repositoryAnalysisSubmissionService,
        QueryReportApiWorkspaceService queryReportApiWorkspaceService,
        QueryReportApiSettingsService queryReportApiSettingsService,
        QueryReportApiServiceProperties properties
    ) {
        return new QueryReportApiHttpHandler(
            queryReportApiStatusService,
            repositoryAnalysisSubmissionService,
            queryReportApiWorkspaceService,
            queryReportApiSettingsService,
            properties.settingsFacade().operatorToken()
        );
    }

    @Bean
    public QueryReportApiHttpServerLifecycle queryReportApiHttpServerLifecycle(
        QueryReportApiServiceProperties properties,
        QueryReportApiHttpHandler queryReportApiHttpHandler
    ) {
        return new QueryReportApiHttpServerLifecycle(properties, queryReportApiHttpHandler);
    }
}
