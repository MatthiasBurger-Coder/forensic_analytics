package de.burger.forensics.analytics.adapter.joern.docker;

import de.burger.forensics.analytics.application.analysis.command.SemanticAnalysisRequest;
import de.burger.forensics.analytics.application.analysis.port.SemanticAnalysisPort;
import de.burger.forensics.analytics.application.analysis.result.SemanticAnalysisResult;
import de.burger.forensics.analytics.domain.artifact.ArtifactReference;
import de.burger.forensics.analytics.observability.OperationLogger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

public final class JoernDockerSemanticAnalysisAdapter implements SemanticAnalysisPort {
    private final JoernDockerSettings settings;
    private final JoernDockerCommandBuilder commandBuilder;
    private final JoernDockerCommandRunner commandRunner;
    private final JoernDockerArtifactCollector artifactCollector;
    private final OperationLogger operationLogger;

    public JoernDockerSemanticAnalysisAdapter(
        JoernDockerSettings settings,
        JoernDockerCommandBuilder commandBuilder,
        JoernDockerCommandRunner commandRunner,
        JoernDockerArtifactCollector artifactCollector
    ) {
        this(
            settings,
            commandBuilder,
            commandRunner,
            artifactCollector,
            OperationLogger.system(JoernDockerSemanticAnalysisAdapter.class)
        );
    }

    JoernDockerSemanticAnalysisAdapter(
        JoernDockerSettings settings,
        JoernDockerCommandBuilder commandBuilder,
        JoernDockerCommandRunner commandRunner,
        JoernDockerArtifactCollector artifactCollector,
        OperationLogger operationLogger
    ) {
        this.settings = Objects.requireNonNull(settings, "settings must not be null");
        this.commandBuilder = Objects.requireNonNull(commandBuilder, "commandBuilder must not be null");
        this.commandRunner = Objects.requireNonNull(commandRunner, "commandRunner must not be null");
        this.artifactCollector = Objects.requireNonNull(artifactCollector, "artifactCollector must not be null");
        this.operationLogger = Objects.requireNonNull(operationLogger, "operationLogger must not be null");
    }

    @Override
    public SemanticAnalysisResult analyze(SemanticAnalysisRequest request) {
        var verifiedRequest = Objects.requireNonNull(request, "request must not be null");
        return operationLogger.logged("adapter.joern-docker.semantic-analysis", () -> analyzeVerified(verifiedRequest));
    }

    private SemanticAnalysisResult analyzeVerified(SemanticAnalysisRequest request) {
        createOutputDirectory();
        var paths = JoernDockerArtifactPaths.under(settings.outputDirectory());
        var operations = commandBuilder.buildAnalysisOperations(settings, request.repositorySource());
        var version = runVersion(operations);
        runAnalysis(operations.stream().skip(1).toList());
        writeEmptySlicesIfMissing(paths.slices());
        var artifacts = artifactCollector.collect(paths);

        return new SemanticAnalysisResult(
            "joern-docker " + version + " " + settings.image().reference(),
            semanticFingerprint(artifacts),
            artifacts,
            new JoernOutputParser().parse(paths)
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

    private static String semanticFingerprint(List<ArtifactReference> artifacts) {
        var fingerprintInput = artifacts.stream()
            .sorted(Comparator.comparing(ArtifactReference::type)
                .thenComparing(ArtifactReference::path))
            .map(artifact -> artifact.type() + "=" + artifact.sha256())
            .reduce((left, right) -> left + "\n" + right)
            .orElse("");
        return "sha256:" + sha256(fingerprintInput);
    }

    private static String sha256(String value) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available.", e);
        }
    }
}
