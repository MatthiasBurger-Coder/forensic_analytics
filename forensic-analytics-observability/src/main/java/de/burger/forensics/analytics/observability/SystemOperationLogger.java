package de.burger.forensics.analytics.observability;

import java.util.Objects;

public final class SystemOperationLogger implements OperationLogger {
    private final System.Logger logger;
    private final LevelLoggerRegistry registry;

    public SystemOperationLogger(System.Logger logger) {
        this(logger, new LevelLoggerRegistry());
    }

    SystemOperationLogger(System.Logger logger, LevelLoggerRegistry registry) {
        this.logger = Objects.requireNonNull(logger, "logger must not be null");
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
    }

    static SystemOperationLogger forSource(Class<?> source) {
        return new SystemOperationLogger(System.getLogger(Objects.requireNonNull(source, "source must not be null").getName()));
    }

    @Override
    public void started(String operation) {
        log(OperationLogEvent.started(operation));
    }

    @Override
    public void succeeded(String operation, long durationMillis) {
        log(OperationLogEvent.succeeded(operation, durationMillis));
    }

    @Override
    public void failed(String operation, long durationMillis, Throwable error) {
        log(OperationLogEvent.failed(operation, durationMillis, error));
    }

    private void log(OperationLogEvent event) {
        try {
            registry.get(event.level()).log(logger, event);
        } catch (RuntimeException ignored) {
            // Operational logging must not alter request, command or server behavior.
        }
    }
}
