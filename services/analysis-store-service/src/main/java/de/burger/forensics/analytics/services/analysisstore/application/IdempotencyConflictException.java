package de.burger.forensics.analytics.services.analysisstore.application;

public final class IdempotencyConflictException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public IdempotencyConflictException(String key) {
        super("idempotency key conflicts with a different request: " + key);
    }
}
