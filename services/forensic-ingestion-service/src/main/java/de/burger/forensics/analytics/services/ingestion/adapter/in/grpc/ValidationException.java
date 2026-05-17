package de.burger.forensics.analytics.services.ingestion.adapter.in.grpc;

public final class ValidationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }
}
