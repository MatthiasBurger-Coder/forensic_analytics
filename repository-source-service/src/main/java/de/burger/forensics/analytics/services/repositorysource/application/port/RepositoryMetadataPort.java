package de.burger.forensics.analytics.services.repositorysource.application.port;

import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryReference;

public interface RepositoryMetadataPort {
    RepositoryMetadataResolution resolveMetadata(
        RepositoryReference repository,
        RepositoryMetadataPreviewPolicy policy
    );
}
