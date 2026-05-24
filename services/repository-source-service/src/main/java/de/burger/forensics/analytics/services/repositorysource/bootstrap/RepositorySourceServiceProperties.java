package de.burger.forensics.analytics.services.repositorysource.bootstrap;

import java.nio.file.Path;
import java.util.Objects;

public record RepositorySourceServiceProperties(
    Grpc grpc,
    Health health,
    Workspace workspace,
    Persistence persistence
) {
    public RepositorySourceServiceProperties {
        Objects.requireNonNull(grpc, "grpc must not be null");
        Objects.requireNonNull(health, "health must not be null");
        Objects.requireNonNull(workspace, "workspace must not be null");
        Objects.requireNonNull(persistence, "persistence must not be null");
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

    public record Workspace(Path root) {
        public Workspace {
            Objects.requireNonNull(root, "workspace root must not be null");
        }
    }

    public record Persistence(String type, H2 h2) {
        public Persistence {
            type = requirePersistenceType(type);
            Objects.requireNonNull(h2, "h2 persistence must not be null");
        }

        public boolean useH2() {
            return "h2".equals(type);
        }
    }

    public record H2(String jdbcUrl, String username, String password) {
        public H2 {
            jdbcUrl = requireSafeH2JdbcUrl(jdbcUrl);
            username = username == null ? "" : username;
            password = password == null ? "" : password;
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

    private static String requirePersistenceType(String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("persistence type must not be blank");
        }
        var normalized = type.trim().toLowerCase(java.util.Locale.ROOT);
        if (!"memory".equals(normalized) && !"h2".equals(normalized)) {
            throw new IllegalArgumentException("persistence type must be memory or h2");
        }
        return normalized;
    }

    private static String requireSafeH2JdbcUrl(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new IllegalArgumentException("H2 JDBC URL must not be blank");
        }
        var trimmed = jdbcUrl.trim();
        var lower = trimmed.toLowerCase(java.util.Locale.ROOT);
        if (!lower.startsWith("jdbc:h2:file:")) {
            throw new IllegalArgumentException("H2 JDBC URL must use file mode");
        }
        var settingsStart = trimmed.indexOf(';');
        var databasePath = settingsStart < 0 ? trimmed.substring("jdbc:h2:file:".length()) : trimmed.substring("jdbc:h2:file:".length(), settingsStart);
        if (!isServiceOwnedH2Path(databasePath)) {
            throw new IllegalArgumentException("H2 JDBC URL must stay under repository-source data storage");
        }
        if (settingsStart >= 0) {
            for (String setting : trimmed.substring(settingsStart + 1).split(";")) {
                var normalizedSetting = setting.trim().toLowerCase(java.util.Locale.ROOT);
                if (normalizedSetting.startsWith("init=")
                    || normalizedSetting.contains("runscript")
                    || normalizedSetting.startsWith("auto_server=true")
                    || normalizedSetting.startsWith("auto_server_port=")) {
                    throw new IllegalArgumentException("H2 JDBC URL contains an unsafe option");
                }
            }
        }
        return trimmed;
    }

    private static boolean isServiceOwnedH2Path(String databasePath) {
        if (databasePath == null || databasePath.isBlank() || databasePath.startsWith("~")) {
            return false;
        }
        var normalized = databasePath.replace('\\', '/');
        if (java.util.Arrays.stream(normalized.split("/")).anyMatch(".."::equals)) {
            return false;
        }
        return isPathOrChild(normalized, "build/repository-source-data")
            || isPathOrChild(normalized, "build/repository-source-test-data")
            || isPathOrChild(normalized, "/var/lib/forensic-analytics/repository-source-data");
    }

    private static boolean isPathOrChild(String path, String root) {
        return path.equals(root) || path.startsWith(root + "/");
    }
}
