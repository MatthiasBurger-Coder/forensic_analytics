package de.burger.forensics.analytics.logging.spring;

import de.burger.forensics.analytics.logging.ForensicLogLevel;
import de.burger.forensics.analytics.logging.ForensicLoggingMode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForensicLoggingSettingsTest {
    @Test
    void readsEnvironmentPropertiesWithDefaultsAndEnumNormalization() {
        var settings = ForensicLoggingSettings.from(new MockEnvironment()
            .withProperty("forensics.analytics.logging.enabled", "false")
            .withProperty("forensics.analytics.logging.base-package", "de.burger.forensics.analytics.logging")
            .withProperty("forensics.analytics.logging.mode", "annotated")
            .withProperty("forensics.analytics.logging.default-level", "error"));

        assertFalse(settings.enabled());
        assertTrue(settings.matchesBasePackage(ForensicLoggingSettingsTest.class));
        assertEquals(ForensicLoggingMode.ANNOTATED, settings.mode());
        assertEquals(ForensicLogLevel.ERROR, settings.defaultLevel());
    }

    @Test
    void matchesOnlyExactOrNestedBasePackages() {
        var settings = new ForensicLoggingSettings(
            true,
            "de.burger.forensics.analytics.logging",
            ForensicLoggingMode.APPLICATION,
            ForensicLogLevel.INFO
        );

        assertTrue(settings.matchesBasePackage(ForensicLoggingSettingsTest.class));
        assertFalse(settings.matchesBasePackage(String.class));
    }

    @Test
    void rejectsInvalidConfigurationValues() {
        assertThrows(IllegalArgumentException.class, () -> new ForensicLoggingSettings(
            true,
            "not-a-package",
            ForensicLoggingMode.APPLICATION,
            ForensicLogLevel.INFO
        ));
        assertThrows(IllegalArgumentException.class, () -> new ForensicLoggingSettings(
            true,
            ".de.burger",
            ForensicLoggingMode.APPLICATION,
            ForensicLogLevel.INFO
        ));
        assertThrows(IllegalArgumentException.class, () -> new ForensicLoggingSettings(
            true,
            "de..burger",
            ForensicLoggingMode.APPLICATION,
            ForensicLogLevel.INFO
        ));
        assertThrows(IllegalArgumentException.class, () -> new ForensicLoggingSettings(
            true,
            "de.burger.",
            ForensicLoggingMode.APPLICATION,
            ForensicLogLevel.INFO
        ));
        assertThrows(IllegalArgumentException.class, () -> new ForensicLoggingSettings(
            true,
            "1de.burger",
            ForensicLoggingMode.APPLICATION,
            ForensicLogLevel.INFO
        ));
        assertThrows(IllegalArgumentException.class, () -> ForensicLoggingSettings.from(new MockEnvironment()
            .withProperty("forensics.analytics.logging.mode", "invalid")));
        assertThrows(IllegalArgumentException.class, () -> ForensicLoggingSettings.from(new MockEnvironment()
            .withProperty("forensics.analytics.logging.default-level", "invalid")));
        assertThrows(NullPointerException.class, () -> new ForensicLoggingSettings(
            true,
            "de.burger.forensics.analytics",
            null,
            ForensicLogLevel.INFO
        ));
        assertThrows(NullPointerException.class, () -> new ForensicLoggingSettings(
            true,
            "de.burger.forensics.analytics",
            ForensicLoggingMode.APPLICATION,
            null
        ));
    }

    @Test
    void methodLoggingDependsOnVisibilityAndMode() throws Exception {
        var applicationSettings = ForensicLoggingSettings.defaults();
        var annotatedSettings = new ForensicLoggingSettings(
            true,
            "de.burger.forensics.analytics",
            ForensicLoggingMode.ANNOTATED,
            ForensicLogLevel.INFO
        );

        assertTrue(applicationSettings.shouldLogMethod(Sample.class.getMethod("publicMethod")));
        assertFalse(applicationSettings.shouldLogMethod(Object.class.getMethod("toString")));
        assertFalse(applicationSettings.shouldLogMethod(Sample.class.getDeclaredMethod("privateMethod")));
        assertFalse(annotatedSettings.shouldLogMethod(Sample.class.getMethod("publicMethod"), false));
        assertTrue(annotatedSettings.shouldLogMethod(Sample.class.getMethod("publicMethod"), true));
    }

    public static final class Sample {
        public void publicMethod() {
        }

        private void privateMethod() {
        }
    }
}
