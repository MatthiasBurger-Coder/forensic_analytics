package de.burger.forensics.analytics.testbed;

import de.burger.forensics.analytics.adapter.joern.docker.FileSystemJoernDockerArtifactCollector;
import de.burger.forensics.analytics.adapter.joern.docker.JoernDockerCommandBuilder;
import de.burger.forensics.analytics.adapter.joern.docker.JoernDockerCommandResult;
import de.burger.forensics.analytics.adapter.joern.docker.JoernDockerCommandRunner;
import de.burger.forensics.analytics.adapter.joern.docker.JoernDockerImage;
import de.burger.forensics.analytics.adapter.joern.docker.JoernDockerSemanticAnalysisAdapter;
import de.burger.forensics.analytics.adapter.joern.docker.JoernDockerSettings;
import de.burger.forensics.analytics.adapter.joern.docker.ProcessJoernDockerCommandRunner;
import de.burger.forensics.analytics.adapter.repository.source.LocalRepositorySourceAdapter;
import de.burger.forensics.analytics.application.analysis.DefaultRunRepositoryAnalysisUseCase;
import de.burger.forensics.analytics.application.analysis.command.RuleGenerationRequest;
import de.burger.forensics.analytics.application.analysis.command.SemanticAnalysisRequest;
import de.burger.forensics.analytics.application.analysis.port.RepositoryAnalysisResultStore;
import de.burger.forensics.analytics.application.analysis.port.RuleGenerationPort;
import de.burger.forensics.analytics.application.analysis.port.SemanticAnalysisPort;
import de.burger.forensics.analytics.application.analysis.port.SourceScannerPort;
import de.burger.forensics.analytics.application.analysis.result.RepositoryAnalysisStatus;
import de.burger.forensics.analytics.application.analysis.result.RuleGenerationResult;
import de.burger.forensics.analytics.application.analysis.result.RunRepositoryAnalysisResult;
import de.burger.forensics.analytics.application.analysis.result.SemanticAnalysisResult;
import de.burger.forensics.analytics.cli.ForensicAnalyticsCli;
import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.artifact.ArtifactReference;
import de.burger.forensics.analytics.domain.repository.RepositoryMetadata;
import de.burger.forensics.analytics.domain.repository.RepositorySource;
import de.burger.forensics.analytics.domain.source.SourceFact;
import de.burger.forensics.analytics.domain.source.SourceLocation;
import de.burger.forensics.analytics.engine.RepositoryAnalysisEngine;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class RepositoryAnalysisTestbedTest {
    private static final String DOCKER_SMOKE_ENABLED = "FORENSIC_ANALYTICS_JOERN_DOCKER_SMOKE";
    private static final String JOERN_IMAGE = "FORENSIC_ANALYTICS_JOERN_IMAGE";
    private static final String DOCKER_EXECUTABLE = "FORENSIC_ANALYTICS_DOCKER_EXECUTABLE";

    @TempDir
    Path tempDir;

    @Test
    void cliRunsLocalRepositoryScenarioWithoutJoern() throws Exception {
        var repository = createSampleRepository("sample-no-joern");
        var outputDirectory = tempDir.resolve("analysis-output");
        var resultStore = new RecordingResultStore();
        var useCase = localUseCase(
            new SemanticAnalysisResult("semantic-disabled", List.of()),
            outputDirectory.resolve("artifacts"),
            resultStore
        );
        var engine = new RepositoryAnalysisEngine(useCase);
        var standardOutput = new ByteArrayOutputStream();
        var errorOutput = new ByteArrayOutputStream();

        var exitCode = new ForensicAnalyticsCli(engine::run, stream(standardOutput), stream(errorOutput)).run(new String[] {
            "analyze",
            "--repo", repository.toString(),
            "--profile", "testbed-baseline",
            "--output", outputDirectory.toString(),
            "--joern-mode", "off"
        });

        assertEquals(0, exitCode);
        assertEquals("", errorOutput.toString(StandardCharsets.UTF_8));
        assertTrue(standardOutput.toString(StandardCharsets.UTF_8).contains("summaryPath="));

        var summary = Files.readString(outputDirectory.resolve("analysis-summary.txt"), StandardCharsets.UTF_8);
        assertTrue(summary.contains("projectId=sample-no-joern"));
        assertTrue(summary.contains("profile=testbed-baseline"));
        assertTrue(summary.contains("joernMode=off"));
        assertTrue(summary.contains("status=COMPLETED"));
        assertTrue(summary.contains("sourceFacts=1"));
        assertTrue(summary.contains("semanticProvider=semantic-disabled"));
        assertTrue(summary.contains("semanticArtifacts=0"));
        assertTrue(summary.contains("ruleArtifacts=1"));

        assertEquals(RepositoryAnalysisStatus.COMPLETED, resultStore.result.status());
        assertEquals("sample-no-joern", resultStore.result.repositoryMetadata().projectId());
        assertEquals(1, resultStore.result.sourceFacts().size());
        assertEquals("semantic-disabled", resultStore.result.semanticAnalysis().providerName());
    }

    @Test
    void cliImportsEngineRequestScenario() throws Exception {
        var requestDirectory = Files.createDirectories(tempDir.resolve("engine-request-fixture"));
        var sourceFacts = Files.writeString(
            requestDirectory.resolve("source-facts.json"),
            "{\"facts\":[\"class com.example.App\"]}",
            StandardCharsets.UTF_8
        );
        var rules = Files.writeString(
            requestDirectory.resolve("rules.btm"),
            "RULE testbed\nENDRULE\n",
            StandardCharsets.UTF_8
        );
        var requestFile = Files.writeString(
            requestDirectory.resolve("engine-request.json"),
            engineRequestJson(sourceFacts, rules),
            StandardCharsets.UTF_8
        );
        var outputDirectory = tempDir.resolve("engine-request-output");
        var standardOutput = new ByteArrayOutputStream();
        var errorOutput = new ByteArrayOutputStream();
        var engine = new RepositoryAnalysisEngine(localUseCase(
            new SemanticAnalysisResult("semantic-unused", List.of()),
            outputDirectory.resolve("artifacts"),
            new RecordingResultStore()
        ));

        var exitCode = new ForensicAnalyticsCli(engine::run, stream(standardOutput), stream(errorOutput)).run(new String[] {
            "ingest-request",
            "--request", requestFile.toString(),
            "--output", outputDirectory.toString()
        });

        assertEquals(0, exitCode);
        assertEquals("", errorOutput.toString(StandardCharsets.UTF_8));
        assertTrue(standardOutput.toString(StandardCharsets.UTF_8).contains("summaryPath="));

        var summary = Files.readString(outputDirectory.resolve("engine-request-import-summary.txt"), StandardCharsets.UTF_8);
        assertTrue(summary.contains("requestFile=" + requestFile.toAbsolutePath().normalize()));
        assertTrue(summary.contains("status=COMPLETED"));
        assertTrue(summary.contains("uploadedPayloads=2"));
    }

    @Test
    void engineRunsRepositoryScenarioThroughJoernAdapterBoundary() throws Exception {
        var repository = createSampleRepository("sample-with-joern");
        var outputDirectory = tempDir.resolve("joern-analysis-output");
        var resultStore = new RecordingResultStore();
        var semanticPort = new JoernDockerSemanticAnalysisAdapter(
            joernSettings(outputDirectory.resolve("joern")),
            new JoernDockerCommandBuilder(),
            new TestbedJoernCommandRunner(outputDirectory.resolve("joern")),
            new FileSystemJoernDockerArtifactCollector()
        );
        var useCase = localUseCase(semanticPort, outputDirectory.resolve("artifacts"), resultStore);
        var engine = new RepositoryAnalysisEngine(useCase);

        var result = engine.run(new de.burger.forensics.analytics.application.analysis.command.RunRepositoryAnalysisCommand(
            AnalysisRunId.deterministic("sample-with-joern|testbed"),
            new RepositoryMetadata("sample-with-joern", repository.toString(), "UNKNOWN", "UNKNOWN"),
            "testbed-joern"
        ));

        assertEquals(RepositoryAnalysisStatus.COMPLETED, result.status());
        assertEquals("sample-with-joern", result.repositoryMetadata().projectId());
        assertEquals("testbed-joern", result.analysisProfile());
        assertEquals(1, result.sourceFacts().size());
        assertTrue(result.semanticAnalysis().providerName().contains("joern-docker joern-testbed 1.0.0"));
        assertIterableEquals(
            List.of("joern-cpg", "joern-callgraph", "joern-controlflow", "joern-dataflow", "joern-slices"),
            result.semanticAnalysis().artifacts().stream().map(ArtifactReference::type).toList()
        );
        assertEquals(1, result.ruleGeneration().artifacts().size());
        assertEquals(result, resultStore.result);
    }

    @Test
    @Tag("docker")
    void runsPinnedJoernContainerVersionSmokeWhenExplicitlyEnabled() throws Exception {
        assumeTrue(
            "true".equalsIgnoreCase(System.getenv(DOCKER_SMOKE_ENABLED)),
            "Set " + DOCKER_SMOKE_ENABLED + "=true to run the external Joern Docker smoke scenario."
        );
        var imageReference = Optional.ofNullable(System.getenv(JOERN_IMAGE))
            .filter(value -> !value.isBlank())
            .orElse("");
        assumeTrue(!imageReference.isBlank(), "Set " + JOERN_IMAGE + " to a sha256-pinned Joern image reference.");
        var dockerExecutable = Optional.ofNullable(System.getenv(DOCKER_EXECUTABLE))
            .filter(value -> !value.isBlank())
            .orElse("docker");
        var repository = createSampleRepository("sample-joern-docker-smoke");
        var settings = new JoernDockerSettings(
            dockerExecutable,
            new JoernDockerImage(imageReference),
            tempDir.resolve("joern-docker-smoke"),
            Duration.ofMinutes(2),
            true
        );
        var source = new RepositorySource(
            new RepositoryMetadata("sample-joern-docker-smoke", repository.toString(), "UNKNOWN", "UNKNOWN"),
            List.of(repository.resolve("src/main/java").toString())
        );
        var versionOperation = new JoernDockerCommandBuilder()
            .buildAnalysisOperations(settings, source)
            .getFirst();

        var result = new ProcessJoernDockerCommandRunner().run(versionOperation.command());

        assertEquals(0, result.exitCode(), result.stderr());
        assertFalse((result.stdout() + result.stderr()).isBlank());
    }

    private DefaultRunRepositoryAnalysisUseCase localUseCase(
        SemanticAnalysisResult semanticResult,
        Path artifactDirectory,
        RecordingResultStore resultStore
    ) {
        return localUseCase(request -> semanticResult, artifactDirectory, resultStore);
    }

    private DefaultRunRepositoryAnalysisUseCase localUseCase(
        SemanticAnalysisPort semanticAnalysisPort,
        Path artifactDirectory,
        RecordingResultStore resultStore
    ) {
        return new DefaultRunRepositoryAnalysisUseCase(
            new LocalRepositorySourceAdapter(tempDir),
            new FixtureSourceScanner(),
            semanticAnalysisPort,
            new FixtureRuleGenerationPort(artifactDirectory.resolve("rules")),
            resultStore
        );
    }

    private Path createSampleRepository(String name) throws IOException {
        var sourceRoot = Files.createDirectories(tempDir.resolve(name).resolve("src/main/java/com/example"));
        Files.writeString(
            sourceRoot.resolve("App.java"),
            """
            package com.example;

            final class App {
                String greet(String name) {
                    return "hello " + name;
                }
            }
            """,
            StandardCharsets.UTF_8
        );
        return tempDir.resolve(name);
    }

    private JoernDockerSettings joernSettings(Path outputDirectory) {
        return new JoernDockerSettings(
            "docker",
            new JoernDockerImage("ghcr.io/joernio/joern@sha256:" + "a".repeat(64)),
            outputDirectory,
            Duration.ofSeconds(30),
            true
        );
    }

    private static PrintStream stream(ByteArrayOutputStream output) {
        return new PrintStream(output, true, StandardCharsets.UTF_8);
    }

    private static String engineRequestJson(Path sourceFacts, Path rules) {
        return """
            {
              "schemaVersion": "1",
              "buildIdentity": {
                "projectId": "testbed-project",
                "repositoryUrl": "UNKNOWN",
                "branchName": "UNKNOWN",
                "commitHash": "UNKNOWN",
                "buildId": "testbed-build",
                "scanTimestamp": "1970-01-01T00:00:00Z"
              },
              "moduleIdentity": {
                "moduleName": "sample-module",
                "modulePath": ":sample-module"
              },
              "pluginIdentity": {
                "pluginName": "forensics-tracing",
                "pluginVersion": "testbed"
              },
              "payloads": [
                {
                  "payloadId": "source-facts",
                  "kind": "SOURCE_FACTS",
                  "contentType": "application/json",
                  "file": "%s",
                  "attributes": {
                    "artifact": "source-facts"
                  }
                },
                {
                  "payloadId": "byteman-rules",
                  "kind": "RULE_ARTIFACTS",
                  "contentType": "text/x-byteman",
                  "file": "%s",
                  "attributes": {
                    "artifact": "btm-rules"
                  }
                }
              ]
            }
            """.formatted(jsonPath(sourceFacts), jsonPath(rules));
    }

    private static String jsonPath(Path path) {
        return path.toAbsolutePath().normalize().toString().replace('\\', '/');
    }

    private static final class FixtureSourceScanner implements SourceScannerPort {
        @Override
        public List<SourceFact> scan(RepositorySource source) {
            Objects.requireNonNull(source, "source must not be null");
            return source.sourceRoots().stream()
                .map(Path::of)
                .flatMap(FixtureSourceScanner::sourceFacts)
                .sorted((left, right) -> left.location().sourcePath().compareTo(right.location().sourcePath()))
                .toList();
        }

        private static java.util.stream.Stream<SourceFact> sourceFacts(Path sourceRoot) {
            try (var paths = Files.walk(sourceRoot)) {
                return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .map(path -> sourceFact(sourceRoot, path))
                    .toList()
                    .stream();
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to scan fixture source root " + sourceRoot + ".", e);
            }
        }

        private static SourceFact sourceFact(Path sourceRoot, Path sourceFile) {
            var relative = sourceRoot.relativize(sourceFile).toString().replace('\\', '/');
            var className = relative.substring(0, relative.length() - ".java".length()).replace('/', '.');
            return new SourceFact(
                "source-file",
                new SourceLocation(relative, className, "UNKNOWN", 1),
                className,
                "source file " + className
            );
        }
    }

    private static final class FixtureRuleGenerationPort implements RuleGenerationPort {
        private final Path outputDirectory;

        private FixtureRuleGenerationPort(Path outputDirectory) {
            this.outputDirectory = Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");
        }

        @Override
        public RuleGenerationResult generate(RuleGenerationRequest request) {
            Objects.requireNonNull(request, "request must not be null");
            try {
                Files.createDirectories(outputDirectory);
                var ruleFile = outputDirectory.resolve("testbed-rules.btm");
                var ruleLines = new ArrayList<String>();
                ruleLines.add("# testbed rules for " + request.analysisRunId().value());
                request.sourceFacts().stream()
                    .map(SourceFact::signature)
                    .sorted()
                    .forEach(signature -> ruleLines.add("# source " + signature));
                Files.writeString(ruleFile, String.join(System.lineSeparator(), ruleLines) + System.lineSeparator(), StandardCharsets.UTF_8);
                return new RuleGenerationResult(List.of(artifact(outputDirectory, ruleFile, "testbed-byteman-rules")));
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to write testbed rule artifact.", e);
            }
        }
    }

    private static final class RecordingResultStore implements RepositoryAnalysisResultStore {
        private RunRepositoryAnalysisResult result;

        @Override
        public void store(RunRepositoryAnalysisResult result) {
            this.result = Objects.requireNonNull(result, "result must not be null");
        }
    }

    private static final class TestbedJoernCommandRunner implements JoernDockerCommandRunner {
        private final Path outputDirectory;

        private TestbedJoernCommandRunner(Path outputDirectory) {
            this.outputDirectory = Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");
        }

        @Override
        public JoernDockerCommandResult run(de.burger.forensics.analytics.adapter.joern.docker.JoernDockerCommand command) {
            Objects.requireNonNull(command, "command must not be null");
            try {
                Files.createDirectories(outputDirectory);
                if (command.arguments().contains("--version")) {
                    return new JoernDockerCommandResult(0, "joern-testbed 1.0.0", "");
                }
                if (command.arguments().contains("joern-parse")) {
                    Files.writeString(outputDirectory.resolve("cpg.bin"), "testbed-cpg", StandardCharsets.UTF_8);
                    return new JoernDockerCommandResult(0, "", "");
                }
                if (command.arguments().contains("callgraph.sc")) {
                    Files.writeString(outputDirectory.resolve("callgraph.json"), "{\"edges\":[]}", StandardCharsets.UTF_8);
                    return new JoernDockerCommandResult(0, "", "");
                }
                if (command.arguments().contains("controlflow.sc")) {
                    Files.writeString(outputDirectory.resolve("controlflow.json"), "{\"blocks\":[]}", StandardCharsets.UTF_8);
                    return new JoernDockerCommandResult(0, "", "");
                }
                if (command.arguments().contains("joern-slice")) {
                    Files.writeString(outputDirectory.resolve("dataflow.json"), "{\"flows\":[]}", StandardCharsets.UTF_8);
                    return new JoernDockerCommandResult(0, "", "");
                }
                return new JoernDockerCommandResult(1, "", "unexpected testbed command");
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to write testbed Joern artifact.", e);
            }
        }
    }

    private static ArtifactReference artifact(Path outputDirectory, Path artifact, String type) throws IOException {
        return new ArtifactReference(
            outputDirectory.relativize(artifact).toString().replace('\\', '/'),
            type,
            sha256(artifact),
            Files.size(artifact)
        );
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
