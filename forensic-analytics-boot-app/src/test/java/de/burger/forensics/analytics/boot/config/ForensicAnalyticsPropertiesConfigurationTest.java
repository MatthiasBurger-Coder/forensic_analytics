package de.burger.forensics.analytics.boot.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForensicAnalyticsPropertiesConfigurationTest {
    private final ForensicAnalyticsPropertiesConfiguration configuration =
        new ForensicAnalyticsPropertiesConfiguration();

    @Test
    void defaultsMatchManualBootstrapBehavior() {
        var properties = configuration.forensicAnalyticsProperties(new MockEnvironment());

        assertEquals(Path.of("/var/lib/forensic-analytics").toAbsolutePath().normalize(), properties.workspace().rootPath());
        assertEquals(
            Path.of("/var/lib/forensic-analytics/workspaces").toAbsolutePath().normalize(),
            properties.workspace().basePath()
        );
        assertTrue(properties.grpc().enabled());
        assertEquals("127.0.0.1", properties.grpc().host());
        assertEquals(9090, properties.grpc().port());
        assertTrue(properties.rest().enabled());
        assertEquals("127.0.0.1", properties.rest().host());
        assertEquals(8080, properties.rest().port());
        assertFalse(properties.joern().enabled());
        assertTrue(properties.joern().failOnError());
        assertTrue(properties.observability().loggingEnabled());
    }

    @Test
    void overridesTypedProperties() {
        var environment = new MockEnvironment()
            .withProperty("forensics.analytics.workspace.root-path", "build")
            .withProperty("forensics.analytics.workspace.base-path", "build/custom-workspaces")
            .withProperty("forensics.analytics.workspace.allow-relative-paths", "true")
            .withProperty("forensics.analytics.ingestion.grpc.enabled", "false")
            .withProperty("forensics.analytics.ingestion.grpc.host", "127.0.0.2")
            .withProperty("forensics.analytics.ingestion.grpc.port", "9191")
            .withProperty("forensics.analytics.rest.enabled", "false")
            .withProperty("forensics.analytics.rest.host", "0.0.0.0")
            .withProperty("forensics.analytics.rest.port", "8181")
            .withProperty("forensics.analytics.joern.enabled", "true")
            .withProperty(
                "forensics.analytics.joern.container-image",
                "example.invalid/joern@sha256:1111111111111111111111111111111111111111111111111111111111111111"
            )
            .withProperty("forensics.analytics.joern.output-directory", "build/joern-output")
            .withProperty("forensics.analytics.joern.timeout", "PT2M")
            .withProperty("forensics.analytics.joern.fail-on-error", "false")
            .withProperty("forensics.analytics.observability.logging.enabled", "false");

        var properties = configuration.forensicAnalyticsProperties(environment);

        assertEquals(Path.of("build").toAbsolutePath().normalize(), properties.workspace().rootPath());
        assertEquals(Path.of("build/custom-workspaces").toAbsolutePath().normalize(), properties.workspace().basePath());
        assertFalse(properties.grpc().enabled());
        assertEquals("127.0.0.2", properties.grpc().host());
        assertEquals(9191, properties.grpc().port());
        assertFalse(properties.rest().enabled());
        assertEquals("0.0.0.0", properties.rest().host());
        assertEquals(8181, properties.rest().port());
        assertTrue(properties.joern().enabled());
        assertFalse(properties.joern().failOnError());
        assertFalse(properties.observability().loggingEnabled());
    }

    @Test
    void allowsEphemeralPortsForDeterministicLifecycleTests() {
        var properties = configuration.forensicAnalyticsProperties(new MockEnvironment()
            .withProperty("forensics.analytics.ingestion.grpc.port", "0")
            .withProperty("forensics.analytics.rest.port", "0"));

        assertEquals(0, properties.grpc().port());
        assertEquals(0, properties.rest().port());
    }

    @Test
    void rejectsInvalidPortsBlankPathsAndEscapingWorkspacePaths() {
        assertThrows(IllegalArgumentException.class, () -> configuration.forensicAnalyticsProperties(
            new MockEnvironment().withProperty("forensics.analytics.ingestion.grpc.port", "-1")
        ));
        assertThrows(IllegalArgumentException.class, () -> configuration.forensicAnalyticsProperties(
            new MockEnvironment().withProperty("forensics.analytics.rest.port", "65536")
        ));
        assertThrows(IllegalArgumentException.class, () -> new ForensicAnalyticsProperties.Grpc(true, null, 9090));
        assertThrows(IllegalArgumentException.class, () -> configuration.forensicAnalyticsProperties(
            new MockEnvironment().withProperty("forensics.analytics.ingestion.grpc.host", "")
        ));
        assertThrows(IllegalArgumentException.class, () -> new ForensicAnalyticsProperties.Rest(true, null, 8080));
        assertThrows(IllegalArgumentException.class, () -> configuration.forensicAnalyticsProperties(
            new MockEnvironment().withProperty("forensics.analytics.rest.host", "")
        ));
        assertThrows(IllegalArgumentException.class, () -> configuration.forensicAnalyticsProperties(
            new MockEnvironment().withProperty("forensics.analytics.workspace.base-path", "")
        ));
        assertThrows(IllegalArgumentException.class, () -> configuration.forensicAnalyticsProperties(
            new MockEnvironment().withProperty("forensics.analytics.workspace.base-path", "relative-workspaces")
        ));
        assertThrows(IllegalArgumentException.class, () -> configuration.forensicAnalyticsProperties(
            new MockEnvironment()
                .withProperty("forensics.analytics.workspace.root-path", "/var/lib/forensic-analytics")
                .withProperty("forensics.analytics.workspace.base-path", "/tmp/forensic-analytics-workspaces")
        ));
        assertThrows(IllegalArgumentException.class, () -> configuration.forensicAnalyticsProperties(
            new MockEnvironment()
                .withProperty("forensics.analytics.workspace.root-path", "/var/lib/forensic-analytics")
                .withProperty("forensics.analytics.joern.output-directory", "/tmp/joern")
        ));
        assertThrows(IllegalArgumentException.class, () -> new ForensicAnalyticsProperties.Workspace(
            Path.of("/"),
            Path.of("/var/lib/forensic-analytics/workspaces"),
            false
        ));
        assertThrows(IllegalArgumentException.class, () -> new ForensicAnalyticsProperties.Workspace(
            Path.of(System.getProperty("user.home")),
            Path.of(System.getProperty("user.home")).resolve("workspaces"),
            false
        ));
        assertThrows(IllegalArgumentException.class, () -> new ForensicAnalyticsProperties.Joern(
            false,
            null,
            Path.of("/var/lib/forensic-analytics/workspaces/joern"),
            Duration.ofMinutes(5),
            true
        ));
        assertThrows(IllegalArgumentException.class, () -> new ForensicAnalyticsProperties.Joern(
            false,
            "",
            Path.of("/var/lib/forensic-analytics/workspaces/joern"),
            Duration.ofMinutes(5),
            true
        ));
        assertThrows(IllegalArgumentException.class, () -> new ForensicAnalyticsProperties.Joern(
            false,
            "example.invalid/joern@sha256:1111111111111111111111111111111111111111111111111111111111111111",
            Path.of("/var/lib/forensic-analytics/workspaces/joern"),
            Duration.ZERO,
            true
        ));
        assertThrows(IllegalArgumentException.class, () -> new ForensicAnalyticsProperties.Joern(
            false,
            "example.invalid/joern@sha256:1111111111111111111111111111111111111111111111111111111111111111",
            Path.of("/var/lib/forensic-analytics/workspaces/joern"),
            Duration.ofSeconds(-1),
            true
        ));
    }

    @Test
    void rejectsMissingTopLevelPropertyGroups() {
        var workspace = new ForensicAnalyticsProperties.Workspace(
            Path.of("/var/lib/forensic-analytics"),
            Path.of("/var/lib/forensic-analytics/workspaces"),
            false
        );
        var grpc = new ForensicAnalyticsProperties.Grpc(true, "127.0.0.1", 9090);
        var rest = new ForensicAnalyticsProperties.Rest(true, "127.0.0.1", 8080);
        var joern = new ForensicAnalyticsProperties.Joern(
            false,
            "example.invalid/joern@sha256:1111111111111111111111111111111111111111111111111111111111111111",
            Path.of("/var/lib/forensic-analytics/workspaces/joern"),
            Duration.ofMinutes(5),
            true
        );
        var observability = new ForensicAnalyticsProperties.Observability(true);

        assertThrows(NullPointerException.class, () -> new ForensicAnalyticsProperties(
            null,
            grpc,
            rest,
            joern,
            observability
        ));
        assertThrows(NullPointerException.class, () -> new ForensicAnalyticsProperties(
            workspace,
            null,
            rest,
            joern,
            observability
        ));
        assertThrows(NullPointerException.class, () -> new ForensicAnalyticsProperties(
            workspace,
            grpc,
            null,
            joern,
            observability
        ));
        assertThrows(NullPointerException.class, () -> new ForensicAnalyticsProperties(
            workspace,
            grpc,
            rest,
            null,
            observability
        ));
        assertThrows(NullPointerException.class, () -> new ForensicAnalyticsProperties(
            workspace,
            grpc,
            rest,
            joern,
            null
        ));
    }
}
