package de.burger.forensics.analytics.services.joerncpganalysis.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

import static de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.requireSha256ImageReference;

@ConfigurationProperties(prefix = "forensics.joern-cpg-analysis.service")
public record JoernCpgAnalysisServiceProperties(
    Grpc grpc,
    Health health,
    Workspace workspace,
    Artifacts artifacts,
    Joern joern
) {
    public static final String DEFAULT_JOERN_RUNTIME_IMAGE_REFERENCE =
        "ghcr.io/joernio/joern@sha256:7918dc450f185433fe6cfaf43e86f5daf5643fba2139406a41a1e6e1d6134295";

    public JoernCpgAnalysisServiceProperties {
        grpc = grpc == null ? new Grpc(true, "0.0.0.0", 9094) : grpc;
        health = health == null ? new Health(true, "0.0.0.0", 8085) : health;
        workspace = workspace == null ? new Workspace(Path.of("build/joern-cpg-workspaces")) : workspace;
        artifacts = artifacts == null ? new Artifacts(Path.of("build/joern-cpg-artifacts")) : artifacts;
        joern = joern == null ? new Joern(
            "joern",
            "joern-parse",
            "8G",
            Path.of("/opt/forensic-analytics/joern-cpg-analysis/queries"),
            DEFAULT_JOERN_RUNTIME_IMAGE_REFERENCE
        ) : joern;
    }

    public record Grpc(boolean enabled, String host, int port) {
        public Grpc {
            host = text(host, "gRPC host");
            if (port < 0 || port > 65_535) {
                throw new IllegalArgumentException("gRPC port must be between 0 and 65535");
            }
        }
    }

    public record Health(boolean enabled, String host, int port) {
        public Health {
            host = text(host, "health host");
            if (port < 0 || port > 65_535) {
                throw new IllegalArgumentException("health port must be between 0 and 65535");
            }
        }
    }

    public record Workspace(Path root) {
        public Workspace {
            if (root == null) {
                throw new IllegalArgumentException("workspace root must not be null");
            }
        }
    }

    public record Artifacts(Path root) {
        public Artifacts {
            if (root == null) {
                throw new IllegalArgumentException("artifact root must not be null");
            }
        }
    }

    public record Joern(
        String executable,
        String parseExecutable,
        String heap,
        Path queryScriptsRoot,
        String runtimeImageReference
    ) {
        public Joern {
            executable = text(executable, "Joern executable");
            parseExecutable = text(parseExecutable, "Joern parse executable");
            heap = text(heap, "Joern heap");
            if (queryScriptsRoot == null) {
                throw new IllegalArgumentException("query scripts root must not be null");
            }
            runtimeImageReference = requireSha256ImageReference(runtimeImageReference, "Joern runtime image reference");
        }
    }

    private static String text(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.strip();
    }
}
