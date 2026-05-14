package de.burger.forensics.analytics.application.ingestion;

public final class RepositoryAnalysisIngestionException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public RepositoryAnalysisIngestionException(String message) {
        super(message);
    }

    public RepositoryAnalysisIngestionException(String message, Throwable cause) {
        super(message, cause);
    }
}
