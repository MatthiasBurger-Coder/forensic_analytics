package de.burger.forensics.analytics.bootstrap;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestApiServerSettingsTest {
    @Test
    void defaultsToEnabledLocalhostPort8080() {
        var settings = RestApiServerSettings.from(new Properties(), Map.of());

        assertTrue(settings.enabled());
        assertEquals("127.0.0.1", settings.host());
        assertEquals(8080, settings.port());
    }

    @Test
    void propertiesOverrideEnvironment() {
        var properties = new Properties();
        properties.setProperty("forensics.analytics.rest.enabled", "false");
        properties.setProperty("forensics.analytics.rest.host", "0.0.0.0");
        properties.setProperty("forensics.analytics.rest.port", "8181");

        var settings = RestApiServerSettings.from(
            properties,
            Map.of(
                "FORENSICS_ANALYTICS_REST_ENABLED", "true",
                "FORENSICS_ANALYTICS_REST_HOST", "127.0.0.1",
                "FORENSICS_ANALYTICS_REST_PORT", "8282"
            )
        );

        assertFalse(settings.enabled());
        assertEquals("0.0.0.0", settings.host());
        assertEquals(8181, settings.port());
    }

    @Test
    void rejectsInvalidSettings() {
        assertThrows(IllegalArgumentException.class, () -> new RestApiServerSettings(true, "", 8080));
        assertThrows(IllegalArgumentException.class, () -> new RestApiServerSettings(true, "127.0.0.1", 0));
    }

    @Test
    void readsSystemPropertiesFromEnvironmentFactory() {
        System.setProperty("forensics.analytics.rest.enabled", "false");
        System.setProperty("forensics.analytics.rest.host", "0.0.0.0");
        System.setProperty("forensics.analytics.rest.port", "8383");
        try {
            var settings = RestApiServerSettings.fromEnvironment();

            assertFalse(settings.enabled());
            assertEquals("0.0.0.0", settings.host());
            assertEquals(8383, settings.port());
        } finally {
            System.clearProperty("forensics.analytics.rest.enabled");
            System.clearProperty("forensics.analytics.rest.host");
            System.clearProperty("forensics.analytics.rest.port");
        }
    }
}
