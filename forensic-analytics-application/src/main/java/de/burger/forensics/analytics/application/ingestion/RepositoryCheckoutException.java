package de.burger.forensics.analytics.application.ingestion;

public final class RepositoryCheckoutException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public RepositoryCheckoutException(String message) {
        super(message);
    }

    public RepositoryCheckoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
