package de.burger.forensics.analytics.services.repositorysource.adapter.out.memory;

import de.burger.forensics.analytics.services.repositorysource.application.port.RepositorySourceIdempotencyRecord;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositorySourceIdempotencyRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryRepositorySourceIdempotencyRepository implements RepositorySourceIdempotencyRepository {
    private final Map<String, RepositorySourceIdempotencyRecord> records = new HashMap<>();

    @Override
    public synchronized Optional<RepositorySourceIdempotencyRecord> find(String operation, String idempotencyKey) {
        return Optional.ofNullable(records.get(key(operation, idempotencyKey)));
    }

    @Override
    public synchronized RepositorySourceIdempotencyRecord save(RepositorySourceIdempotencyRecord record) {
        records.put(key(record.operation(), record.idempotencyKey()), record);
        return record;
    }

    private static String key(String operation, String idempotencyKey) {
        return operation + "|" + idempotencyKey;
    }
}
