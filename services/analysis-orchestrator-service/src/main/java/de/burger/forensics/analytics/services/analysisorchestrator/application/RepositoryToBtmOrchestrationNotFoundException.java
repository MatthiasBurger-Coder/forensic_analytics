package de.burger.forensics.analytics.services.analysisorchestrator.application;

public final class RepositoryToBtmOrchestrationNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public RepositoryToBtmOrchestrationNotFoundException(String analysisRunId) {
        super("repository-to-BTM orchestration not found: " + analysisRunId);
    }
}
