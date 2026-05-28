package de.burger.forensics.analytics.services.cliclient.domain;

public record CliClientSubmissionResult(
    String analysisRunId,
    String status,
    String statusUrl,
    String jobsUrl,
    String btmDeliveryStatus,
    String btmDeliveryService,
    String correlationId,
    int diagnosticCount
) {
}
