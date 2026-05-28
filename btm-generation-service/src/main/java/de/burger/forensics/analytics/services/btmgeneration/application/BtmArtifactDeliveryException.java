package de.burger.forensics.analytics.services.btmgeneration.application;

public final class BtmArtifactDeliveryException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final Reason reason;

    public BtmArtifactDeliveryException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public BtmArtifactDeliveryException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }

    public enum Reason {
        INVALID_REQUEST,
        NOT_FOUND,
        FAILED_PRECONDITION,
        RESOURCE_EXHAUSTED
    }
}
