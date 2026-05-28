package de.burger.forensics.analytics.services.ingestion.bootstrap;

public record ForensicIngestionServiceProperties(
    Grpc grpc,
    Health health
) {
    public ForensicIngestionServiceProperties {
        java.util.Objects.requireNonNull(grpc, "grpc must not be null");
        java.util.Objects.requireNonNull(health, "health must not be null");
    }

    public record Grpc(boolean enabled, String host, int port) {
        public Grpc {
            requireHost(host, "gRPC host");
            requirePort(port, "gRPC port");
        }
    }

    public record Health(boolean enabled, String host, int port) {
        public Health {
            requireHost(host, "health host");
            requirePort(port, "health port");
        }
    }

    private static void requireHost(String host, String name) {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    private static void requirePort(int port, String name) {
        if (port < 0 || port > 65_535) {
            throw new IllegalArgumentException(name + " must be between 0 and 65535");
        }
    }
}
