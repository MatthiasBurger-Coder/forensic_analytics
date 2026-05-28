package de.burger.forensics.analytics.services.repositorysource.application.port;

import java.util.Optional;

public interface RepositorySourceIdempotencyRepository {
    Optional<RepositorySourceIdempotencyRecord> find(String operation, String idempotencyKey);

    RepositorySourceIdempotencyRecord save(RepositorySourceIdempotencyRecord record);
}
