package de.burger.forensics.analytics.services.analysisorchestrator.application.result;

import de.burger.forensics.analytics.services.analysisorchestrator.domain.AnalysisJob;

import java.util.List;

public record LeaseAnalysisJobResult(List<AnalysisJob> jobs, OperationOutcome status) {
    public LeaseAnalysisJobResult {
        jobs = List.copyOf(jobs);
    }
}
