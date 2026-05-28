package de.burger.forensics.analytics.services.analysisorchestrator.domain;

import java.util.Objects;

public record RepositoryToBtmDiagnostic(
    String code,
    String message,
    RepositoryToBtmDiagnosticSeverity severity,
    boolean retryable,
    boolean affectsCompleteness
) {
    public RepositoryToBtmDiagnostic {
        code = RequiredText.require(code, "diagnostic code");
        message = RequiredText.require(message, "diagnostic message");
        Objects.requireNonNull(severity, "diagnostic severity must not be null");
    }
}
