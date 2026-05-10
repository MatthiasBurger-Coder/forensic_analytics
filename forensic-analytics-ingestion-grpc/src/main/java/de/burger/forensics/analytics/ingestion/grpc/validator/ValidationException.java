package de.burger.forensics.analytics.ingestion.grpc.validator;

public final class ValidationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }
}
