package de.burger.forensics.analytics.services.analysisstore.application.result;

import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJob;

import java.util.List;

public record LeaseAnalysisJobResult(List<AnalysisJob> jobs, OperationOutcome status) {
    public LeaseAnalysisJobResult {
        jobs = List.copyOf(jobs);
    }
}
