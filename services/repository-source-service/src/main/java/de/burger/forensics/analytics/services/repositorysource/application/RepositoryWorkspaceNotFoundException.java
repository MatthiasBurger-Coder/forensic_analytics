package de.burger.forensics.analytics.services.repositorysource.application;

public final class RepositoryWorkspaceNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public RepositoryWorkspaceNotFoundException(String message) {
        super(message);
    }
}
