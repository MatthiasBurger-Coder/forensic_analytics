package de.burger.forensics.analytics.observability;

enum OperationLogLevel {
    INFO(System.Logger.Level.INFO),
    WARN(System.Logger.Level.WARNING),
    ERROR(System.Logger.Level.ERROR);

    private final System.Logger.Level systemLevel;

    OperationLogLevel(System.Logger.Level systemLevel) {
        this.systemLevel = systemLevel;
    }

    System.Logger.Level systemLevel() {
        return systemLevel;
    }
}
