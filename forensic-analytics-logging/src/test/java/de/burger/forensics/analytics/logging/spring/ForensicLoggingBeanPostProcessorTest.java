package de.burger.forensics.analytics.logging.spring;

import de.burger.forensics.analytics.logging.ForensicLogLevel;
import de.burger.forensics.analytics.logging.ForensicLoggerFactory;
import de.burger.forensics.analytics.logging.ForensicLoggingMode;
import de.burger.forensics.analytics.loggingfixture.AnnotatedInterfaceBasedService;
import de.burger.forensics.analytics.loggingfixture.ClassAnnotatedService;
import de.burger.forensics.analytics.loggingfixture.ConfigurationFixture;
import de.burger.forensics.analytics.loggingfixture.FinalConcreteService;
import de.burger.forensics.analytics.loggingfixture.InterfaceBasedService;
import de.burger.forensics.analytics.loggingfixture.LifecycleFixture;
import de.burger.forensics.analytics.loggingfixture.OpenService;
import de.burger.forensics.analytics.loggingfixture.SampleService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForensicLoggingBeanPostProcessorTest {
    @Test
    void wrapsInterfaceBasedFinalBeansWithoutRequiringClassProxying() {
        var recordingLogger = new RecordingSystemLogger();
        var processor = new ForensicLoggingBeanPostProcessor(
            new ForensicLoggingSettings(
                true,
                "de.burger.forensics.analytics.loggingfixture",
                ForensicLoggingMode.APPLICATION,
                ForensicLogLevel.INFO
            ),
            new ForensicLoggerFactory(ignored -> recordingLogger)
        );
        var bean = new InterfaceBasedService();

        var processed = processor.postProcessAfterInitialization(bean, "interfaceBasedService");

        assertNotSame(bean, processed);
        assertInstanceOf(SampleService.class, processed);
        assertEquals("done", ((SampleService) processed).run("raw-secret"));
        assertTrue(recordingLogger.messages().contains("operation=" + InterfaceBasedService.class.getName() + ".run"));
        assertFalse(recordingLogger.messages().contains("raw-secret"));
    }

    @Test
    void leavesFinalConcreteBeansUnchangedWhenNoStableInterfaceExists() {
        var processor = new ForensicLoggingBeanPostProcessor(
            ForensicLoggingSettings.defaults(),
            new ForensicLoggerFactory()
        );
        var bean = new FinalConcreteService();

        var processed = processor.postProcessAfterInitialization(bean, "finalConcreteService");

        assertSame(bean, processed);
    }

    @Test
    void wrapsAnnotatedBeansInAnnotatedMode() {
        var recordingLogger = new RecordingSystemLogger();
        var processor = new ForensicLoggingBeanPostProcessor(
            new ForensicLoggingSettings(
                true,
                "de.burger.forensics.analytics.loggingfixture",
                ForensicLoggingMode.ANNOTATED,
                ForensicLogLevel.INFO
            ),
            new ForensicLoggerFactory(ignored -> recordingLogger)
        );
        var bean = new AnnotatedInterfaceBasedService();

        var processed = processor.postProcessAfterInitialization(bean, "annotatedInterfaceBasedService");

        assertNotSame(bean, processed);
        assertInstanceOf(SampleService.class, processed);
        assertEquals("annotated", ((SampleService) processed).run("raw-secret"));
        assertTrue(recordingLogger.messages().contains(
            "operation=" + AnnotatedInterfaceBasedService.class.getName() + ".run"
        ));
        assertFalse(recordingLogger.messages().contains("raw-secret"));
    }

    @Test
    void wrapsNonFinalBeansWithClassProxying() {
        var recordingLogger = new RecordingSystemLogger();
        var processor = new ForensicLoggingBeanPostProcessor(
            settings(ForensicLoggingMode.APPLICATION),
            new ForensicLoggerFactory(ignored -> recordingLogger)
        );
        var bean = new OpenService();

        var processed = processor.postProcessAfterInitialization(bean, "openService");

        assertNotSame(bean, processed);
        assertInstanceOf(OpenService.class, processed);
        assertEquals("open", ((OpenService) processed).run("raw-secret"));
        assertTrue(recordingLogger.messages().contains("operation=" + OpenService.class.getName() + ".run"));
    }

    @Test
    void wrapsClassAnnotatedBeansInAnnotatedMode() {
        var recordingLogger = new RecordingSystemLogger();
        var processor = new ForensicLoggingBeanPostProcessor(
            settings(ForensicLoggingMode.ANNOTATED),
            new ForensicLoggerFactory(ignored -> recordingLogger)
        );
        var bean = new ClassAnnotatedService();

        var processed = processor.postProcessAfterInitialization(bean, "classAnnotatedService");

        assertNotSame(bean, processed);
        assertEquals("class-annotated", ((SampleService) processed).run("raw-secret"));
        assertTrue(recordingLogger.messages().contains("operation=" + ClassAnnotatedService.class.getName() + ".run"));
    }

    @Test
    void leavesIneligibleBeansUnchanged() {
        var processor = new ForensicLoggingBeanPostProcessor(
            new ForensicLoggingSettings(
                false,
                "de.burger.forensics.analytics.loggingfixture",
                ForensicLoggingMode.APPLICATION,
                ForensicLogLevel.INFO
            ),
            new ForensicLoggerFactory()
        );

        var bean = new InterfaceBasedService();
        assertSame(bean, processor.postProcessAfterInitialization(bean, "disabledService"));

        var enabledProcessor = new ForensicLoggingBeanPostProcessor(
            settings(ForensicLoggingMode.APPLICATION),
            new ForensicLoggerFactory()
        );
        var outsidePackageBean = "outside";
        var loggingInfrastructureBean = new LoggingInfrastructureFixture();
        var configurationBean = new ConfigurationFixture();
        var lifecycleBean = new LifecycleFixture();
        var proxy = enabledProcessor.postProcessAfterInitialization(new InterfaceBasedService(), "proxySource");

        assertSame(outsidePackageBean, enabledProcessor.postProcessAfterInitialization(outsidePackageBean, "outside"));
        assertSame(
            loggingInfrastructureBean,
            enabledProcessor.postProcessAfterInitialization(loggingInfrastructureBean, "loggingInfrastructure")
        );
        assertSame(configurationBean, enabledProcessor.postProcessAfterInitialization(configurationBean, "configuration"));
        assertSame(lifecycleBean, enabledProcessor.postProcessAfterInitialization(lifecycleBean, "lifecycle"));
        assertSame(proxy, enabledProcessor.postProcessAfterInitialization(proxy, "alreadyProxy"));
    }

    private static ForensicLoggingSettings settings(ForensicLoggingMode mode) {
        return new ForensicLoggingSettings(
            true,
            "de.burger.forensics.analytics.loggingfixture",
            mode,
            ForensicLogLevel.INFO
        );
    }

    public static final class LoggingInfrastructureFixture {
        public String run() {
            return "logging-infrastructure";
        }
    }

    private static final class RecordingSystemLogger implements System.Logger {
        private final List<String> messages = new ArrayList<>();

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
            messages.add(message);
        }

        @Override
        public void log(Level level, ResourceBundle bundle, String format, Object... params) {
            messages.add(format);
        }

        private String messages() {
            return String.join("\n", messages);
        }
    }
}
