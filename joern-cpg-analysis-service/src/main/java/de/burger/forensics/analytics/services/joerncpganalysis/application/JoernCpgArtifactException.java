package de.burger.forensics.analytics.services.joerncpganalysis.application;

public final class JoernCpgArtifactException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public JoernCpgArtifactException(String message) {
        super(message);
    }

    public JoernCpgArtifactException(String message, Throwable cause) {
        super(message, cause);
    }
}
