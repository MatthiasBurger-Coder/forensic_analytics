package de.burger.forensics.analytics.adapter.joern.docker;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record JoernDockerArtifactPaths(
    Path outputDirectory,
    Path cpg,
    Path callgraph,
    Path controlflow,
    Path dataflow,
    Path slices
) {
    public JoernDockerArtifactPaths {
        outputDirectory = Objects.requireNonNull(outputDirectory, "outputDirectory must not be null")
            .toAbsolutePath()
            .normalize();
        Objects.requireNonNull(cpg, "cpg must not be null");
        Objects.requireNonNull(callgraph, "callgraph must not be null");
        Objects.requireNonNull(controlflow, "controlflow must not be null");
        Objects.requireNonNull(dataflow, "dataflow must not be null");
        Objects.requireNonNull(slices, "slices must not be null");
    }

    public static JoernDockerArtifactPaths under(Path outputDirectory) {
        var normalized = Objects.requireNonNull(outputDirectory, "outputDirectory must not be null")
            .toAbsolutePath()
            .normalize();
        return new JoernDockerArtifactPaths(
            normalized,
            normalized.resolve("cpg.bin"),
            normalized.resolve("callgraph.json"),
            normalized.resolve("controlflow.json"),
            normalized.resolve("dataflow.json"),
            normalized.resolve("slices.json")
        );
    }

    public List<Path> all() {
        return List.of(cpg, callgraph, controlflow, dataflow, slices);
    }
}
