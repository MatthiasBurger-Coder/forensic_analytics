package de.burger.forensics.analytics.observability;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemOperationLoggerTest {
    private final RecordingSystemLogger systemLogger = new RecordingSystemLogger();
    private final SystemOperationLogger operationLogger = new SystemOperationLogger(systemLogger);

    @AfterEach
    void clearContext() {
        CorrelationContext.clear();
    }

    @Test
    void logsStartedAndSucceededEventsWithCurrentCorrelationId() {
        try (var scope = CorrelationContext.open(new CorrelationId("corr-1"))) {
            assertEquals("corr-1", scope.correlationId().value());
            operationLogger.started("rest.repository-analyses.GET");
            operationLogger.succeeded("rest.repository-analyses.GET", 12);
        }

        assertEquals(2, systemLogger.entries.size());
        assertEquals(System.Logger.Level.INFO, systemLogger.entries.get(0).level());
        assertTrue(systemLogger.entries.get(0).message().contains("phase=STARTED"));
        assertTrue(systemLogger.entries.get(0).message().contains("correlationId=corr-1"));
        assertTrue(systemLogger.entries.get(1).message().contains("phase=SUCCEEDED"));
        assertTrue(systemLogger.entries.get(1).message().contains("durationMs=12"));
    }

    @Test
    void failureLogContainsErrorTypeWithoutExceptionMessage() {
        operationLogger.failed(
            "grpc.UploadAnalysisData",
            3,
            new IllegalStateException("token=secret at example.Secret(File.java:1)")
        );

        assertEquals(1, systemLogger.entries.size());
        var entry = systemLogger.entries.getFirst();
        assertEquals(System.Logger.Level.ERROR, entry.level());
        assertTrue(entry.message().contains("errorType=IllegalStateException"));
        assertFalse(entry.message().contains("secret"));
        assertFalse(entry.message().contains("example.Secret"));
    }

    @Test
    void suppressesMessagesWhenSystemLoggerLevelIsDisabled() {
        systemLogger.loggable = false;

        operationLogger.started("cli.analyze");

        assertTrue(systemLogger.entries.isEmpty());
    }

    @Test
    void validatesOperationNamesAndDurations() {
        assertThrows(IllegalArgumentException.class, () -> operationLogger.started(" "));
        assertThrows(IllegalArgumentException.class, () -> operationLogger.succeeded("cli.analyze", -2));
        assertThrows(NullPointerException.class, () -> operationLogger.failed("cli.analyze", 1, null));
    }

    private static final class RecordingSystemLogger implements System.Logger {
        private final List<Entry> entries = new ArrayList<>();
        private boolean loggable = true;

        @Override
        public String getName() {
            return "recording";
        }

        @Override
        public boolean isLoggable(Level level) {
            return loggable;
        }

        @Override
        public void log(Level level, ResourceBundle bundle, String message, Throwable thrown) {
            entries.add(new Entry(level, message));
        }

        @Override
        public void log(Level level, ResourceBundle bundle, String format, Object... params) {
            entries.add(new Entry(level, format));
        }
    }

    private record Entry(System.Logger.Level level, String message) {
    }
}
