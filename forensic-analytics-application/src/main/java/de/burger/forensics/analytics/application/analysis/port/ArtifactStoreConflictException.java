package de.burger.forensics.analytics.application.analysis.port;

public final class ArtifactStoreConflictException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ArtifactStoreConflictException(String message) {
        super(message);
    }
}
