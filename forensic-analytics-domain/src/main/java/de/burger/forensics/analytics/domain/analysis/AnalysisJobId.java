package de.burger.forensics.analytics.domain.analysis;

public record AnalysisJobId(String value) {
    public AnalysisJobId {
        RequiredAnalysisText.requireText(value, "analysis job id");
    }
}
