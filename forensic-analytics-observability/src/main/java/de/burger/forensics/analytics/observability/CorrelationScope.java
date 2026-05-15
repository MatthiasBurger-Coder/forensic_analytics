package de.burger.forensics.analytics.observability;

public final class CorrelationScope implements AutoCloseable {
    private final CorrelationId correlationId;
    private boolean closed;

    CorrelationScope(CorrelationId correlationId) {
        this.correlationId = correlationId;
    }

    public CorrelationId correlationId() {
        return correlationId;
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            CorrelationContext.close(correlationId);
        }
    }
}
