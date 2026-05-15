package de.burger.forensics.analytics.observability;

interface LevelLogger {
    void log(System.Logger logger, OperationLogEvent event);
}
