package de.burger.forensics.analytics.observability;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Optional;

public final class CorrelationContext {
    private static final ThreadLocal<Deque<CorrelationId>> CURRENT = ThreadLocal.withInitial(ArrayDeque::new);

    private CorrelationContext() {
    }

    public static Optional<CorrelationId> current() {
        var stack = CURRENT.get();
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(stack.peek());
    }

    public static CorrelationScope openGenerated() {
        return open(CorrelationId.generate());
    }

    public static CorrelationScope open(String correlationId) {
        return open(CorrelationId.fromExternal(correlationId));
    }

    public static CorrelationScope open(CorrelationId correlationId) {
        var verifiedCorrelationId = Objects.requireNonNull(correlationId, "correlationId must not be null");
        CURRENT.get().push(verifiedCorrelationId);
        return new CorrelationScope(verifiedCorrelationId);
    }

    static void close(CorrelationId correlationId) {
        var stack = CURRENT.get();
        if (stack.isEmpty()) {
            CURRENT.remove();
            return;
        }
        if (correlationId.equals(stack.peek())) {
            stack.pop();
        } else {
            stack.remove(correlationId);
        }
        if (stack.isEmpty()) {
            CURRENT.remove();
        }
    }

    static void clear() {
        CURRENT.remove();
    }
}
