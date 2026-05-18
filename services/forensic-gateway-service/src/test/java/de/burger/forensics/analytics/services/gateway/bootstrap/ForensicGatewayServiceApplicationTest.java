package de.burger.forensics.analytics.services.gateway.bootstrap;

import de.burger.forensics.analytics.services.gateway.adapter.in.http.GatewayHttpHandler;
import de.burger.forensics.analytics.services.gateway.application.GatewayStatusService;
import de.burger.forensics.analytics.services.gateway.domain.DownstreamServiceStatus;
import de.burger.forensics.analytics.services.gateway.domain.GatewayStatusSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForensicGatewayServiceApplicationTest {
    @Test
    void dispatchesMainBranchesWithoutStartingSpringInHealthProbeMode() {
        var started = new AtomicBoolean();
        var exitCode = new AtomicInteger(-1);

        ForensicGatewayServiceApplication.run(
            new String[] { "--healthcheck", "--forensics.gateway.service.http.port=1" },
            args -> started.set(true),
            exitCode::set
        );

        assertFalse(started.get());
        assertEquals(1, exitCode.get());

        ForensicGatewayServiceApplication.run(
            new String[] { "--spring.main.banner-mode=off" },
            args -> started.set(true),
            exitCode::set
        );

        assertTrue(started.get());
    }

    @Test
    void startsHttpEndpointWithEphemeralPort() throws Exception {
        try (var context = new SpringApplicationBuilder(ForensicGatewayServiceApplication.class)
            .web(WebApplicationType.NONE)
            .profiles("test")
            .run("--spring.main.banner-mode=off")) {

            var http = context.getBean(GatewayHttpServerLifecycle.class);

            assertTrue(http.isRunning());
            assertTrue(http.port() > 0);
            assertEquals(200, responseCode(http.port(), "/api/health"));
            assertEquals(200, responseCode(http.port(), "/api/status"));
            assertEquals(0, HealthProbe.run(new String[] {
                "--forensics.gateway.service.http.host=127.0.0.1",
                "--forensics.gateway.service.http.port=" + http.port()
            }));

            http.start();
            assertTrue(http.isRunning());
        }
    }

    @Test
    void rejectsInvalidPropertiesAndKeepsDisabledLifecycleStopped() {
        assertThrows(NullPointerException.class, () -> new ForensicGatewayServiceProperties(null));
        assertThrows(IllegalArgumentException.class, () -> new ForensicGatewayServiceProperties.Http(true, " ", 0));
        assertThrows(IllegalArgumentException.class, () -> new ForensicGatewayServiceProperties.Http(true, null, 0));
        assertThrows(IllegalArgumentException.class, () -> new ForensicGatewayServiceProperties.Http(true, "127.0.0.1", -1));
        assertThrows(IllegalArgumentException.class, () -> new ForensicGatewayServiceProperties.Http(true, "127.0.0.1", 65_536));
        assertThrows(IllegalArgumentException.class, () -> new DownstreamServiceStatus(" ", "UP"));
        assertThrows(IllegalArgumentException.class, () -> new GatewayStatusSnapshot(" ", List.of()));

        var properties = new ForensicGatewayServiceProperties(
            new ForensicGatewayServiceProperties.Http(false, "127.0.0.1", 0)
        );
        var lifecycle = new GatewayHttpServerLifecycle(properties, new GatewayHttpHandler(new GatewayStatusService()));

        lifecycle.start();
        lifecycle.stop();

        assertFalse(lifecycle.isRunning());
        assertFalse(lifecycle.isAutoStartup());
        assertEquals(-1, lifecycle.port());
        assertTrue(HealthProbe.isHealthCheck(new String[] { "--healthcheck" }));
        assertFalse(HealthProbe.isHealthCheck(new String[] { "--spring.profiles.active=test" }));
        assertEquals(1, HealthProbe.run(new String[] {
            "--forensics.gateway.service.http.host=127.0.0.1",
            "--forensics.gateway.service.http.port=1"
        }));
    }

    @Test
    void preservesGatewayDomainStatusModels() {
        var downstream = new DownstreamServiceStatus("repository-analysis-service", "UP");
        var snapshot = new GatewayStatusSnapshot("UP", List.of(downstream));

        assertEquals("repository-analysis-service", downstream.serviceName());
        assertEquals("UP", downstream.status());
        assertEquals("UP", snapshot.status());
        assertEquals(List.of(downstream), snapshot.services());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.services().add(downstream));

        assertThrows(IllegalArgumentException.class, () -> new DownstreamServiceStatus(null, "UP"));
        assertThrows(IllegalArgumentException.class, () -> new DownstreamServiceStatus("repository-analysis-service", " "));
        assertThrows(IllegalArgumentException.class, () -> new GatewayStatusSnapshot(null, List.of()));
        assertThrows(NullPointerException.class, () -> new GatewayStatusSnapshot("UP", null));
    }

    private static int responseCode(int port, String path) throws Exception {
        var connection = URI.create("http://127.0.0.1:" + port + path).toURL().openConnection();
        return ((java.net.HttpURLConnection) connection).getResponseCode();
    }
}
