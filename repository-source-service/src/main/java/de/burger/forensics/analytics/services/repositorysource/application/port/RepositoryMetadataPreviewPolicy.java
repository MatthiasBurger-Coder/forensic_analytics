package de.burger.forensics.analytics.services.repositorysource.application.port;

public record RepositoryMetadataPreviewPolicy(long timeoutSeconds) {
    private static final long MAX_TIMEOUT_SECONDS = 3_600;

    public RepositoryMetadataPreviewPolicy {
        if (timeoutSeconds <= 0 || timeoutSeconds > MAX_TIMEOUT_SECONDS) {
            throw new IllegalArgumentException("metadata preview timeout must be positive and bounded");
        }
    }
}
