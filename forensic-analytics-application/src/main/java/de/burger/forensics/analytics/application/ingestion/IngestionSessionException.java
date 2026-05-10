package de.burger.forensics.analytics.application.ingestion;

public final class IngestionSessionException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private IngestionSessionException(String message) {
        super(message);
    }

    public static IngestionSessionException missing(String sessionId) {
        return new IngestionSessionException("Analysis session does not exist: " + sessionId);
    }

    public static IngestionSessionException closed(String sessionId) {
        return new IngestionSessionException("Analysis session is not active: " + sessionId);
    }
}
