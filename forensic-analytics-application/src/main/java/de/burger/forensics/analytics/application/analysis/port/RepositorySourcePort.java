package de.burger.forensics.analytics.application.analysis.port;

import de.burger.forensics.analytics.domain.repository.RepositoryMetadata;
import de.burger.forensics.analytics.domain.repository.RepositorySource;

public interface RepositorySourcePort {
    RepositorySource resolve(RepositoryMetadata repositoryMetadata);
}
