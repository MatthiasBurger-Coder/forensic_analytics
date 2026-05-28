package de.burger.forensics.analytics.services.analysisstore.application;

public final class AnalysisJobNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public AnalysisJobNotFoundException(String jobId) {
        super("analysis job not found: " + jobId);
    }
}
