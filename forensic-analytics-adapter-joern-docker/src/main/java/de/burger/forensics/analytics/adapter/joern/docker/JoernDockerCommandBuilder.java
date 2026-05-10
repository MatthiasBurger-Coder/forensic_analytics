package de.burger.forensics.analytics.adapter.joern.docker;

import de.burger.forensics.analytics.domain.repository.RepositorySource;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class JoernDockerCommandBuilder {
    private static final String CONTAINER_OUTPUT = "/workspace/output";

    public List<JoernDockerOperation> buildAnalysisOperations(
        JoernDockerSettings settings,
        RepositorySource source
    ) {
        Objects.requireNonNull(settings, "settings must not be null");
        Objects.requireNonNull(source, "source must not be null");
        var sourceRoots = source.sourceRoots();
        if (sourceRoots.isEmpty()) {
            throw new IllegalArgumentException("source roots must not be empty");
        }

        var artifacts = JoernDockerArtifactPaths.under(settings.outputDirectory());
        return List.of(
            operation("joern-version", settings, List.of("joern", "--version"), List.of()),
            operation("joern-parse", settings, parseArguments(sourceRoots), parseVolumes(settings, sourceRoots)),
            operation("joern-callgraph", settings, exportArguments("callgraph", artifacts.cpg(), artifacts.callgraph()), outputVolume(settings)),
            operation("joern-controlflow", settings, exportArguments("controlflow", artifacts.cpg(), artifacts.controlflow()), outputVolume(settings)),
            operation("joern-slice", settings, sliceArguments(artifacts.cpg(), artifacts.dataflow()), outputVolume(settings))
        );
    }

    private static JoernDockerOperation operation(
        String name,
        JoernDockerSettings settings,
        List<String> containerArguments,
        List<String> volumes
    ) {
        var arguments = new ArrayList<String>();
        arguments.add(settings.dockerExecutable());
        arguments.add("run");
        arguments.add("--rm");
        arguments.add("--network");
        arguments.add("none");
        volumes.forEach(volume -> {
            arguments.add("--volume");
            arguments.add(volume);
        });
        arguments.add(settings.image().reference());
        arguments.addAll(containerArguments);
        return new JoernDockerOperation(
            name,
            new JoernDockerCommand(arguments, settings.timeout(), settings.outputDirectory())
        );
    }

    private static List<String> parseArguments(List<String> sourceRoots) {
        var arguments = new ArrayList<String>();
        arguments.add("joern-parse");
        arguments.add("--output");
        arguments.add(CONTAINER_OUTPUT + "/cpg.bin");
        for (var index = 0; index < sourceRoots.size(); index++) {
            arguments.add("/workspace/source" + index);
        }
        return arguments;
    }

    private static List<String> exportArguments(String kind, Path cpg, Path output) {
        return List.of(
            "joern",
            "--script",
            kind + ".sc",
            "--params",
            "cpg=" + containerOutputPath(cpg) + ",out=" + containerOutputPath(output)
        );
    }

    private static List<String> sliceArguments(Path cpg, Path dataflow) {
        return List.of(
            "joern-slice",
            "data-flow",
            "--out",
            containerOutputPath(dataflow),
            containerOutputPath(cpg)
        );
    }

    private static String containerOutputPath(Path artifact) {
        return CONTAINER_OUTPUT + "/" + artifact.getFileName();
    }

    private static List<String> outputVolume(JoernDockerSettings settings) {
        return List.of(settings.outputDirectory() + ":" + CONTAINER_OUTPUT);
    }

    private static List<String> parseVolumes(JoernDockerSettings settings, List<String> sourceRoots) {
        var volumes = new ArrayList<String>();
        volumes.addAll(outputVolume(settings));
        volumes.addAll(sourceRootVolumes(sourceRoots));
        return volumes;
    }

    private static List<String> sourceRootVolumes(List<String> sourceRoots) {
        var volumes = new ArrayList<String>();
        volumes.addAll(sourceRoots.stream()
            .map(Path::of)
            .map(path -> path.toAbsolutePath().normalize())
            .map(Path::toString)
            .map(new SourceRootVolume()::format)
            .toList());
        return volumes;
    }

    private static final class SourceRootVolume {
        private int index;

        private String format(String sourceRoot) {
            return sourceRoot + ":/workspace/source" + index++ + ":ro";
        }
    }
}
