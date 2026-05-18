package de.burger.forensics.analytics.services.gateway.bootstrap;

public record ForensicGatewayServiceProperties(
    Http http
) {
    public ForensicGatewayServiceProperties {
        if (http == null) {
            throw new NullPointerException("http must not be null");
        }
    }

    public record Http(boolean enabled, String host, int port) {
        public Http {
            requireHost(host);
            requirePort(port);
        }
    }

    private static void requireHost(String host) {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("HTTP host must not be blank");
        }
    }

    private static void requirePort(int port) {
        if (port < 0 || port > 65_535) {
            throw new IllegalArgumentException("HTTP port must be between 0 and 65535");
        }
    }
}
