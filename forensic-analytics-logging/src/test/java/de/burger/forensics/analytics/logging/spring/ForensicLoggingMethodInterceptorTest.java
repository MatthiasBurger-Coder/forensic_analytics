package de.burger.forensics.analytics.logging.spring;

import de.burger.forensics.analytics.logging.ForensicLogLevel;
import de.burger.forensics.analytics.logging.ForensicLoggable;
import de.burger.forensics.analytics.logging.ForensicLoggerFactory;
import de.burger.forensics.analytics.logging.ForensicLoggingMode;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForensicLoggingMethodInterceptorTest {
    @Test
    void logsAnnotatedMethodWithoutArgumentsResultOrRawExceptionMessage() {
        var recordingLogger = new RecordingSystemLogger();
        var proxy = proxiedService(
            new ForensicLoggingSettings(
                true,
                "de.burger.forensics.analytics.logging.spring",
                ForensicLoggingMode.APPLICATION,
                ForensicLogLevel.INFO
            ),
            recordingLogger
        );

        assertEquals("hello", proxy.greet("raw-secret-argument"));
        assertThrows(IllegalArgumentException.class, () -> proxy.fail("hidden-message"));

        assertTrue(recordingLogger.entries().stream().anyMatch(entry -> entry.level() == System.Logger.Level.DEBUG));
        assertTrue(recordingLogger.messages().contains("SampleService.greet"));
        assertTrue(recordingLogger.messages().contains("phase=SUCCEEDED"));
        assertTrue(recordingLogger.messages().contains("errorType=IllegalArgumentException"));
        assertFalse(recordingLogger.messages().contains("raw-secret-argument"));
        assertFalse(recordingLogger.messages().contains("hidden-message"));
        assertFalse(recordingLogger.messages().contains("hello"));
    }

    @Test
    void annotatedModeSkipsUnannotatedMethods() {
        var recordingLogger = new RecordingSystemLogger();
        var proxy = proxiedService(
            new ForensicLoggingSettings(
                true,
                "de.burger.forensics.analytics.logging.spring",
                ForensicLoggingMode.ANNOTATED,
                ForensicLogLevel.INFO
            ),
            recordingLogger
        );

        assertEquals("plain", proxy.plain());

        assertTrue(recordingLogger.entries().isEmpty());
    }

    private static SampleService proxiedService(
        ForensicLoggingSettings settings,
        RecordingSystemLogger recordingLogger
    ) {
        var target = new SampleService();
        var proxyFactory = new ProxyFactory(target);
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvice(
            new ForensicLoggingMethodInterceptor(
                settings,
                new ForensicLoggerFactory(ignored -> recordingLogger),
                SampleService.class
            )
        );
        return (SampleService) proxyFactory.getProxy();
    }

    static class SampleService {
        @ForensicLoggable(ForensicLogLevel.DEBUG)
        public String greet(String value) {
            return "hello";
        }

        public String plain() {
            return "plain";
        }

        @ForensicLoggable
        public String fail(String value) {
            throw new IllegalArgumentException(value);
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
