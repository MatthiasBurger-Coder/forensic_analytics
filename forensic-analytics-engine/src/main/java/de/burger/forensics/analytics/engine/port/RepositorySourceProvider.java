package de.burger.forensics.analytics.engine.port;

import de.burger.forensics.analytics.domain.repository.RepositoryMetadata;
import de.burger.forensics.analytics.domain.repository.RepositorySource;

public interface RepositorySourceProvider {
    RepositorySource resolve(RepositoryMetadata repositoryMetadata);
}
