package de.burger.forensics.analytics.services.joerncpganalysis.application;

public final class JoernCpgAnalysisTimeoutException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public JoernCpgAnalysisTimeoutException(String message) {
        super(message);
    }
}
