package de.burger.forensics.analytics.services.repositorysource.application;

public final class IdempotencyConflictException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public IdempotencyConflictException(String message) {
        super(message);
    }
}
