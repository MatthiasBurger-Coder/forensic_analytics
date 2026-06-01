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

    public record Persistence(String type, Postgres postgres) {
        public Persistence {
            type = requirePersistenceType(type);
            Objects.requireNonNull(postgres, "postgres persistence must not be null");
        }

        public boolean usePostgres() {
            return "postgres".equals(type);
        }
    }

    public record Postgres(String jdbcUrl, String username, String password, String schema, String changeLog) {
        public Postgres {
            jdbcUrl = requirePostgresJdbcUrl(jdbcUrl);
            username = requireText(username, "PostgreSQL username");
            password = password == null ? "" : password;
            schema = requireSqlIdentifier(schema, "PostgreSQL schema");
            changeLog = requireRepositorySourceChangeLog(changeLog);
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
        if (!"memory".equals(normalized) && !"postgres".equals(normalized)) {
            throw new IllegalArgumentException("persistence type must be memory or postgres");
        }
        return normalized;
    }

    private static String requireText(String text, String name) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return text.trim();
    }

    private static String requirePostgresJdbcUrl(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new IllegalArgumentException("PostgreSQL JDBC URL must not be blank");
        }
        var trimmed = jdbcUrl.trim();
        var lower = trimmed.toLowerCase(java.util.Locale.ROOT);
        if (!lower.startsWith("jdbc:postgresql://")) {
            throw new IllegalArgumentException("PostgreSQL JDBC URL must use the PostgreSQL driver");
        }
        if (lower.contains("password=") || lower.contains("user=")) {
            throw new IllegalArgumentException("PostgreSQL JDBC URL must not contain credentials");
        }
        return trimmed;
    }

    private static String requireSqlIdentifier(String value, String name) {
        var trimmed = requireText(value, name);
        if (!trimmed.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException(name + " must be a simple SQL identifier");
        }
        return trimmed;
    }

    private static String requireRepositorySourceChangeLog(String changeLog) {
        var trimmed = requireText(changeLog, "PostgreSQL Liquibase changelog");
        if (!trimmed.startsWith("classpath:db/changelog/")) {
            throw new IllegalArgumentException("PostgreSQL Liquibase changelog must be a repository-source classpath changelog");
        }
        return trimmed;
    }

}
