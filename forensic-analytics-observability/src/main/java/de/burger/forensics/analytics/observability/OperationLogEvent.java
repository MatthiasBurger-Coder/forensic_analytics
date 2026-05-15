package de.burger.forensics.analytics.observability;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

record OperationLogEvent(
    String operation,
    OperationLogPhase phase,
    OperationLogLevel level,
    Optional<CorrelationId> correlationId,
    long durationMillis,
    String errorType
) {
    private static final long NO_DURATION = -1L;
    private static final Pattern UNSAFE_TOKEN_CHARACTER = Pattern.compile("[^A-Za-z0-9_.:-]");

    OperationLogEvent {
        operation = requiredToken(operation, "operation");
        phase = Objects.requireNonNull(phase, "phase must not be null");
        level = Objects.requireNonNull(level, "level must not be null");
        correlationId = Objects.requireNonNull(correlationId, "correlationId must not be null");
        if (durationMillis < NO_DURATION) {
            throw new IllegalArgumentException("durationMillis must not be less than -1");
        }
        errorType = optionalToken(errorType);
    }

    static OperationLogEvent started(String operation) {
        return new OperationLogEvent(
            operation,
            OperationLogPhase.STARTED,
            OperationLogLevel.INFO,
            CorrelationContext.current(),
            NO_DURATION,
            ""
        );
    }

    static OperationLogEvent succeeded(String operation, long durationMillis) {
        return new OperationLogEvent(
            operation,
            OperationLogPhase.SUCCEEDED,
            OperationLogLevel.INFO,
            CorrelationContext.current(),
            durationMillis,
            ""
        );
    }

    static OperationLogEvent failed(String operation, long durationMillis, Throwable error) {
        return new OperationLogEvent(
            operation,
            OperationLogPhase.FAILED,
            OperationLogLevel.ERROR,
            CorrelationContext.current(),
            durationMillis,
            Objects.requireNonNull(error, "error must not be null").getClass().getSimpleName()
        );
    }

    String message() {
        var message = new StringBuilder()
            .append("operation=").append(operation)
            .append(" phase=").append(phase.name());
        correlationId.map(CorrelationId::value)
            .ifPresent(value -> message.append(" correlationId=").append(value));
        if (durationMillis >= 0L) {
            message.append(" durationMs=").append(durationMillis);
        }
        if (!errorType.isBlank()) {
            message.append(" errorType=").append(errorType);
        }
        return message.toString();
    }

    private static String requiredToken(String value, String fieldName) {
        var token = optionalToken(value);
        if (token.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return token;
    }

    private static String optionalToken(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return UNSAFE_TOKEN_CHARACTER.matcher(value.strip()).replaceAll("_");
    }
}
