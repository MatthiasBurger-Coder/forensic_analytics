package de.burger.forensics.analytics.bootstrap;

import java.util.Map;

public record RestApiServerSettings(boolean enabled, String host, int port) {
    private static final String ENABLED_PROPERTY = "forensics.analytics.rest.enabled";
    private static final String HOST_PROPERTY = "forensics.analytics.rest.host";
    private static final String PORT_PROPERTY = "forensics.analytics.rest.port";
    private static final String ENABLED_ENV = "FORENSICS_ANALYTICS_REST_ENABLED";
    private static final String HOST_ENV = "FORENSICS_ANALYTICS_REST_HOST";
    private static final String PORT_ENV = "FORENSICS_ANALYTICS_REST_PORT";
    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 8080;

    public RestApiServerSettings {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }
        if (port <= 0 || port > 65_535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
    }

    public static RestApiServerSettings fromEnvironment() {
        return from(System.getProperties(), System.getenv());
    }

    static RestApiServerSettings from(java.util.Properties properties, Map<String, String> environment) {
        var enabledValue = properties.getProperty(ENABLED_PROPERTY, environment.getOrDefault(ENABLED_ENV, "true"));
        var hostValue = properties.getProperty(HOST_PROPERTY, environment.getOrDefault(HOST_ENV, DEFAULT_HOST));
        var portValue = properties.getProperty(PORT_PROPERTY, environment.getOrDefault(PORT_ENV, String.valueOf(DEFAULT_PORT)));
        return new RestApiServerSettings(Boolean.parseBoolean(enabledValue), hostValue, Integer.parseInt(portValue));
    }
}
