package de.burger.forensics.analytics.services.cliclient.adapter.in.cli;

import de.burger.forensics.analytics.services.cliclient.domain.CliClientSubmissionResult;

final class CliClientOutput {
    String format(CliClientSubmissionResult result) {
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
