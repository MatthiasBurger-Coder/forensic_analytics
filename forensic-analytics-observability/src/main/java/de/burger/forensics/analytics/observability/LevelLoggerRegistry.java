package de.burger.forensics.analytics.observability;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

final class LevelLoggerRegistry {
    private final Map<OperationLogLevel, LevelLogger> registry;

    LevelLoggerRegistry() {
        var loggers = new EnumMap<OperationLogLevel, LevelLogger>(OperationLogLevel.class);
        loggers.put(OperationLogLevel.INFO, new SystemLevelLogger());
        loggers.put(OperationLogLevel.WARN, new SystemLevelLogger());
        loggers.put(OperationLogLevel.ERROR, new SystemLevelLogger());
        registry = Map.copyOf(loggers);
    }

    LevelLogger get(OperationLogLevel level) {
        var logger = registry.get(Objects.requireNonNull(level, "level must not be null"));
        if (logger == null) {
            throw new IllegalArgumentException("No logger registered for level " + level);
        }
        return logger;
    }
}
