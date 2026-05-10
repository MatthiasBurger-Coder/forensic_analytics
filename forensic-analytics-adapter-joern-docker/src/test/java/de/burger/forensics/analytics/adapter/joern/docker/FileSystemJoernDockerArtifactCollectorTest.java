package de.burger.forensics.analytics.adapter.joern.docker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileSystemJoernDockerArtifactCollectorTest {
    @TempDir
    Path tempDir;

    private final FileSystemJoernDockerArtifactCollector collector = new FileSystemJoernDockerArtifactCollector();

    @Test
    void collectsExistingStandardArtifacts() throws Exception {
        var paths = JoernDockerArtifactPaths.under(tempDir);
        Files.writeString(paths.cpg(), "cpg", StandardCharsets.UTF_8);
        Files.writeString(paths.callgraph(), "callgraph", StandardCharsets.UTF_8);
        Files.writeString(paths.controlflow(), "controlflow", StandardCharsets.UTF_8);
        Files.writeString(paths.dataflow(), "dataflow", StandardCharsets.UTF_8);
        Files.writeString(paths.slices(), "slices", StandardCharsets.UTF_8);

        var artifacts = collector.collect(paths);

        assertEquals(
            List.of("joern-cpg", "joern-callgraph", "joern-controlflow", "joern-dataflow", "joern-slices"),
            artifacts.stream().map(artifact -> artifact.type()).toList()
        );
        assertTrue(artifacts.stream().allMatch(artifact -> artifact.sha256().length() == 64));
        assertEquals(List.of(3L, 9L, 11L, 8L, 6L), artifacts.stream().map(artifact -> artifact.sizeBytes()).toList());
    }

    @Test
    void ignoresMissingArtifactsAndUsesGenericTypeForUnknownNames() throws Exception {
        var unknown = tempDir.resolve("unknown.bin");
        Files.writeString(unknown, "artifact", StandardCharsets.UTF_8);
        var paths = new JoernDockerArtifactPaths(
            tempDir,
            unknown,
            tempDir.resolve("missing-callgraph.json"),
            tempDir.resolve("missing-controlflow.json"),
            tempDir.resolve("missing-dataflow.json"),
            tempDir.resolve("missing-slices.json")
        );

        var artifacts = collector.collect(paths);

        assertEquals(1, artifacts.size());
        assertEquals("unknown.bin", artifacts.getFirst().path());
        assertEquals("joern-artifact", artifacts.getFirst().type());
    }
}
