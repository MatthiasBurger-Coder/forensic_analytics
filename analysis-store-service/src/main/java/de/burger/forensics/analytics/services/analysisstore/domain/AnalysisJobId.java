package de.burger.forensics.analytics.services.analysisstore.domain;

public record AnalysisJobId(String value) {
    public AnalysisJobId {
        value = RequiredText.require(value, "jobId");
    }
}
