package de.burger.forensics.analytics.services.repositorysource.bootstrap;

@FunctionalInterface
public interface RepositorySourceStorageReadiness {
    boolean isReady();

    static RepositorySourceStorageReadiness ready() {
        return () -> true;
    }
}
