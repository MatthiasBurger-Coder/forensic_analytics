package de.burger.forensics.analytics.observability;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public interface OperationLogger {
    void started(String operation);

    void succeeded(String operation, long durationMillis);

    void failed(String operation, long durationMillis, Throwable error);

    default <T> T logged(String operation, Supplier<T> action) {
        Objects.requireNonNull(action, "action must not be null");
        var startedAt = System.nanoTime();
        started(operation);
        try {
            var result = action.get();
            succeeded(operation, elapsedMillis(startedAt));
            return result;
        } catch (RuntimeException | Error error) {
            failed(operation, elapsedMillis(startedAt), error);
            throw error;
        }
    }

    static OperationLogger system(Class<?> source) {
        return SystemOperationLogger.forSource(source);
    }

    static OperationLogger noop() {
        return new NoopOperationLogger();
    }

    private static long elapsedMillis(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }
}
