package de.burger.forensics.analytics.services.queryreportapi.bootstrap;

public record QueryReportApiServiceProperties(
    Http http,
    AnalysisOrchestrator analysisOrchestrator,
    RepositorySource repositorySource,
    WorkspaceFacade workspaceFacade
) {
    public QueryReportApiServiceProperties {
        if (http == null) {
            throw new NullPointerException("http must not be null");
        }
        if (analysisOrchestrator == null) {
            throw new NullPointerException("analysis orchestrator must not be null");
        }
        if (repositorySource == null) {
            throw new NullPointerException("repository source must not be null");
        }
        if (workspaceFacade == null) {
            throw new NullPointerException("workspace facade must not be null");
        }
    }

    public record Http(boolean enabled, String host, int port) {
        public Http {
            requireHost(host);
            requirePort(port);
        }
    }

    public record AnalysisOrchestrator(Grpc grpc) {
        public AnalysisOrchestrator {
            if (grpc == null) {
                throw new NullPointerException("analysis orchestrator gRPC must not be null");
            }
        }
    }

    public record RepositorySource(Grpc grpc) {
        public RepositorySource {
            if (grpc == null) {
                throw new NullPointerException("repository source gRPC must not be null");
            }
        }
    }

    public record WorkspaceFacade(
        String schemaVersion,
        long metadataTimeoutSeconds,
        boolean refreshEphemeral,
        boolean refreshAllowShallowClone,
        boolean refreshAllowPartialClone,
        boolean refreshAllowSparseCheckout,
        long refreshTimeoutSeconds,
        long refreshMaxWorkspaceBytes
    ) {
        public WorkspaceFacade {
            requireText(schemaVersion, "workspace schema version");
            requirePositiveBounded(metadataTimeoutSeconds, "metadata timeout seconds");
            requirePositiveBounded(refreshTimeoutSeconds, "refresh timeout seconds");
            if (refreshMaxWorkspaceBytes < 1 || refreshMaxWorkspaceBytes > 107_374_182_400L) {
                throw new IllegalArgumentException("refresh max workspace bytes must be between 1 and 107374182400");
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

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    private static void requirePort(int port) {
        if (port < 0 || port > 65_535) {
            throw new IllegalArgumentException("HTTP port must be between 0 and 65535");
        }
    }

    private static void requirePositiveBounded(long value, String name) {
        if (value < 1 || value > 3_600) {
            throw new IllegalArgumentException(name + " must be between 1 and 3600");
        }
    }
}
