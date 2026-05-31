package de.burger.forensics.analytics.services.repositorysource.bootstrap;

import java.util.Objects;

public record RepositorySourceDatabaseSettingsValidationResult(
    Status status,
    String code,
    String message,
    boolean retryable
) {
    public RepositorySourceDatabaseSettingsValidationResult {
        Objects.requireNonNull(status, "validation status must not be null");
        code = requireText(code, "validation code");
        message = requireText(message, "validation message");
    }

    public static RepositorySourceDatabaseSettingsValidationResult valid() {
        return new RepositorySourceDatabaseSettingsValidationResult(
            Status.VALID,
            "DATABASE_SETTINGS_VALID",
            "PostgreSQL settings are reachable",
            false
        );
    }

    public static RepositorySourceDatabaseSettingsValidationResult unreachable() {
        return new RepositorySourceDatabaseSettingsValidationResult(
            Status.UNREACHABLE,
            "DATABASE_SETTINGS_UNREACHABLE",
            "PostgreSQL is not reachable",
            true
        );
    }

    public static RepositorySourceDatabaseSettingsValidationResult authenticationFailed() {
        return new RepositorySourceDatabaseSettingsValidationResult(
            Status.AUTHENTICATION_FAILED,
            "DATABASE_SETTINGS_AUTHENTICATION_FAILED",
            "PostgreSQL authentication failed",
            false
        );
    }

    public enum Status {
        VALID,
        UNREACHABLE,
        AUTHENTICATION_FAILED
    }

    private static String requireText(String value, String name) {
        var text = Objects.requireNonNull(value, name + " must not be null").trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return text;
    }
}
