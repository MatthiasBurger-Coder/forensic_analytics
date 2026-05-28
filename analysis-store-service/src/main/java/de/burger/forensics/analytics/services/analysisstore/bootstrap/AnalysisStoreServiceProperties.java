package de.burger.forensics.analytics.services.analysisstore.bootstrap;

public record AnalysisStoreServiceProperties(
    Grpc grpc,
    Health health,
    JavaAstAnalysis javaAstAnalysis,
    RepositoryAnalysis repositoryAnalysis,
    JoernCpgAnalysis joernCpgAnalysis,
    BtmGeneration btmGeneration
) {
    private static final java.util.Set<String> TRUSTED_PLAINTEXT_CLIENT_HOSTS = java.util.Set.of(
        "localhost",
        "127.0.0.1",
        "::1",
        "[::1]",
        "java-ast-analysis-service",
        "repository-analysis-service",
        "joern-cpg-analysis-service",
        "btm-generation-service"
    );
    private static final java.util.regex.Pattern PINNED_PUBLIC_IMAGE =
        java.util.regex.Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*(/[A-Za-z0-9][A-Za-z0-9._-]*)+@sha256:[A-Fa-f0-9]{64}");
    private static final java.util.regex.Pattern PUBLIC_TOKEN =
        java.util.regex.Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,127}");
    private static final java.util.regex.Pattern SENSITIVE_TOKEN_TEXT =
        java.util.regex.Pattern.compile("(?i).*(secret|token|password|passwd|credential|api[-_]?key|private[-_]?key|bearer|authorization|auth).*");
    private static final java.util.regex.Pattern CREDENTIAL_LIKE_PREFIX =
        java.util.regex.Pattern.compile("(?i)(ghp|github_pat|sk|xox[baprs]|akia|aiza|ya29|glpat)[._-].*|AKIA[A-Z0-9]{16}.*");

    public AnalysisStoreServiceProperties {
        java.util.Objects.requireNonNull(grpc, "grpc must not be null");
        java.util.Objects.requireNonNull(health, "health must not be null");
        java.util.Objects.requireNonNull(javaAstAnalysis, "java ast analysis must not be null");
        java.util.Objects.requireNonNull(repositoryAnalysis, "repository analysis must not be null");
        java.util.Objects.requireNonNull(joernCpgAnalysis, "joern cpg analysis must not be null");
        java.util.Objects.requireNonNull(btmGeneration, "btm generation must not be null");
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

    public record RepositoryAnalysis(OwnerGrpc grpc) {
        public RepositoryAnalysis {
            java.util.Objects.requireNonNull(grpc, "repository analysis gRPC must not be null");
        }
    }

    public record JoernCpgAnalysis(JoernGrpc grpc) {
        public JoernCpgAnalysis {
            java.util.Objects.requireNonNull(grpc, "joern cpg analysis gRPC must not be null");
        }
    }

    public record BtmGeneration(BtmGrpc grpc) {
        public BtmGeneration {
            java.util.Objects.requireNonNull(grpc, "btm generation gRPC must not be null");
        }
    }

    public record ClientGrpc(String host, int port, long deadlineSeconds, long maxBytes) {
        public ClientGrpc {
            requireHost(host, "client gRPC host");
            requireTrustedPlaintextClientHost(host, "client gRPC host");
            requirePort(port, "client gRPC port");
            if (deadlineSeconds < 1 || deadlineSeconds > 3_600) {
                throw new IllegalArgumentException("client gRPC deadline must be between 1 and 3600 seconds");
            }
            if (maxBytes < 1 || maxBytes > 104_857_600L) {
                throw new IllegalArgumentException("client gRPC max bytes must be between 1 and 104857600");
            }
        }
    }

    public record OwnerGrpc(String host, int port, long deadlineSeconds) {
        public OwnerGrpc {
            requireHost(host, "owner gRPC host");
            requireTrustedPlaintextClientHost(host, "owner gRPC host");
            requirePort(port, "owner gRPC port");
            if (deadlineSeconds < 1 || deadlineSeconds > 3_600) {
                throw new IllegalArgumentException("owner gRPC deadline must be between 1 and 3600 seconds");
            }
        }
    }

    public record JoernGrpc(
        String host,
        int port,
        long deadlineSeconds,
        String joernImageReference,
        String queryBundleVersion
    ) {
        public JoernGrpc {
            requireHost(host, "Joern gRPC host");
            requireTrustedPlaintextClientHost(host, "Joern gRPC host");
            requirePort(port, "Joern gRPC port");
            if (deadlineSeconds < 1 || deadlineSeconds > 3_600) {
                throw new IllegalArgumentException("Joern gRPC deadline must be between 1 and 3600 seconds");
            }
            requirePinnedPublicImageReference(joernImageReference);
            requirePublicToken(queryBundleVersion, "Joern query bundle version");
        }
    }

    public record BtmGrpc(String host, int port, long deadlineSeconds, long maxArtifactBytes) {
        public BtmGrpc {
            requireHost(host, "BTM gRPC host");
            requireTrustedPlaintextClientHost(host, "BTM gRPC host");
            requirePort(port, "BTM gRPC port");
            if (deadlineSeconds < 1 || deadlineSeconds > 3_600) {
                throw new IllegalArgumentException("BTM gRPC deadline must be between 1 and 3600 seconds");
            }
            if (maxArtifactBytes < 1 || maxArtifactBytes > 1_073_741_824L) {
                throw new IllegalArgumentException("BTM max artifact bytes must be between 1 and 1073741824");
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

    private static void requireTrustedPlaintextClientHost(String host, String name) {
        if (!TRUSTED_PLAINTEXT_CLIENT_HOSTS.contains(host.strip())) {
            throw new IllegalArgumentException(name + " must be loopback or a configured internal service DNS name");
        }
    }

    private static void requirePinnedPublicImageReference(String reference) {
        requireHost(reference, "Joern image reference");
        if (!PINNED_PUBLIC_IMAGE.matcher(reference.strip()).matches()) {
            throw new IllegalArgumentException("Joern image reference must be a public digest-pinned image coordinate");
        }
    }

    private static void requirePublicToken(String value, String name) {
        requireHost(value, name);
        var stripped = value.strip();
        if (!PUBLIC_TOKEN.matcher(stripped).matches()
            || SENSITIVE_TOKEN_TEXT.matcher(stripped).matches()
            || CREDENTIAL_LIKE_PREFIX.matcher(stripped).matches()) {
            throw new IllegalArgumentException(name + " must be a public token");
        }
    }
}
