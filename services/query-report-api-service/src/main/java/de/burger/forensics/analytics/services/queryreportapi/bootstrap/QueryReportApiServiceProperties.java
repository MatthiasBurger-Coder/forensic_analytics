package de.burger.forensics.analytics.services.queryreportapi.bootstrap;

public record QueryReportApiServiceProperties(
    Http http,
    AnalysisStore analysisStore
) {
    public QueryReportApiServiceProperties {
        if (http == null) {
            throw new NullPointerException("http must not be null");
        }
        if (analysisStore == null) {
            throw new NullPointerException("analysis store must not be null");
        }
    }

    public record Http(boolean enabled, String host, int port) {
        public Http {
            requireHost(host);
            requirePort(port);
        }
    }

    public record AnalysisStore(Grpc grpc) {
        public AnalysisStore {
            if (grpc == null) {
                throw new NullPointerException("analysis store gRPC must not be null");
            }
        }
    }

    public record Grpc(String host, int port, int deadlineSeconds) {
        public Grpc {
            requireHost(host);
            requirePort(port);
            if (deadlineSeconds < 1 || deadlineSeconds > 300) {
                throw new IllegalArgumentException("gRPC deadline seconds must be between 1 and 300");
            }
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
