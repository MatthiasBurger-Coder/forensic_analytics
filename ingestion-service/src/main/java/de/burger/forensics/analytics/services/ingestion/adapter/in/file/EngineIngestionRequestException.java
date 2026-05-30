package de.burger.forensics.analytics.services.ingestion.adapter.in.file;

public final class EngineIngestionRequestException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    EngineIngestionRequestException(String message) {
        super(message);
    }

    EngineIngestionRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
