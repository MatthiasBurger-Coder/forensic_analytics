package de.burger.forensics.analytics.bootstrap;

import de.burger.forensics.analytics.rest.RestApiServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Objects;

public final class RestApiServerBootstrapFactory {
    public RestApiServer create(RestApiServerSettings settings) throws IOException {
        return create(settings, ForensicAnalyticsBackendComponents.createDefault());
    }

    RestApiServer create(RestApiServerSettings settings, ForensicAnalyticsBackendComponents components)
        throws IOException {
        Objects.requireNonNull(settings, "settings must not be null");
        Objects.requireNonNull(components, "components must not be null");
        return new de.burger.forensics.analytics.rest.RestApiServerFactory().create(
            new InetSocketAddress(settings.host(), settings.port()),
            components.repositoryAnalysisIngestionUseCase(),
            components.repositoryAnalysisQueryUseCase()
        );
    }
}
