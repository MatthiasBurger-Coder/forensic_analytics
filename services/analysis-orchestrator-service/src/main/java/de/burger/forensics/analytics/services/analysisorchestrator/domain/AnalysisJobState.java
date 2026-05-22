package de.burger.forensics.analytics.services.analysisorchestrator.domain;

public enum AnalysisJobState {
    ACCEPTED,
    DISPATCHABLE,
    RUNNING,
    RETRYABLE,
    FAILED,
    DEAD_LETTERED,
    COMPLETED
}
