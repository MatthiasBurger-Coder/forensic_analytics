package de.burger.forensics.analytics.boot.config;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

public record ForensicAnalyticsProperties(
    Workspace workspace,
    Grpc grpc,
    Rest rest,
    Joern joern,
    Observability observability
) {
    public ForensicAnalyticsProperties {
        Objects.requireNonNull(workspace, "workspace must not be null");
        Objects.requireNonNull(grpc, "grpc must not be null");
        Objects.requireNonNull(rest, "rest must not be null");
        Objects.requireNonNull(joern, "joern must not be null");
        Objects.requireNonNull(observability, "observability must not be null");
        if (!joern.outputDirectory().startsWith(workspace.rootPath())) {
            throw new IllegalArgumentException("Joern output directory must stay under the workspace root");
        }
    }

    public record Workspace(Path rootPath, Path basePath, boolean allowRelativePaths) {
        public Workspace {
            rootPath = configuredPath(rootPath, "workspace root path", allowRelativePaths);
            basePath = configuredPath(basePath, "workspace base path", allowRelativePaths);
            if (!basePath.startsWith(rootPath)) {
                throw new IllegalArgumentException("workspace base path must stay under the workspace root path");
            }
        }
    }

    public record Grpc(boolean enabled, String host, int port) {
        public Grpc {
            if (host == null || host.isBlank()) {
                throw new IllegalArgumentException("gRPC host must not be blank");
            }
            requirePort(port, "gRPC port");
        }
    }

    public record Rest(boolean enabled, String host, int port) {
        public Rest {
            if (host == null || host.isBlank()) {
                throw new IllegalArgumentException("REST host must not be blank");
            }
            requirePort(port, "REST port");
        }
    }

    public record Joern(
        boolean enabled,
        String containerImage,
        Path outputDirectory,
        Duration timeout,
        boolean failOnError
    ) {
        public Joern {
            if (containerImage == null || containerImage.isBlank()) {
                throw new IllegalArgumentException("Joern container image must not be blank");
            }
            outputDirectory = Objects.requireNonNull(outputDirectory, "outputDirectory must not be null")
                .toAbsolutePath()
                .normalize();
            Objects.requireNonNull(timeout, "timeout must not be null");
            if (timeout.isZero() || timeout.isNegative()) {
                throw new IllegalArgumentException("Joern timeout must be positive");
            }
        }
    }

    public record Observability(boolean loggingEnabled) {
    }

    private static void requirePort(int port, String name) {
        if (port < 0 || port > 65_535) {
            throw new IllegalArgumentException(name + " must be between 0 and 65535");
        }
    }

    private static Path configuredPath(Path path, String name, boolean allowRelativePaths) {
        var configured = Objects.requireNonNull(path, name + " must not be null");
        if (!allowRelativePaths && !configured.isAbsolute()) {
            throw new IllegalArgumentException(name + " must be absolute");
        }
        var normalized = configured.toAbsolutePath().normalize();
        if (normalized.getParent() == null) {
            throw new IllegalArgumentException(name + " must not be a file-system root");
        }
        if (homePath().map(normalized::equals).orElse(false)) {
            throw new IllegalArgumentException(name + " must not be the user home directory");
        }
        return normalized;
    }

    private static java.util.Optional<Path> homePath() {
        var home = System.getProperty("user.home");
        if (home == null || home.isBlank()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(Path.of(home).toAbsolutePath().normalize());
    }
}
