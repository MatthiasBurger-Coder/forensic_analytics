package de.burger.forensics.analytics.services.analysisstore.bootstrap;

public record AnalysisStoreServiceProperties(
    Grpc grpc,
    Health health,
    JavaAstAnalysis javaAstAnalysis
) {
    public AnalysisStoreServiceProperties {
        java.util.Objects.requireNonNull(grpc, "grpc must not be null");
        java.util.Objects.requireNonNull(health, "health must not be null");
        java.util.Objects.requireNonNull(javaAstAnalysis, "java ast analysis must not be null");
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

    public record JavaAstAnalysis(ClientGrpc grpc) {
        public JavaAstAnalysis {
            java.util.Objects.requireNonNull(grpc, "java ast analysis gRPC must not be null");
        }
    }

    public record ClientGrpc(String host, int port, long deadlineSeconds, long maxBytes) {
        public ClientGrpc {
            requireHost(host, "client gRPC host");
            requirePort(port, "client gRPC port");
            if (deadlineSeconds < 1 || deadlineSeconds > 3_600) {
                throw new IllegalArgumentException("client gRPC deadline must be between 1 and 3600 seconds");
            }
            if (maxBytes < 1 || maxBytes > 104_857_600L) {
                throw new IllegalArgumentException("client gRPC max bytes must be between 1 and 104857600");
            }
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
