package de.burger.forensics.analytics.ingestion.grpc.validator;

public final class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
