package de.burger.forensics.analytics.services.repositoryanalysis.application;

public final class RepositoryPreparationNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public RepositoryPreparationNotFoundException(String message) {
        super(message);
    }
}
