package de.burger.forensics.analytics.observability;

public interface OperationLogger {
    void started(String operation);

    void succeeded(String operation, long durationMillis);

    void failed(String operation, long durationMillis, Throwable error);

    static OperationLogger system(Class<?> source) {
        return SystemOperationLogger.forSource(source);
    }

    static OperationLogger noop() {
        return new NoopOperationLogger();
    }
}
