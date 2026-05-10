package de.burger.forensics.analytics.bootstrap;

import java.util.Map;

public record GrpcIngestionServerSettings(boolean enabled, int port) {
    private static final String ENABLED_PROPERTY = "forensics.analytics.ingestion.grpc.enabled";
    private static final String PORT_PROPERTY = "forensics.analytics.ingestion.grpc.port";
    private static final String ENABLED_ENV = "FORENSICS_ANALYTICS_INGESTION_GRPC_ENABLED";
    private static final String PORT_ENV = "FORENSICS_ANALYTICS_INGESTION_GRPC_PORT";
    private static final int DEFAULT_PORT = 9090;

    public GrpcIngestionServerSettings {
        if (port <= 0 || port > 65_535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
    }

    public static GrpcIngestionServerSettings fromEnvironment() {
        return from(System.getProperties(), System.getenv());
    }

    static GrpcIngestionServerSettings from(java.util.Properties properties, Map<String, String> environment) {
        var enabledValue = properties.getProperty(ENABLED_PROPERTY, environment.getOrDefault(ENABLED_ENV, "true"));
        var portValue = properties.getProperty(PORT_PROPERTY, environment.getOrDefault(PORT_ENV, String.valueOf(DEFAULT_PORT)));
        return new GrpcIngestionServerSettings(Boolean.parseBoolean(enabledValue), Integer.parseInt(portValue));
    }
}
