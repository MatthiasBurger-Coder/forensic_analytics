package de.burger.forensics.analytics.services.repositorysource.bootstrap;

import de.burger.forensics.analytics.services.repositorysource.adapter.in.grpc.RepositorySourceGrpcEndpoint;
import de.burger.forensics.analytics.services.repositorysource.adapter.out.filesystem.FileSystemRepositoryWorkspaceAdapter;
import de.burger.forensics.analytics.services.repositorysource.adapter.out.git.GitRepositoryCheckoutAdapter;
import de.burger.forensics.analytics.services.repositorysource.adapter.out.git.SafeGitCommandRunner;
import de.burger.forensics.analytics.services.repositorysource.adapter.out.git.SourceRootDetector;
import de.burger.forensics.analytics.services.repositorysource.adapter.out.memory.InMemoryRepositoryPreparationRepository;
import de.burger.forensics.analytics.services.repositorysource.application.RepositorySourceApplicationService;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryCheckoutPort;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryPreparationRepository;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryWorkspacePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class RepositorySourceServiceConfiguration {
    @Bean
    public Clock repositorySourceClock() {
        return Clock.systemUTC();
    }

    @Bean
    public RepositoryPreparationRepository repositoryPreparationRepository() {
        return new InMemoryRepositoryPreparationRepository();
    }

    @Bean
    public RepositoryWorkspacePort repositoryWorkspacePort(RepositorySourceServiceProperties properties) {
        return new FileSystemRepositoryWorkspaceAdapter(properties.workspace().root());
    }

    @Bean
    public RepositoryCheckoutPort repositoryCheckoutPort() {
        return new GitRepositoryCheckoutAdapter(new SafeGitCommandRunner(), new SourceRootDetector());
    }

    @Bean
    public RepositorySourceApplicationService repositorySourceApplicationService(
        RepositoryPreparationRepository repository,
        RepositoryWorkspacePort workspacePort,
        RepositoryCheckoutPort checkoutPort,
        Clock repositorySourceClock
    ) {
        return new RepositorySourceApplicationService(repository, workspacePort, checkoutPort, repositorySourceClock);
    }

    @Bean
    public RepositorySourceGrpcEndpoint repositorySourceGrpcEndpoint(RepositorySourceApplicationService applicationService) {
        return new RepositorySourceGrpcEndpoint(applicationService);
    }

    @Bean
    public GrpcServerLifecycle grpcServerLifecycle(
        RepositorySourceServiceProperties properties,
        RepositorySourceGrpcEndpoint endpoint
    ) {
        return new GrpcServerLifecycle(properties, endpoint);
    }

    @Bean
    public HealthHttpServerLifecycle healthHttpServerLifecycle(
        RepositorySourceServiceProperties properties,
        GrpcServerLifecycle grpcServerLifecycle
    ) {
        return new HealthHttpServerLifecycle(properties, grpcServerLifecycle);
    }
}
