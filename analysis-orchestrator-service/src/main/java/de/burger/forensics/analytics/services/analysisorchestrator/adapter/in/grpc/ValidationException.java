package de.burger.forensics.analytics.services.analysisorchestrator.adapter.in.grpc;

final class ValidationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    ValidationException(String message) {
        super(message);
    }
}
