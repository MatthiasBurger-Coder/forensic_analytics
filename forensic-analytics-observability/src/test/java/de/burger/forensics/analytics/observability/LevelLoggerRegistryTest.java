package de.burger.forensics.analytics.observability;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LevelLoggerRegistryTest {
    private final LevelLoggerRegistry registry = new LevelLoggerRegistry();
    private final RecordingSystemLogger logger = new RecordingSystemLogger();

    @Test
    void resolvesConfiguredLevels() {
        registry.get(OperationLogLevel.INFO).log(logger, OperationLogEvent.started("operation.info"));
        registry.get(OperationLogLevel.WARN).log(logger, new OperationLogEvent(
            "operation.warn",
            OperationLogPhase.SUCCEEDED,
            OperationLogLevel.WARN,
            CorrelationContext.current(),
            1,
            ""
        ));
        registry.get(OperationLogLevel.ERROR).log(logger, OperationLogEvent.failed(
            "operation.error",
            2,
            new IllegalStateException("hidden")
        ));

        assertEquals(List.of(
            System.Logger.Level.INFO,
            System.Logger.Level.WARNING,
            System.Logger.Level.ERROR
        ), logger.levels());
    }

    @Test
    void rejectsNullLevel() {
        assertThrows(NullPointerException.class, () -> registry.get(null));
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
            entries.add(new Entry(level));
        }

        @Override
        public void log(Level level, ResourceBundle bundle, String format, Object... params) {
            entries.add(new Entry(level));
        }

        private List<System.Logger.Level> levels() {
            return entries.stream().map(Entry::level).toList();
        }
    }

    private record Entry(System.Logger.Level level) {
    }
}
