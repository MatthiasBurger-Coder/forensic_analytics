package de.burger.forensics.analytics.services.gateway.application;

public final class GatewayIdempotencyConflictException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public GatewayIdempotencyConflictException(String message) {
        super(message);
    }
}
