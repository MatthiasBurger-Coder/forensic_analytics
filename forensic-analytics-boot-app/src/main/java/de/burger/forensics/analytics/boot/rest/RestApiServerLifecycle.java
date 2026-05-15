package de.burger.forensics.analytics.boot.rest;

import de.burger.forensics.analytics.application.ingestion.RepositoryAnalysisIngestionUseCase;
import de.burger.forensics.analytics.application.ingestion.RepositoryAnalysisQueryUseCase;
import de.burger.forensics.analytics.boot.config.ForensicAnalyticsProperties;
import de.burger.forensics.analytics.rest.RestApiServer;
import de.burger.forensics.analytics.rest.RestApiServerFactory;
import org.springframework.context.SmartLifecycle;

import java.io.IOException;
import java.net.InetSocketAddress;

public final class RestApiServerLifecycle implements SmartLifecycle {
    private final ForensicAnalyticsProperties properties;
    private final RepositoryAnalysisIngestionUseCase ingestionUseCase;
    private final RepositoryAnalysisQueryUseCase queryUseCase;
    private RestApiServer server;
    private boolean running;

    public RestApiServerLifecycle(
        ForensicAnalyticsProperties properties,
        RepositoryAnalysisIngestionUseCase ingestionUseCase,
        RepositoryAnalysisQueryUseCase queryUseCase
    ) {
        this.properties = properties;
        this.ingestionUseCase = ingestionUseCase;
        this.queryUseCase = queryUseCase;
    }

    @Override
    public void start() {
        if (!properties.rest().enabled() || running) {
            return;
        }
        try {
            server = new RestApiServerFactory().create(
                new InetSocketAddress(properties.rest().host(), properties.rest().port()),
                ingestionUseCase,
                queryUseCase
            );
            server.start();
            running = true;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to start REST API server", exception);
        }
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop();
        }
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return properties.rest().enabled();
    }

    public int port() {
        return server == null ? -1 : server.port();
    }
}
