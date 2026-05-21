package de.burger.forensics.analytics.cli;

final class GatewaySubmissionOutput {
    String format(GatewaySubmissionResult result) {
        return """
            analysisRunId=%s
            status=%s
            statusUrl=%s
            jobsUrl=%s
            btmDeliveryStatus=%s
            btmDeliveryService=%s
            correlationId=%s
            diagnostics=%d
            """.formatted(
            result.analysisRunId(),
            result.status(),
            result.statusUrl(),
            result.jobsUrl(),
            result.btmDeliveryStatus(),
            result.btmDeliveryService(),
            result.correlationId(),
            result.diagnosticCount()
        );
    }
}
