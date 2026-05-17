package de.burger.forensics.analytics.services.analysisstore.application.result;

import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJob;

public record SubmitAnalysisJobResult(AnalysisJob job, OperationOutcome status) {
}
