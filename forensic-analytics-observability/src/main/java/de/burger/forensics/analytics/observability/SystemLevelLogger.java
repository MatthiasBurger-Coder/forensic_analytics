package de.burger.forensics.analytics.observability;

import java.util.Objects;

final class SystemLevelLogger implements LevelLogger {
    @Override
    public void log(System.Logger logger, OperationLogEvent event) {
        var verifiedLogger = Objects.requireNonNull(logger, "logger must not be null");
        var verifiedEvent = Objects.requireNonNull(event, "event must not be null");
        var level = verifiedEvent.level().systemLevel();
        if (verifiedLogger.isLoggable(level)) {
            verifiedLogger.log(level, verifiedEvent.message());
        }
    }
}
