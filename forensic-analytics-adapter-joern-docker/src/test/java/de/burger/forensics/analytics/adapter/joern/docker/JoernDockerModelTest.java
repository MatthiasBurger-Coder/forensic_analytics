package de.burger.forensics.analytics.adapter.joern.docker;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JoernDockerModelTest {
    @Test
    void dockerImageMustBePinnedByDigest() {
        assertEquals(pinnedImageReference(), new JoernDockerImage(pinnedImageReference()).reference());
        assertThrows(IllegalArgumentException.class, () -> new JoernDockerImage(null));
        assertThrows(IllegalArgumentException.class, () -> new JoernDockerImage(" "));
        assertThrows(IllegalArgumentException.class, () -> new JoernDockerImage("ghcr.io/joernio/joern:latest"));
        assertThrows(IllegalArgumentException.class, () -> new JoernDockerImage("ghcr.io/joernio/joern@sha256:"));
        assertThrows(IllegalArgumentException.class, () -> new JoernDockerImage("ghcr.io/joernio/joern@sha256:" + "z".repeat(64)));
    }

    @Test
    void settingsRequireValidValues() {
        var image = new JoernDockerImage(pinnedImageReference());
        var output = Path.of("build/joern").toAbsolutePath().normalize();

        var settings = new JoernDockerSettings("docker", image, output, Duration.ofSeconds(30), true);

        assertEquals("docker", settings.dockerExecutable());
        assertEquals(image, settings.image());
        assertEquals(output, settings.outputDirectory());
        assertThrows(IllegalArgumentException.class, () -> new JoernDockerSettings(null, image, output, Duration.ofSeconds(30), true));
        assertThrows(IllegalArgumentException.class, () -> new JoernDockerSettings("", image, output, Duration.ofSeconds(30), true));
        assertThrows(NullPointerException.class, () -> new JoernDockerSettings("docker", null, output, Duration.ofSeconds(30), true));
        assertThrows(NullPointerException.class, () -> new JoernDockerSettings("docker", image, null, Duration.ofSeconds(30), true));
        assertThrows(NullPointerException.class, () -> new JoernDockerSettings("docker", image, output, null, true));
        assertThrows(IllegalArgumentException.class, () -> new JoernDockerSettings("docker", image, output, Duration.ZERO, true));
        assertThrows(IllegalArgumentException.class, () -> new JoernDockerSettings("docker", image, output, Duration.ofSeconds(-1), true));
    }

    @Test
    void commandAndOperationModelsDefensivelyCopyAndValidateInputs() {
        var command = new JoernDockerCommand(List.of("docker", "run"), Duration.ofSeconds(1), Path.of("."));
        var operation = new JoernDockerOperation("joern-version", command);

        assertEquals(List.of("docker", "run"), command.arguments());
        assertEquals(command, operation.command());
        assertThrows(NullPointerException.class, () -> new JoernDockerCommand(null, Duration.ofSeconds(1), Path.of(".")));
        assertThrows(NullPointerException.class, () -> new JoernDockerCommand(List.of("docker"), null, Path.of(".")));
        assertThrows(NullPointerException.class, () -> new JoernDockerCommand(List.of("docker"), Duration.ofSeconds(1), null));
        assertThrows(IllegalArgumentException.class, () -> new JoernDockerOperation(null, command));
        assertThrows(IllegalArgumentException.class, () -> new JoernDockerOperation("", command));
        assertThrows(NullPointerException.class, () -> new JoernDockerOperation("joern-version", null));
    }

    @Test
    void commandResultNormalizesOutputAndExposesSuccess() {
        var failed = new JoernDockerCommandResult(1, null, null);

        assertEquals("", failed.stdout());
        assertEquals("", failed.stderr());
        assertFalse(failed.successful());
    }

    @Test
    void artifactPathsRequireOutputDirectory() {
        assertThrows(NullPointerException.class, () -> JoernDockerArtifactPaths.under(null));
        assertThrows(NullPointerException.class, () -> new JoernDockerArtifactPaths(null, Path.of("cpg"), Path.of("callgraph"), Path.of("controlflow"), Path.of("dataflow"), Path.of("slices")));
        assertThrows(NullPointerException.class, () -> new JoernDockerArtifactPaths(Path.of("."), null, Path.of("callgraph"), Path.of("controlflow"), Path.of("dataflow"), Path.of("slices")));
    }

    private static String pinnedImageReference() {
        return "ghcr.io/joernio/joern@sha256:" + "a".repeat(64);
    }
}
