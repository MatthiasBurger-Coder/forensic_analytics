package de.burger.forensics.analytics.services.analysisstore.application.result;

import java.util.List;

public record OperationOutcome(
    String code,
    String message,
    boolean retryable,
    String correlationId,
    List<String> diagnostics
) {
    public OperationOutcome {
        diagnostics = List.copyOf(diagnostics);
    }

    public static OperationOutcome accepted(String correlationId, String message) {
        return new OperationOutcome("ACCEPTED", message, false, correlationId, List.of());
    }

    public static OperationOutcome completed(String correlationId, String message) {
        return new OperationOutcome("COMPLETED", message, false, correlationId, List.of());
    }
}
