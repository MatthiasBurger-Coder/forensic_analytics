package de.burger.forensics.analytics.services.analysisstore.domain;

public record AnalysisRunId(String value) {
    public AnalysisRunId {
        value = RequiredText.require(value, "analysisRunId");
    }
}
