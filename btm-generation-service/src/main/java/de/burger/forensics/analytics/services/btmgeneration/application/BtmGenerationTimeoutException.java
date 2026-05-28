package de.burger.forensics.analytics.services.btmgeneration.application;

public class BtmGenerationTimeoutException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public BtmGenerationTimeoutException(String message) {
        super(message);
    }
}
