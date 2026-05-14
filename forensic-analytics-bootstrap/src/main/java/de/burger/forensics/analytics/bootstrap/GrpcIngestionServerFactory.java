package de.burger.forensics.analytics.bootstrap;

import de.burger.forensics.analytics.adapter.repository.source.FileSystemWorkspacePreparationAdapter;
import de.burger.forensics.analytics.adapter.repository.source.GitRepositoryCheckoutAdapter;
import de.burger.forensics.analytics.application.ingestion.DefaultForensicIngestionUseCase;
import de.burger.forensics.analytics.application.ingestion.DefaultRepositoryAnalysisIngestionUseCase;
import de.burger.forensics.analytics.ingestion.grpc.ForensicIngestionGrpcService;
import de.burger.forensics.analytics.persistence.InMemoryAnalysisSessionRepository;
import de.burger.forensics.analytics.persistence.InMemoryIngestionSessionRepository;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.nio.file.Path;

public final class GrpcIngestionServerFactory {
    public Server create(GrpcIngestionServerSettings settings) {
        var ingestionRepository = new InMemoryIngestionSessionRepository();
        var analysisSessionRepository = new InMemoryAnalysisSessionRepository();
        var ingestionUseCase = new DefaultForensicIngestionUseCase(ingestionRepository);
        var repositoryAnalysisUseCase = new DefaultRepositoryAnalysisIngestionUseCase(
            new FileSystemWorkspacePreparationAdapter(defaultWorkspaceRoot()),
            new GitRepositoryCheckoutAdapter(),
            analysisSessionRepository
        );
        var service = new ForensicIngestionGrpcService(ingestionUseCase, repositoryAnalysisUseCase);
        return ServerBuilder.forPort(settings.port())
            .addService(service)
            .build();
    }

    private static Path defaultWorkspaceRoot() {
        return Path.of(System.getProperty("java.io.tmpdir"), "forensic-analytics-workspaces");
    }
}
