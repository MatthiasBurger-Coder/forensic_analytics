package de.burger.forensics.analytics.adapter.joern.docker;

import de.burger.forensics.analytics.domain.repository.RepositoryMetadata;
import de.burger.forensics.analytics.domain.repository.RepositorySource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JoernDockerCommandBuilderTest {
    @TempDir
    Path tempDir;

    private final JoernDockerCommandBuilder builder = new JoernDockerCommandBuilder();

    @Test
    void buildsVersionParseExportAndSliceOperations() throws Exception {
        var sourceRoot = Files.createDirectories(tempDir.resolve("src"));
        var settings = settings();

        var operations = builder.buildAnalysisOperations(
            settings,
            new RepositorySource(repositoryMetadata(), List.of(sourceRoot.toString()))
        );

        assertEquals(
            List.of("joern-version", "joern-parse", "joern-callgraph", "joern-controlflow", "joern-slice"),
            operations.stream().map(JoernDockerOperation::name).toList()
        );
        assertEquals(List.of("docker", "run", "--rm", "--network", "none", image().reference(), "joern", "--version"), operations.getFirst().command().arguments());

        var parse = operations.get(1).command().arguments();
        assertTrue(parse.contains(settings.outputDirectory() + ":/workspace/output"));
        assertTrue(parse.contains(sourceRoot.toAbsolutePath().normalize() + ":/workspace/source0:ro"));
        assertTrue(parse.contains("joern-parse"));
        assertTrue(parse.contains("/workspace/output/cpg.bin"));
        assertTrue(parse.contains("/workspace/source0"));

        var callgraph = operations.get(2).command().arguments();
        assertTrue(callgraph.contains("callgraph.sc"));
        assertTrue(callgraph.contains("cpg=/workspace/output/cpg.bin,out=/workspace/output/callgraph.json"));

        var controlflow = operations.get(3).command().arguments();
        assertTrue(controlflow.contains("controlflow.sc"));
        assertTrue(controlflow.contains("cpg=/workspace/output/cpg.bin,out=/workspace/output/controlflow.json"));

        var slice = operations.get(4).command().arguments();
        assertTrue(slice.contains("joern-slice"));
        assertTrue(slice.contains("/workspace/output/dataflow.json"));
        assertTrue(slice.contains("/workspace/output/cpg.bin"));
    }

    @Test
    void requiresSourceRoots() {
        assertThrows(
            IllegalArgumentException.class,
            () -> builder.buildAnalysisOperations(settings(), new RepositorySource(repositoryMetadata(), List.of()))
        );
    }

    @Test
    void requiresInputs() {
        assertThrows(NullPointerException.class, () -> builder.buildAnalysisOperations(null, repositorySource()));
        assertThrows(NullPointerException.class, () -> builder.buildAnalysisOperations(settings(), null));
    }

    private JoernDockerSettings settings() {
        return new JoernDockerSettings("docker", image(), tempDir.resolve("joern"), Duration.ofSeconds(30), true);
    }

    private static JoernDockerImage image() {
        return new JoernDockerImage("ghcr.io/joernio/joern@sha256:" + "a".repeat(64));
    }

    private static RepositorySource repositorySource() {
        return new RepositorySource(repositoryMetadata(), List.of("src/main/java"));
    }

    private static RepositoryMetadata repositoryMetadata() {
        return new RepositoryMetadata("project-a", "file:///workspace/project", "main", "abcdef");
    }
}
