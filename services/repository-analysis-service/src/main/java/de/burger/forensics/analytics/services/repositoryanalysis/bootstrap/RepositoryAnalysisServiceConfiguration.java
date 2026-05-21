package de.burger.forensics.analytics.services.repositoryanalysis.bootstrap;

import de.burger.forensics.analytics.services.repositoryanalysis.adapter.in.grpc.RepositoryAnalysisGrpcEndpoint;
import de.burger.forensics.analytics.services.repositoryanalysis.adapter.out.filesystem.FileSystemRepositoryWorkspaceAdapter;
import de.burger.forensics.analytics.services.repositoryanalysis.adapter.out.filesystem.FileSystemSourceSnapshotFileCollector;
import de.burger.forensics.analytics.services.repositoryanalysis.adapter.out.git.GitRepositoryCheckoutAdapter;
import de.burger.forensics.analytics.services.repositoryanalysis.adapter.out.git.SafeGitCommandRunner;
import de.burger.forensics.analytics.services.repositoryanalysis.adapter.out.git.SourceRootDetector;
import de.burger.forensics.analytics.services.repositoryanalysis.adapter.out.grpc.JavaAstAnalysisGrpcClient;
import de.burger.forensics.analytics.services.repositoryanalysis.adapter.out.memory.InMemoryRepositoryPreparationRepository;
import de.burger.forensics.analytics.services.repositoryanalysis.application.RepositoryAnalysisApplicationService;
import de.burger.forensics.analytics.services.repositoryanalysis.application.RepositorySourceSnapshotHandoffService;
import de.burger.forensics.analytics.services.repositoryanalysis.application.port.JavaAstAnalysisPort;
import de.burger.forensics.analytics.services.repositoryanalysis.application.port.RepositoryCheckoutPort;
import de.burger.forensics.analytics.services.repositoryanalysis.application.port.RepositoryPreparationRepository;
import de.burger.forensics.analytics.services.repositoryanalysis.application.port.RepositoryWorkspacePort;
import de.burger.forensics.analytics.services.repositoryanalysis.application.port.SourceSnapshotFileCollectorPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class RepositoryAnalysisServiceConfiguration {
    @Bean
    public Clock repositoryAnalysisClock() {
        return Clock.systemUTC();
    }

    @Bean
    public RepositoryPreparationRepository repositoryPreparationRepository() {
        return new InMemoryRepositoryPreparationRepository();
    }

    @Bean
    public RepositoryWorkspacePort repositoryWorkspacePort(RepositoryAnalysisServiceProperties properties) {
        return new FileSystemRepositoryWorkspaceAdapter(properties.workspace().root());
    }

    @Bean
    public SourceSnapshotFileCollectorPort sourceSnapshotFileCollectorPort(RepositoryAnalysisServiceProperties properties) {
        return new FileSystemSourceSnapshotFileCollector(properties.workspace().root());
    }

    @Bean
    public RepositoryCheckoutPort repositoryCheckoutPort() {
        return new GitRepositoryCheckoutAdapter(new SafeGitCommandRunner(), new SourceRootDetector());
    }

    @Bean
    public JavaAstAnalysisPort javaAstAnalysisPort(RepositoryAnalysisServiceProperties properties) {
        var grpc = properties.javaAstAnalysis().grpc();
        return new JavaAstAnalysisGrpcClient(grpc.host(), grpc.port(), grpc.deadlineSeconds());
    }

    @Bean
    public RepositoryAnalysisApplicationService repositoryAnalysisApplicationService(
        RepositoryPreparationRepository repository,
        RepositoryWorkspacePort workspacePort,
        RepositoryCheckoutPort checkoutPort,
        Clock repositoryAnalysisClock
    ) {
        return new RepositoryAnalysisApplicationService(repository, workspacePort, checkoutPort, repositoryAnalysisClock);
    }

    @Bean
    public RepositorySourceSnapshotHandoffService repositorySourceSnapshotHandoffService(
        RepositoryPreparationRepository repository,
        SourceSnapshotFileCollectorPort sourceFileCollector,
        JavaAstAnalysisPort javaAstAnalysisPort
    ) {
        return new RepositorySourceSnapshotHandoffService(repository, sourceFileCollector, javaAstAnalysisPort);
    }

    @Bean
    public RepositoryAnalysisGrpcEndpoint repositoryAnalysisGrpcEndpoint(
        RepositoryAnalysisApplicationService applicationService,
        RepositorySourceSnapshotHandoffService handoffService
    ) {
        return new RepositoryAnalysisGrpcEndpoint(applicationService, handoffService);
    }

    @Bean
    public GrpcServerLifecycle grpcServerLifecycle(
        RepositoryAnalysisServiceProperties properties,
        RepositoryAnalysisGrpcEndpoint endpoint
    ) {
        return new GrpcServerLifecycle(properties, endpoint);
    }

    @Bean
    public HealthHttpServerLifecycle healthHttpServerLifecycle(
        RepositoryAnalysisServiceProperties properties,
        GrpcServerLifecycle grpcServerLifecycle
    ) {
        return new HealthHttpServerLifecycle(properties, grpcServerLifecycle);
    }
}
