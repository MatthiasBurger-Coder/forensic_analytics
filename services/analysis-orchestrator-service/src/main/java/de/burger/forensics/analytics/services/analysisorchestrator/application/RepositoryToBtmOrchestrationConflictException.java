package de.burger.forensics.analytics.services.analysisorchestrator.application;

public final class RepositoryToBtmOrchestrationConflictException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public RepositoryToBtmOrchestrationConflictException(String analysisRunId) {
        super("repository-to-BTM orchestration already exists for analysisRunId: " + analysisRunId);
    }
}
