package de.burger.forensics.analytics.cli;

record GatewaySubmissionResult(
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
