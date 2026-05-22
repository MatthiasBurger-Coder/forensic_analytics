package de.burger.forensics.analytics.services.analysisorchestrator.application.result;

import de.burger.forensics.analytics.services.analysisorchestrator.domain.AnalysisJob;

public record SubmitAnalysisJobResult(AnalysisJob job, OperationOutcome status) {
}
