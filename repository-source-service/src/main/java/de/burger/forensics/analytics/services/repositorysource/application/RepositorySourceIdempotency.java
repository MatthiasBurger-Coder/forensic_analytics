package de.burger.forensics.analytics.services.repositorysource.application;

import de.burger.forensics.analytics.services.repositorysource.application.port.RepositorySourceIdempotencyRecord;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositorySourceIdempotencyRepository;

import java.time.Clock;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

final class RepositorySourceIdempotency {
    private static final String COMPLETED = "COMPLETED";
    private final RepositorySourceIdempotencyRepository repository;
    private final Clock clock;

    RepositorySourceIdempotency(RepositorySourceIdempotencyRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "idempotency repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    <T> T replayOrExecute(
        String operation,
        String idempotencyKey,
        String fingerprint,
        Function<RepositorySourceIdempotencyRecord, T> replay,
        Supplier<CompletedResult<T>> execute
    ) {
        var existing = repository.find(operation, idempotencyKey);
        if (existing.isPresent()) {
            var record = existing.get();
            if (!record.fingerprint().equals(fingerprint)) {
                throw new IdempotencyConflictException("idempotency key was reused with different input");
            }
            return replay.apply(record);
        }

        var result = execute.get();
        repository.save(new RepositorySourceIdempotencyRecord(
            idempotencyKey,
            operation,
            fingerprint,
            result.resultType(),
            result.resultReference(),
            result.resultPayload(),
            COMPLETED,
            clock.instant(),
            null
        ));
        return result.result();
    }

    record CompletedResult<T>(String resultType, String resultReference, String resultPayload, T result) {
        CompletedResult {
            resultPayload = resultPayload == null ? "" : resultPayload;
            Objects.requireNonNull(result, "idempotent result must not be null");
        }
    }
}
