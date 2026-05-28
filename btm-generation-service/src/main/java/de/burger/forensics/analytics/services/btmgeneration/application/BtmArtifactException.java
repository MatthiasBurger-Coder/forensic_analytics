package de.burger.forensics.analytics.services.btmgeneration.application;

public class BtmArtifactException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public BtmArtifactException(String message) {
        super(message);
    }

    public BtmArtifactException(String message, Throwable cause) {
        super(message, cause);
    }
}
