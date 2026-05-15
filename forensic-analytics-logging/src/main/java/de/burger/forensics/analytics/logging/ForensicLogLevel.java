package de.burger.forensics.analytics.logging;

public enum ForensicLogLevel {
    TRACE(System.Logger.Level.TRACE),
    DEBUG(System.Logger.Level.DEBUG),
    INFO(System.Logger.Level.INFO),
    WARN(System.Logger.Level.WARNING),
    ERROR(System.Logger.Level.ERROR);

    private final System.Logger.Level systemLevel;

    ForensicLogLevel(System.Logger.Level systemLevel) {
        this.systemLevel = systemLevel;
    }

    System.Logger.Level systemLevel() {
        return systemLevel;
    }
}
