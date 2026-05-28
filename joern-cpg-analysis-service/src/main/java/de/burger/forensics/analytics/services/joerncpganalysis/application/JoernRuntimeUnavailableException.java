package de.burger.forensics.analytics.services.joerncpganalysis.application;

public final class JoernRuntimeUnavailableException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public JoernRuntimeUnavailableException(String message) {
        super(message);
    }

    public JoernRuntimeUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
