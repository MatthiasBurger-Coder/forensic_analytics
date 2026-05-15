package de.burger.forensics.analytics.bootstrap;

import de.burger.forensics.analytics.ingestion.grpc.ForensicIngestionGrpcService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.util.Objects;

public final class GrpcIngestionServerFactory {
    public Server create(GrpcIngestionServerSettings settings) {
        return create(settings, ForensicAnalyticsBackendComponents.createDefault());
    }

    Server create(GrpcIngestionServerSettings settings, ForensicAnalyticsBackendComponents components) {
        Objects.requireNonNull(settings, "settings must not be null");
        Objects.requireNonNull(components, "components must not be null");
        var service = new ForensicIngestionGrpcService(
            components.ingestionUseCase(),
            components.repositoryAnalysisIngestionUseCase()
        );
        return ServerBuilder.forPort(settings.port())
            .addService(service)
            .build();
    }
}
