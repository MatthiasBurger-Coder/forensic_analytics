package de.burger.forensics.analytics.services.queryreportapi.domain;

import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.Diagnostic;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public final class QueryReportApiSettings {
    private static final Pattern HOST = Pattern.compile("[A-Za-z0-9.-]{1,253}");
    private static final Pattern SQL_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private QueryReportApiSettings() {
    }

    public record DatabaseSettingsRequest(
        String requestId,
        String correlationId
    ) {
        public DatabaseSettingsRequest {
            requestId = requireText(requestId, "request id");
            correlationId = requireText(correlationId, "correlation id");
        }
    }

    public record DatabaseSettingsValidationRequest(
        String requestId,
        String correlationId,
        String host,
        int port,
        String databaseName,
        String username,
        String password,
        String schema,
        String sslMode
    ) {
        public DatabaseSettingsValidationRequest {
            requestId = requireText(requestId, "request id");
            correlationId = requireText(correlationId, "correlation id");
            host = requireHost(host);
            if (port < 1 || port > 65_535) {
                throw new IllegalArgumentException("database port must be between 1 and 65535");
            }
            databaseName = requireSqlIdentifier(databaseName, "database name");
            username = requireText(username, "database username");
            password = password == null ? "" : password;
            schema = requireSqlIdentifier(schema, "database schema");
            sslMode = normalizeSslMode(sslMode);
        }
    }

    public record DatabaseSettingsStatus(
        DatabaseSettingsView settings,
        String status,
        List<Diagnostic> diagnostics
    ) {
        public DatabaseSettingsStatus {
            Objects.requireNonNull(settings, "database settings view must not be null");
            status = normalizeStatus(status);
            diagnostics = List.copyOf(Objects.requireNonNullElse(diagnostics, List.of()));
        }
    }

    public record DatabaseSettingsValidationResponse(
        DatabaseSettingsView settings,
        String validationStatus,
        String applyMode,
        boolean hotApplySupported,
        List<Diagnostic> diagnostics
    ) {
        public DatabaseSettingsValidationResponse {
            Objects.requireNonNull(settings, "database settings view must not be null");
            validationStatus = normalizeValidationStatus(validationStatus);
            applyMode = restartRequired(applyMode);
            if (hotApplySupported) {
                throw new IllegalArgumentException("database settings hot apply is not supported");
            }
            diagnostics = List.copyOf(Objects.requireNonNullElse(diagnostics, List.of()));
        }
    }

    public record DatabaseSettingsView(
        String engine,
        String host,
        int port,
        String databaseName,
        String username,
        boolean authenticationConfigured,
        String schema,
        String sslMode,
        String configurationSource,
        String applyMode,
        boolean hotApplySupported
    ) {
        public DatabaseSettingsView {
            engine = requireEnum(engine, "POSTGRESQL", "database engine");
            host = requireHost(host);
            if (port < 1 || port > 65_535) {
                throw new IllegalArgumentException("database port must be between 1 and 65535");
            }
            databaseName = requireSqlIdentifier(databaseName, "database name");
            username = requireText(username, "database username");
            schema = requireSqlIdentifier(schema, "database schema");
            sslMode = normalizeSslMode(sslMode);
            configurationSource = requireKnown(configurationSource, "configuration source", "REPOSITORY_SOURCE_RUNTIME", "VALIDATION_REQUEST");
            applyMode = restartRequired(applyMode);
            if (hotApplySupported) {
                throw new IllegalArgumentException("database settings hot apply is not supported");
            }
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }

    private static String requireHost(String host) {
        var text = requireText(host, "database host");
        if (!HOST.matcher(text).matches() || text.startsWith(".") || text.endsWith(".")) {
            throw new IllegalArgumentException("database host must be a DNS name or address label");
        }
        return text;
    }

    private static String requireSqlIdentifier(String value, String name) {
        var text = requireText(value, name);
        if (!SQL_IDENTIFIER.matcher(text).matches()) {
            throw new IllegalArgumentException(name + " must be a simple SQL identifier");
        }
        return text;
    }

    private static String normalizeSslMode(String value) {
        if (value == null || value.isBlank() || "UNSPECIFIED".equalsIgnoreCase(value)) {
            return "UNSPECIFIED";
        }
        var normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
        return switch (normalized) {
            case "disable", "allow", "prefer", "require", "verify-ca", "verify-full" -> normalized;
            default -> throw new IllegalArgumentException("database ssl mode is not supported");
        };
    }

    private static String normalizeStatus(String value) {
        return requireKnown(value, "database settings status", "AVAILABLE", "UNAVAILABLE");
    }

    private static String normalizeValidationStatus(String value) {
        return requireKnown(value, "database settings validation status", "VALID", "INVALID", "UNREACHABLE", "AUTHENTICATION_FAILED", "UNSUPPORTED");
    }

    private static String restartRequired(String value) {
        return requireEnum(value, "RESTART_REQUIRED", "database settings apply mode");
    }

    private static String requireEnum(String value, String expected, String name) {
        var text = requireText(value, name);
        if (!expected.equals(text)) {
            throw new IllegalArgumentException(name + " is not supported");
        }
        return text;
    }

    private static String requireKnown(String value, String name, String... accepted) {
        var text = requireText(value, name);
        for (var candidate : accepted) {
            if (candidate.equals(text)) {
                return text;
            }
        }
        throw new IllegalArgumentException(name + " is not supported");
    }
}
