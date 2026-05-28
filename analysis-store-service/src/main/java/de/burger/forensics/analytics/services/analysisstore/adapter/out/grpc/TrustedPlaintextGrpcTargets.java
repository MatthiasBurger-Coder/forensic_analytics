package de.burger.forensics.analytics.services.analysisstore.adapter.out.grpc;

import java.util.Set;

final class TrustedPlaintextGrpcTargets {
    private static final Set<String> TRUSTED_HOSTS = Set.of(
        "localhost",
        "127.0.0.1",
        "::1",
        "[::1]",
        "java-ast-analysis-service",
        "repository-analysis-service",
        "joern-cpg-analysis-service",
        "btm-generation-service"
    );

    private TrustedPlaintextGrpcTargets() {
    }

    static String requireTrustedHost(String host, String name) {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        var stripped = host.strip();
        if (!TRUSTED_HOSTS.contains(stripped)) {
            throw new IllegalArgumentException(name + " must be loopback or a configured internal service DNS name");
        }
        return stripped;
    }
}
