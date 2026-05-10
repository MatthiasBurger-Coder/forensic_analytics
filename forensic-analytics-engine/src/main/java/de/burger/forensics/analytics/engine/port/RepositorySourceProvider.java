package de.burger.forensics.analytics.engine.port;

import de.burger.forensics.analytics.engine.RepositorySource;

public interface RepositorySourceProvider {
    RepositorySource resolve(String repositoryLocation);
}
