package de.burger.forensics.analytics.services.javaastanalysis.application;

public final class JavaAstAnalysisTimeoutException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public JavaAstAnalysisTimeoutException(String message) {
        super(message);
    }
}
