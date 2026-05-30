package de.burger.forensics.analytics.services.repositorysource.bootstrap;

import de.burger.forensics.analytics.services.repositorysource.adapter.in.grpc.RepositorySourceGrpcEndpoint;
import de.burger.forensics.analytics.services.repositorysource.adapter.out.filesystem.FileSystemRepositoryWorkspaceAdapter;
import de.burger.forensics.analytics.services.repositorysource.adapter.out.git.GitRepositoryCheckoutAdapter;
import de.burger.forensics.analytics.services.repositorysource.adapter.out.git.GitRepositoryMetadataAdapter;
import de.burger.forensics.analytics.services.repositorysource.adapter.out.git.SafeGitCommandRunner;
import de.burger.forensics.analytics.services.repositorysource.adapter.out.git.SourceRootDetector;
import de.burger.forensics.analytics.services.repositorysource.adapter.out.h2.H2RepositorySourcePersistenceAdapter;
import de.burger.forensics.analytics.services.repositorysource.adapter.out.id.UuidRepositoryWorkspaceIdGenerator;
import de.burger.forensics.analytics.services.repositorysource.adapter.out.memory.InMemoryRepositoryPreparationRepository;
import de.burger.forensics.analytics.services.repositorysource.adapter.out.memory.InMemoryRepositorySourceIdempotencyRepository;
import de.burger.forensics.analytics.services.repositorysource.adapter.out.memory.InMemoryRepositoryWorkspaceRepository;
import de.burger.forensics.analytics.services.repositorysource.adapter.out.postgres.PostgresRepositorySourcePersistenceAdapter;
import de.burger.forensics.analytics.services.repositorysource.application.RepositorySourceApplicationService;
import de.burger.forensics.analytics.services.repositorysource.application.RepositoryWorkspaceApplicationService;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryCheckoutPort;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryMetadataPort;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryPreparationRepository;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositorySourceIdempotencyRepository;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryWorkspaceIdGenerator;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryWorkspacePort;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryWorkspaceRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration(proxyBeanMethods = false)
public class RepositorySourceServiceConfiguration {
    private final PostgresMigrationRunner postgresMigrationRunner;
    private final PostgresPersistenceComponentsFactory postgresPersistenceComponentsFactory;

    public RepositorySourceServiceConfiguration() {
        this(
            postgres -> new RepositorySourcePostgresLiquibaseMigration(postgres).migrate(),
            RepositorySourceServiceConfiguration::postgresPersistenceComponents
        );
    }

    RepositorySourceServiceConfiguration(
        PostgresMigrationRunner postgresMigrationRunner,
        PostgresPersistenceComponentsFactory postgresPersistenceComponentsFactory
    ) {
        this.postgresMigrationRunner = postgresMigrationRunner;
        this.postgresPersistenceComponentsFactory = postgresPersistenceComponentsFactory;
    }

    @Bean
    public Clock repositorySourceClock() {
        return Clock.systemUTC();
    }

    @Bean
    public RepositorySourcePersistenceComponents repositorySourcePersistenceComponents(
        RepositorySourceServiceProperties properties
    ) {
        if (properties.persistence().useH2()) {
            var adapter = new H2RepositorySourcePersistenceAdapter(
                properties.persistence().h2().jdbcUrl(),
                properties.persistence().h2().username(),
                properties.persistence().h2().password()
            );
            return new RepositorySourcePersistenceComponents(adapter, adapter, adapter);
        }
        if (properties.persistence().usePostgres()) {
            return migratedPostgresPersistenceComponents(properties.persistence().postgres());
        }
        return new RepositorySourcePersistenceComponents(
            new InMemoryRepositoryPreparationRepository(),
            new InMemoryRepositoryWorkspaceRepository(),
            new InMemoryRepositorySourceIdempotencyRepository()
        );
    }

    @Bean
    public RepositoryPreparationRepository repositoryPreparationRepository(RepositorySourcePersistenceComponents components) {
        return components.preparationRepository();
    }

    @Bean
    public RepositoryWorkspaceRepository repositoryWorkspaceRepository(RepositorySourcePersistenceComponents components) {
        return components.workspaceRepository();
    }

