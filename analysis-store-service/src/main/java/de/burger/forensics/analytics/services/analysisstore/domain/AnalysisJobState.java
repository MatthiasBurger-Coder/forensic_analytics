package de.burger.forensics.analytics.services.analysisstore.domain;

public enum AnalysisJobState {
    ACCEPTED,
    DISPATCHABLE,
    RUNNING,
    RETRYABLE,
    FAILED,
    DEAD_LETTERED,
    COMPLETED
}
