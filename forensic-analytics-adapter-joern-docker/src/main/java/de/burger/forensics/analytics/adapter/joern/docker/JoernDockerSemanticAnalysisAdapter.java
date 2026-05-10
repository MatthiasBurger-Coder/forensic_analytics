package de.burger.forensics.analytics.adapter.joern.docker;

import de.burger.forensics.analytics.application.analysis.command.SemanticAnalysisRequest;
import de.burger.forensics.analytics.application.analysis.port.SemanticAnalysisPort;
import de.burger.forensics.analytics.application.analysis.result.SemanticAnalysisResult;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class JoernDockerSemanticAnalysisAdapter implements SemanticAnalysisPort {
    private final JoernDockerSettings settings;
    private final JoernDockerCommandBuilder commandBuilder;
    private final JoernDockerCommandRunner commandRunner;
    private final JoernDockerArtifactCollector artifactCollector;

    public JoernDockerSemanticAnalysisAdapter(
        JoernDockerSettings settings,
        JoernDockerCommandBuilder commandBuilder,
        JoernDockerCommandRunner commandRunner,
        JoernDockerArtifactCollector artifactCollector
    ) {
        this.settings = Objects.requireNonNull(settings, "settings must not be null");
        this.commandBuilder = Objects.requireNonNull(commandBuilder, "commandBuilder must not be null");
        this.commandRunner = Objects.requireNonNull(commandRunner, "commandRunner must not be null");
        this.artifactCollector = Objects.requireNonNull(artifactCollector, "artifactCollector must not be null");
    }

    @Override
    public SemanticAnalysisResult analyze(SemanticAnalysisRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        createOutputDirectory();
        var paths = JoernDockerArtifactPaths.under(settings.outputDirectory());
        var operations = commandBuilder.buildAnalysisOperations(settings, request.repositorySource());
        var version = runVersion(operations);
        runAnalysis(operations.stream().skip(1).toList());
        writeEmptySlicesIfMissing(paths.slices());

        return new SemanticAnalysisResult(
            "joern-docker " + version + " " + settings.image().reference(),
            artifactCollector.collect(paths)
        );
    }

    private String runVersion(List<JoernDockerOperation> operations) {
        var result = run(operations.getFirst());
        if (!result.successful()) {
            return "UNKNOWN";
        }
        var version = result.stdout().isBlank() ? result.stderr() : result.stdout();
        return version.isBlank() ? "UNKNOWN" : version.strip();
    }

    private void runAnalysis(List<JoernDockerOperation> operations) {
        operations.forEach(operation -> requireSuccess(operation, run(operation)));
    }

    private JoernDockerCommandResult run(JoernDockerOperation operation) {
        return Objects.requireNonNull(commandRunner.run(operation.command()), operation.name() + " result must not be null");
    }

    private void requireSuccess(JoernDockerOperation operation, JoernDockerCommandResult result) {
        if (result.successful()) {
            return;
        }
        if (settings.failOnError()) {
            throw new JoernDockerAnalysisException(
                "Joern Docker operation " + operation.name()
                    + " failed with exit code " + result.exitCode()
                    + ". stderr: " + result.stderr()
            );
        }
    }

    private void createOutputDirectory() {
        try {
            Files.createDirectories(settings.outputDirectory());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create Joern Docker output directory.", e);
        }
    }

    private static void writeEmptySlicesIfMissing(Path slices) {
        if (Files.exists(slices)) {
            return;
        }
        try {
            Files.writeString(slices, "{\"anchors\":[]}", StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create empty Joern slices artifact " + slices + ".", e);
        }
    }
}
