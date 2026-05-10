package de.burger.forensics.analytics.adapter.joern.docker;

import de.burger.forensics.analytics.domain.artifact.ArtifactReference;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

public final class FileSystemJoernDockerArtifactCollector implements JoernDockerArtifactCollector {
    @Override
    public List<ArtifactReference> collect(JoernDockerArtifactPaths paths) {
        return paths.all().stream()
            .filter(Files::isRegularFile)
            .map(path -> artifact(paths.outputDirectory(), path))
            .toList();
    }

    private static ArtifactReference artifact(Path outputDirectory, Path artifact) {
        try {
            return new ArtifactReference(
                relativePath(outputDirectory, artifact),
                artifactType(artifact),
                sha256(artifact),
                Files.size(artifact)
            );
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to collect Joern artifact " + artifact + ".", e);
        }
    }

    private static String relativePath(Path outputDirectory, Path artifact) {
        return outputDirectory.relativize(artifact).toString().replace('\\', '/');
    }

    private static String artifactType(Path artifact) {
        return switch (artifact.getFileName().toString()) {
            case "cpg.bin" -> "joern-cpg";
            case "callgraph.json" -> "joern-callgraph";
            case "controlflow.json" -> "joern-controlflow";
            case "dataflow.json" -> "joern-dataflow";
            case "slices.json" -> "joern-slices";
            default -> "joern-artifact";
        };
    }

    private static String sha256(Path artifact) throws IOException {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(Files.readAllBytes(artifact)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available.", e);
        }
    }
}
