package de.burger.forensics.analytics.services.analysisorchestrator.domain;

public record AnalysisJobId(String value) {
    public AnalysisJobId {
        value = RequiredText.require(value, "jobId");
    }
}
