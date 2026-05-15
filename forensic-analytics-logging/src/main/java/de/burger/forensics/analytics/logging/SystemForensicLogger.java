package de.burger.forensics.analytics.logging;

import de.burger.forensics.analytics.observability.CorrelationContext;
import de.burger.forensics.analytics.observability.CorrelationId;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Pattern;

final class SystemForensicLogger implements ForensicLogger {
    private static final Pattern UNSAFE_TOKEN_CHARACTER = Pattern.compile("[^A-Za-z0-9_.:-]");
    private final System.Logger logger;

    SystemForensicLogger(System.Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger must not be null");
    }

    @Override
    public void trace(String event) {
        event(ForensicLogLevel.TRACE, event);
    }

    @Override
    public void debug(String event) {
        event(ForensicLogLevel.DEBUG, event);
    }

    @Override
    public void info(String event) {
        event(ForensicLogLevel.INFO, event);
    }

    @Override
    public void warn(String event) {
        event(ForensicLogLevel.WARN, event);
    }

    @Override
    public void error(String event) {
        event(ForensicLogLevel.ERROR, event);
    }

    @Override
    public void failed(String event, Throwable error) {
        var scope = openCorrelationIfMissing();
        try {
            write(
                ForensicLogLevel.ERROR,
                baseMessage("event", event)
                    .append(" phase=FAILED errorType=")
                    .append(token(Objects.requireNonNull(error, "error must not be null").getClass().getSimpleName()))
                    .toString()
            );
        } finally {
            scope.close();
        }
    }

    @Override
    public void operationStarted(String operation, ForensicLogLevel level) {
        write(verifiedLevel(level), operationMessage(operation, "STARTED", -1L, ""));
    }

    @Override
    public void operationSucceeded(String operation, ForensicLogLevel level, long durationMillis) {
        write(verifiedLevel(level), operationMessage(operation, "SUCCEEDED", durationMillis, ""));
    }

    @Override
    public void operationFailed(String operation, long durationMillis, Throwable error) {
        var errorType = Objects.requireNonNull(error, "error must not be null").getClass().getSimpleName();
        write(ForensicLogLevel.ERROR, operationMessage(operation, "FAILED", durationMillis, errorType));
    }

    @Override
    public <T> T logged(String operation, Supplier<T> action) {
        Objects.requireNonNull(action, "action must not be null");
        var scope = openCorrelationIfMissing();
        var startedAt = System.nanoTime();
        operationStarted(operation, ForensicLogLevel.INFO);
        try {
            var result = action.get();
            operationSucceeded(operation, ForensicLogLevel.INFO, elapsedMillis(startedAt));
            return result;
        } catch (RuntimeException | Error error) {
            operationFailed(operation, elapsedMillis(startedAt), error);
            throw error;
        } finally {
            scope.close();
        }
    }

    private void event(ForensicLogLevel level, String event) {
        var scope = openCorrelationIfMissing();
        try {
            write(verifiedLevel(level), baseMessage("event", event).toString());
        } finally {
            scope.close();
        }
    }

    private String operationMessage(String operation, String phase, long durationMillis, String errorType) {
        var message = baseMessage("operation", operation)
            .append(" phase=")
            .append(phase);
        if (durationMillis >= 0L) {
            message.append(" durationMs=").append(durationMillis);
        }
        if (!errorType.isBlank()) {
            message.append(" errorType=").append(token(errorType));
        }
        return message.toString();
    }

    private StringBuilder baseMessage(String fieldName, String value) {
        var message = new StringBuilder()
            .append(fieldName)
            .append("=")
            .append(requiredToken(value, fieldName));
        currentCorrelationId().ifPresent(correlationId -> message
            .append(" correlationId=")
            .append(correlationId.value()));
        return message;
    }

    private void write(ForensicLogLevel level, String message) {
        try {
            var systemLevel = verifiedLevel(level).systemLevel();
            if (logger.isLoggable(systemLevel)) {
                logger.log(systemLevel, message);
            }
        } catch (RuntimeException ignored) {
            // Operational logging must never change application behavior.
        }
    }

    private static Optional<CorrelationId> currentCorrelationId() {
        return CorrelationContext.current();
    }

    private static CorrelationHandle openCorrelationIfMissing() {
        if (CorrelationContext.current().isPresent()) {
            return () -> {
            };
        }
        var scope = CorrelationContext.openGenerated();
        return scope::close;
    }

    private static ForensicLogLevel verifiedLevel(ForensicLogLevel level) {
        return Objects.requireNonNull(level, "level must not be null");
    }

    private static long elapsedMillis(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    private static String requiredToken(String value, String fieldName) {
        var token = token(value);
        if (token.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return token;
    }

    private static String token(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return UNSAFE_TOKEN_CHARACTER.matcher(value.strip()).replaceAll("_");
    }

    private interface CorrelationHandle {
        void close();
    }
}
