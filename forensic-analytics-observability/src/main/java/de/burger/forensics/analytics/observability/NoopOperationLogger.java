package de.burger.forensics.analytics.observability;

final class NoopOperationLogger implements OperationLogger {
    @Override
    public void started(String operation) {
    }

    @Override
    public void succeeded(String operation, long durationMillis) {
    }

    @Override
    public void failed(String operation, long durationMillis, Throwable error) {
    }
}
