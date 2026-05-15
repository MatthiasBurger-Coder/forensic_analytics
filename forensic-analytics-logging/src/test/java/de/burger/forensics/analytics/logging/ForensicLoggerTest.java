package de.burger.forensics.analytics.logging;

import de.burger.forensics.analytics.observability.CorrelationContext;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForensicLoggerTest {
    @Test
    void logsOperationWithCorrelationIdWithoutArgumentsOrResultValues() {
        var recordingLogger = new RecordingSystemLogger();
        var logger = new ForensicLoggerFactory(ignored -> recordingLogger).logger(ForensicLoggerTest.class);

        var result = logger.logged("analysis.run", () -> "secret-result");

        assertEquals("secret-result", result);
        assertEquals(2, recordingLogger.entries().size());
        assertTrue(recordingLogger.entries().getFirst().message().contains("operation=analysis.run"));
        assertTrue(recordingLogger.entries().getFirst().message().contains("phase=STARTED"));
        assertTrue(recordingLogger.entries().getFirst().message().contains("correlationId="));
        assertTrue(recordingLogger.entries().get(1).message().contains("phase=SUCCEEDED"));
        assertFalse(recordingLogger.messages().contains("secret-result"));
    }

    @Test
    void logsExceptionTypeWithoutRawExceptionMessage() {
        var recordingLogger = new RecordingSystemLogger();
        var logger = new ForensicLoggerFactory(ignored -> recordingLogger).logger(ForensicLoggerTest.class);

        assertThrows(IllegalStateException.class, () -> logger.logged("analysis.fail", () -> {
            throw new IllegalStateException("raw secret detail");
        }));

        assertTrue(recordingLogger.messages().contains("errorType=IllegalStateException"));
        assertFalse(recordingLogger.messages().contains("raw secret detail"));
    }

    @Test
    void loggingBackendFailureDoesNotChangeApplicationBehavior() {
        var logger = new ForensicLoggerFactory(ignored -> new ThrowingSystemLogger()).logger(ForensicLoggerTest.class);

        assertDoesNotThrow(() -> logger.info("analysis.event"));
        assertEquals("done", logger.logged("analysis.operation", () -> "done"));
    }

    @Test
    void skipsWritesWhenBackendLevelIsDisabled() {
        var filteringLogger = new FilteringSystemLogger();
        var logger = new ForensicLoggerFactory(ignored -> filteringLogger).logger(ForensicLoggerTest.class);

        logger.warn("analysis.skipped");

        assertTrue(filteringLogger.entries().isEmpty());
    }

    @Test
    void preservesExistingCorrelationScope() {
        var recordingLogger = new RecordingSystemLogger();
        var logger = new ForensicLoggerFactory(ignored -> recordingLogger).logger(ForensicLoggerTest.class);

        try (var scope = CorrelationContext.open("external correlation")) {
            assertNotNull(scope);
            logger.error("analysis.event");

            assertTrue(recordingLogger.messages().contains("correlationId=external_correlation"));
            assertTrue(CorrelationContext.current().isPresent());
        }
    }

    @Test
    void rejectsBlankOperationalTokens() {
        var logger = new ForensicLoggerFactory(ignored -> new RecordingSystemLogger()).logger(ForensicLoggerTest.class);

        assertThrows(IllegalArgumentException.class, () -> logger.info("   "));
        assertThrows(IllegalArgumentException.class, () -> logger.operationStarted("   ", ForensicLogLevel.INFO));
    }

    @Test
    void defaultRunnableLoggingDelegatesToSupplierLogging() {
        var logger = new RecordingForensicLogger();

        logger.logged("analysis.runnable", () -> logger.info("inside"));

        assertEquals("analysis.runnable", logger.loggedOperation());
        assertTrue(logger.events().contains("inside"));
    }

    @Test
    void staticFactoryCreatesSystemLoggerForSource() {
        assertNotNull(ForensicLogger.forSource(ForensicLoggerTest.class));
        assertNotNull(ForensicLoggerFactory.system().logger(ForensicLoggerTest.class));
    }

    @Test
    void factoryRejectsMissingSourceAndProvider() {
        assertThrows(NullPointerException.class, () -> new ForensicLoggerFactory(null));
        var factory = new ForensicLoggerFactory(ignored -> new RecordingSystemLogger());

        assertThrows(NullPointerException.class, () -> factory.logger(null));
    }

    private static final class RecordingForensicLogger implements ForensicLogger {
        private final List<String> events = new ArrayList<>();
        private String loggedOperation;

        @Override
        public void trace(String event) {
            events.add(event);
        }

        @Override
        public void debug(String event) {
            events.add(event);
        }

        @Override
        public void info(String event) {
            events.add(event);
        }

        @Override
        public void warn(String event) {
            events.add(event);
        }

        @Override
        public void error(String event) {
            events.add(event);
        }

        @Override
        public void failed(String event, Throwable error) {
            events.add(event);
        }

        @Override
        public void operationStarted(String operation, ForensicLogLevel level) {
            events.add(operation + ":started");
        }

        @Override
        public void operationSucceeded(String operation, ForensicLogLevel level, long durationMillis) {
            events.add(operation + ":succeeded");
        }

        @Override
        public void operationFailed(String operation, long durationMillis, Throwable error) {
            events.add(operation + ":failed");
        }

        @Override
        public <T> T logged(String operation, java.util.function.Supplier<T> action) {
            loggedOperation = operation;
            return action.get();
        }

        private List<String> events() {
            return List.copyOf(events);
        }

        private String loggedOperation() {
            return loggedOperation;
        }
    }

    private static final class ThrowingSystemLogger implements System.Logger {
        @Override
        public String getName() {
            return "throwing";
        }

        @Override
        public boolean isLoggable(Level level) {
            throw new IllegalStateException("backend unavailable");
        }

        @Override
        public void log(Level level, ResourceBundle bundle, String message, Throwable thrown) {
            throw new IllegalStateException("backend unavailable");
        }

        @Override
        public void log(Level level, ResourceBundle bundle, String format, Object... params) {
            throw new IllegalStateException("backend unavailable");
        }
    }

    private static final class FilteringSystemLogger implements System.Logger {
        private final List<Entry> entries = new ArrayList<>();

        @Override
        public String getName() {
            return "filtering";
        }

        @Override
        public boolean isLoggable(Level level) {
            return false;
        }

        @Override
        public void log(Level level, ResourceBundle bundle, String message, Throwable thrown) {
            entries.add(new Entry(level, message));
        }

        @Override
        public void log(Level level, ResourceBundle bundle, String format, Object... params) {
            entries.add(new Entry(level, format));
        }

        private List<Entry> entries() {
            return List.copyOf(entries);
        }
    }

    private static final class RecordingSystemLogger implements System.Logger {
        private final List<Entry> entries = new ArrayList<>();

        @Override
        public String getName() {
            return "recording";
        }

        @Override
        public boolean isLoggable(Level level) {
            return true;
        }

        @Override
        public void log(Level level, ResourceBundle bundle, String message, Throwable thrown) {
            entries.add(new Entry(level, message));
        }

        @Override
        public void log(Level level, ResourceBundle bundle, String format, Object... params) {
            entries.add(new Entry(level, format));
        }

        private List<Entry> entries() {
            return List.copyOf(entries);
        }

        private String messages() {
            var messages = new StringBuilder();
            entries.forEach(entry -> messages.append(entry.message()).append('\n'));
            return messages.toString();
        }
    }

    private record Entry(System.Logger.Level level, String message) {
    }
}
