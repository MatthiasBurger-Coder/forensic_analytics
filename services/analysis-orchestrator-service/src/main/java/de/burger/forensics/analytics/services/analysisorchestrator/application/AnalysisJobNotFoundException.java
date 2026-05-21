package de.burger.forensics.analytics.services.analysisorchestrator.application;

public final class AnalysisJobNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public AnalysisJobNotFoundException(String jobId) {
        super("analysis job not found: " + jobId);
    }
}
