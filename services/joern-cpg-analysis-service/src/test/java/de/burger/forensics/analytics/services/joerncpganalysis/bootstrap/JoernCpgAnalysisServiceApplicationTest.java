package de.burger.forensics.analytics.services.joerncpganalysis.bootstrap;

import de.burger.forensics.analytics.services.joerncpganalysis.adapter.in.grpc.JoernCpgAnalysisGrpcEndpoint;
import de.burger.forensics.analytics.services.joerncpganalysis.application.JoernCpgAnalysisApplicationService;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.JoernArtifactCollectionResult;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.JoernRuntimeResult;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JoernCpgAnalysisServiceApplicationTest {
    @Test
    void dispatchesMainBranchesWithoutStartingSpringInHealthProbeMode() {
        var started = new AtomicBoolean();
        var exitCode = new AtomicInteger(-1);

        JoernCpgAnalysisServiceApplication.run(
            new String[] { "--healthcheck", "--forensics.joern-cpg-analysis.service.health.port=1" },
            args -> started.set(true),
            exitCode::set
        );

        assertFalse(started.get());
        assertEquals(1, exitCode.get());

        JoernCpgAnalysisServiceApplication.run(
            new String[] { "--spring.main.banner-mode=off" },
            args -> started.set(true),
            exitCode::set
        );

        assertTrue(started.get());
    }

    @Test
    void startsGrpcAndHealthEndpointsWithEphemeralPorts() throws Exception {
        try (var context = new SpringApplicationBuilder(JoernCpgAnalysisServiceApplication.class)
            .web(WebApplicationType.NONE)
            .profiles("test")
            .run("--spring.main.banner-mode=off")) {

            var grpc = context.getBean(GrpcServerLifecycle.class);
            var health = context.getBean(HealthHttpServerLifecycle.class);

            assertTrue(grpc.isRunning());
            assertTrue(grpc.port() > 0);
            assertTrue(health.isRunning());
            assertTrue(health.port() > 0);
            assertEquals(200, healthResponseCode(health.port()));
        }
    }

    @Test
    void healthProbeAndDisabledLifecyclesBehaveDeterministically() {
        var properties = new JoernCpgAnalysisServiceProperties(
            new JoernCpgAnalysisServiceProperties.Grpc(false, "127.0.0.1", 0),
            new JoernCpgAnalysisServiceProperties.Health(false, "127.0.0.1", 0),
            new JoernCpgAnalysisServiceProperties.Workspace(Path.of("build/test-workspaces")),
            new JoernCpgAnalysisServiceProperties.Artifacts(Path.of("build/test-artifacts")),
            new JoernCpgAnalysisServiceProperties.Joern("joern", "joern-parse", "64m", Path.of("build/test-queries"), image())
        );
        var grpc = new GrpcServerLifecycle(properties, null);
        var health = new HealthHttpServerLifecycle(properties, grpc);

        grpc.start();
        health.start();
        grpc.stop();
        health.stop();

        assertTrue(HealthProbe.isHealthCheck(new String[] { "--healthcheck" }));
        assertFalse(HealthProbe.isHealthCheck(new String[] { "--spring.profiles.active=test" }));
        assertEquals(1, HealthProbe.run(new String[] {
            "--forensics.joern-cpg-analysis.service.health.host=127.0.0.1",
            "--forensics.joern-cpg-analysis.service.health.port=1"
        }));
        assertFalse(grpc.isRunning());
        assertFalse(grpc.isAutoStartup());
        assertEquals(-1, grpc.port());
        assertFalse(health.isRunning());
        assertFalse(health.isAutoStartup());
        assertEquals(-1, health.port());
    }

    @Test
    void defaultPropertiesAndRepeatedLifecycleStartsAreDeterministic() throws Exception {
        var defaults = new JoernCpgAnalysisServiceProperties(null, null, null, null, null);
        assertEquals(9094, defaults.grpc().port());
        assertEquals(8085, defaults.health().port());
        assertEquals(Path.of("build/joern-cpg-workspaces"), defaults.workspace().root());
        assertEquals("joern", defaults.joern().executable());

        var properties = new JoernCpgAnalysisServiceProperties(
            new JoernCpgAnalysisServiceProperties.Grpc(true, "127.0.0.1", 0),
            new JoernCpgAnalysisServiceProperties.Health(true, "127.0.0.1", 0),
            new JoernCpgAnalysisServiceProperties.Workspace(Path.of("build/test-workspaces")),
            new JoernCpgAnalysisServiceProperties.Artifacts(Path.of("build/test-artifacts")),
            new JoernCpgAnalysisServiceProperties.Joern("joern", "joern-parse", "64m", Path.of("build/test-queries"), image())
        );
        var grpc = new GrpcServerLifecycle(properties, endpoint());
        var health = new HealthHttpServerLifecycle(properties, grpc);

        try {
            grpc.start();
            grpc.start();
            health.start();
            health.start();

            assertTrue(grpc.isRunning());
            assertTrue(health.isRunning());
            assertEquals(0, HealthProbe.run(new String[] {
                "--forensics.joern-cpg-analysis.service.health.host=127.0.0.1",
                "--forensics.joern-cpg-analysis.service.health.port=" + health.port()
            }));
        } finally {
            health.stop();
            grpc.stop();
        }
    }

    @Test
    void rejectsInvalidServicePropertiesAndReportsDownWhenGrpcStopped() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> new JoernCpgAnalysisServiceProperties.Grpc(true, " ", 0));
        assertThrows(IllegalArgumentException.class, () -> new JoernCpgAnalysisServiceProperties.Grpc(true, "127.0.0.1", -1));
        assertThrows(IllegalArgumentException.class, () -> new JoernCpgAnalysisServiceProperties.Health(true, null, 0));
        assertThrows(IllegalArgumentException.class, () -> new JoernCpgAnalysisServiceProperties.Health(true, "127.0.0.1", 65_536));
        assertThrows(IllegalArgumentException.class, () -> new JoernCpgAnalysisServiceProperties.Workspace(null));
        assertThrows(IllegalArgumentException.class, () -> new JoernCpgAnalysisServiceProperties.Artifacts(null));
        assertThrows(IllegalArgumentException.class, () -> new JoernCpgAnalysisServiceProperties.Joern("", "joern-parse", "64m", Path.of("queries"), image()));
        assertThrows(IllegalArgumentException.class, () -> new JoernCpgAnalysisServiceProperties.Joern("joern", " ", "64m", Path.of("queries"), image()));
        assertThrows(IllegalArgumentException.class, () -> new JoernCpgAnalysisServiceProperties.Joern("joern", "joern-parse", "", Path.of("queries"), image()));
        assertThrows(IllegalArgumentException.class, () -> new JoernCpgAnalysisServiceProperties.Joern("joern", "joern-parse", "64m", null, image()));
        assertThrows(IllegalArgumentException.class, () -> new JoernCpgAnalysisServiceProperties.Joern("joern", "joern-parse", "64m", Path.of("queries"), "ghcr.io/joernio/joern:latest"));

        var properties = new JoernCpgAnalysisServiceProperties(
            new JoernCpgAnalysisServiceProperties.Grpc(true, "127.0.0.1", 0),
            new JoernCpgAnalysisServiceProperties.Health(true, "127.0.0.1", 0),
            new JoernCpgAnalysisServiceProperties.Workspace(Path.of("build/test-workspaces")),
            new JoernCpgAnalysisServiceProperties.Artifacts(Path.of("build/test-artifacts")),
            new JoernCpgAnalysisServiceProperties.Joern("joern", "joern-parse", "64m", Path.of("build/test-queries"), image())
        );
        var grpc = new GrpcServerLifecycle(properties, null);
        var health = new HealthHttpServerLifecycle(properties, grpc);

        try {
            health.start();
            assertEquals(503, healthResponseCode(health.port()));
        } finally {
            health.stop();
        }
    }

    private static int healthResponseCode(int port) throws Exception {
        var connection = URI.create("http://127.0.0.1:" + port + "/health").toURL().openConnection();
        return ((java.net.HttpURLConnection) connection).getResponseCode();
    }

    private static JoernCpgAnalysisGrpcEndpoint endpoint() {
        return new JoernCpgAnalysisGrpcEndpoint(new JoernCpgAnalysisApplicationService(
            command -> {
                throw new UnsupportedOperationException("workspace not used by lifecycle test");
            },
            (command, workspace) -> new JoernRuntimeResult("joern-test", image(), "joern-cpg/run-1", List.of()),
            (command, runtimeResult) -> new JoernArtifactCollectionResult(List.of(), 0, List.of())
        ));
    }

    private static String image() {
        return JoernCpgAnalysisServiceProperties.DEFAULT_JOERN_RUNTIME_IMAGE_REFERENCE;
    }
}
