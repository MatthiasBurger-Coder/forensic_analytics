package de.burger.forensics.analytics.bootstrap;

import de.burger.forensics.analytics.application.ingestion.DefaultForensicIngestionUseCase;
import de.burger.forensics.analytics.ingestion.grpc.ForensicIngestionGrpcService;
import de.burger.forensics.analytics.persistence.InMemoryIngestionSessionRepository;
import io.grpc.Server;
import io.grpc.ServerBuilder;

public final class GrpcIngestionServerFactory {
    public Server create(GrpcIngestionServerSettings settings) {
        var repository = new InMemoryIngestionSessionRepository();
        var useCase = new DefaultForensicIngestionUseCase(repository);
        var service = new ForensicIngestionGrpcService(useCase);
        return ServerBuilder.forPort(settings.port())
            .addService(service)
            .build();
    }
}