    @Bean
    public RepositorySourceIdempotencyRepository repositorySourceIdempotencyRepository(
        RepositorySourcePersistenceComponents components
    ) {
        return components.idempotencyRepository();
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
    public RepositoryMetadataPort repositoryMetadataPort(RepositorySourceServiceProperties properties) {
        return new GitRepositoryMetadataAdapter(new SafeGitCommandRunner(), properties.workspace().root().resolve("metadata"));
    }

    @Bean
    public RepositoryWorkspaceIdGenerator repositoryWorkspaceIdGenerator() {
        return new UuidRepositoryWorkspaceIdGenerator();
    }

    @Bean(destroyMethod = "shutdown")
    public ExecutorService repositoryWorkspaceCheckoutExecutor() {
        return Executors.newFixedThreadPool(2);
    }

    @Bean
    public RepositorySourceApplicationService repositorySourceApplicationService(
        @Qualifier("repositoryPreparationRepository") RepositoryPreparationRepository repository,
        @Qualifier("repositorySourceIdempotencyRepository") RepositorySourceIdempotencyRepository idempotencyRepository,
        RepositoryWorkspacePort workspacePort,
        RepositoryCheckoutPort checkoutPort,
        Clock repositorySourceClock
    ) {
        return new RepositorySourceApplicationService(
            repository,
            idempotencyRepository,
            workspacePort,
            checkoutPort,
            repositorySourceClock
        );
    }

    @Bean
    public RepositoryWorkspaceApplicationService repositoryWorkspaceApplicationService(
        @Qualifier("repositoryWorkspaceRepository") RepositoryWorkspaceRepository repository,
        RepositoryWorkspaceIdGenerator idGenerator,
        @Qualifier("repositorySourceIdempotencyRepository") RepositorySourceIdempotencyRepository idempotencyRepository,
        RepositoryWorkspacePort workspacePort,
        RepositoryCheckoutPort checkoutPort,
        RepositoryMetadataPort metadataPort,
        Clock repositorySourceClock,
        ExecutorService repositoryWorkspaceCheckoutExecutor
    ) {
        return new RepositoryWorkspaceApplicationService(
            repository,
            idGenerator,
            idempotencyRepository,
            workspacePort,
            checkoutPort,
            metadataPort,
            repositorySourceClock,
            repositoryWorkspaceCheckoutExecutor
        );
    }

    @Bean
    public RepositorySourceGrpcEndpoint repositorySourceGrpcEndpoint(
        RepositorySourceApplicationService applicationService,
        RepositoryWorkspaceApplicationService workspaceApplicationService
    ) {
        return new RepositorySourceGrpcEndpoint(applicationService, workspaceApplicationService);
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
        GrpcServerLifecycle grpcServerLifecycle,
        RepositorySourcePersistenceComponents persistenceComponents
    ) {
        return new HealthHttpServerLifecycle(properties, grpcServerLifecycle, persistenceComponents.storageReadiness());
    }

    private RepositorySourcePersistenceComponents migratedPostgresPersistenceComponents(
        RepositorySourceServiceProperties.Postgres postgres
    ) {
        try {
            postgresMigrationRunner.migrate(postgres);
            return postgresPersistenceComponentsFactory.create(postgres);
        } catch (RuntimeException error) {
            throw new IllegalStateException("Repository-source PostgreSQL storage is not ready");
        }
    }

    private static RepositorySourcePersistenceComponents postgresPersistenceComponents(
        RepositorySourceServiceProperties.Postgres postgres
    ) {
        var adapter = new PostgresRepositorySourcePersistenceAdapter(
            postgres.jdbcUrl(),
            postgres.username(),
            postgres.password(),
            postgres.schema()
        );
        return new RepositorySourcePersistenceComponents(adapter, adapter, adapter);
    }

    public record RepositorySourcePersistenceComponents(
        RepositoryPreparationRepository preparationRepository,
        RepositoryWorkspaceRepository workspaceRepository,
        RepositorySourceIdempotencyRepository idempotencyRepository,
        RepositorySourceStorageReadiness storageReadiness
    ) {
        public RepositorySourcePersistenceComponents(
            RepositoryPreparationRepository preparationRepository,
            RepositoryWorkspaceRepository workspaceRepository,
            RepositorySourceIdempotencyRepository idempotencyRepository
        ) {
            this(preparationRepository, workspaceRepository, idempotencyRepository, RepositorySourceStorageReadiness.ready());
        }
    }

    @FunctionalInterface
    interface PostgresMigrationRunner {
        void migrate(RepositorySourceServiceProperties.Postgres postgres);
    }

    @FunctionalInterface
    interface PostgresPersistenceComponentsFactory {
        RepositorySourcePersistenceComponents create(RepositorySourceServiceProperties.Postgres postgres);
    }
}
