package de.burger.forensics.analytics.services.ingestion.application;

public final class IngestionSessionNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public IngestionSessionNotFoundException(String sessionId) {
        super("ingestion session was not found: " + sessionId);
    }
}
