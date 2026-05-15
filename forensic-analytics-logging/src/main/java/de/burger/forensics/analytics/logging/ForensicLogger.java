package de.burger.forensics.analytics.logging;

import java.util.Objects;
import java.util.function.Supplier;

public interface ForensicLogger {
    void trace(String event);

    void debug(String event);

    void info(String event);

    void warn(String event);

    void error(String event);

    void failed(String event, Throwable error);

    void operationStarted(String operation, ForensicLogLevel level);

    void operationSucceeded(String operation, ForensicLogLevel level, long durationMillis);

    void operationFailed(String operation, long durationMillis, Throwable error);

    <T> T logged(String operation, Supplier<T> action);

    default void logged(String operation, Runnable action) {
        Objects.requireNonNull(action, "action must not be null");
        logged(operation, () -> {
            action.run();
            return null;
        });
    }

    static ForensicLogger forSource(Class<?> source) {
        return ForensicLoggerFactory.system().logger(source);
    }
}
