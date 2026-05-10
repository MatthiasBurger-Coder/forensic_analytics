package de.burger.forensics.analytics.bootstrap;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrpcIngestionServerSettingsTest {
    @Test
    void defaultsToEnabledPort9090() {
        var settings = GrpcIngestionServerSettings.from(new Properties(), Map.of());

        assertTrue(settings.enabled());
        assertEquals(9090, settings.port());
    }

    @Test
    void propertiesOverrideEnvironment() {
        var properties = new Properties();
        properties.setProperty("forensics.analytics.ingestion.grpc.enabled", "false");
        properties.setProperty("forensics.analytics.ingestion.grpc.port", "9191");

        var settings = GrpcIngestionServerSettings.from(
            properties,
            Map.of(
                "FORENSICS_ANALYTICS_INGESTION_GRPC_ENABLED", "true",
                "FORENSICS_ANALYTICS_INGESTION_GRPC_PORT", "9292"
            )
        );

        assertFalse(settings.enabled());
        assertEquals(9191, settings.port());
    }

    @Test
    void rejectsInvalidPort() {
        assertThrows(IllegalArgumentException.class, () -> new GrpcIngestionServerSettings(true, 0));
    }

    @Test
    void readsSystemPropertiesFromEnvironmentFactory() {
        System.setProperty("forensics.analytics.ingestion.grpc.enabled", "false");
        System.setProperty("forensics.analytics.ingestion.grpc.port", "9393");
        try {
            var settings = GrpcIngestionServerSettings.fromEnvironment();

            assertFalse(settings.enabled());
            assertEquals(9393, settings.port());
        } finally {
            System.clearProperty("forensics.analytics.ingestion.grpc.enabled");
            System.clearProperty("forensics.analytics.ingestion.grpc.port");
        }
    }
}
