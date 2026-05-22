package de.burger.forensics.analytics.services.cliclient.domain;

public final class CliClientValidationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public CliClientValidationException(String message) {
        super(message);
    }
}
