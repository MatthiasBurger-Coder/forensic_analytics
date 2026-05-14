package de.burger.forensics.analytics.adapter.joern.docker;

import de.burger.forensics.analytics.application.analysis.command.SemanticAnalysisRequest;
import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.repository.RepositoryMetadata;
import de.burger.forensics.analytics.domain.repository.RepositorySource;
import de.burger.forensics.analytics.domain.source.SourceFact;
import de.burger.forensics.analytics.domain.source.SourceLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JoernDockerSemanticAnalysisAdapterTest {
    @TempDir
    Path tempDir;

    @Test
    void runsDockerOperationsAndCollectsArtifacts() throws Exception {
        var runner = new RecordingRunner(tempDir.resolve("joern"));
        var adapter = adapter(true, runner);

        var result = adapter.analyze(request());

        assertEquals(5, runner.commands.size());
        assertTrue(result.providerName().contains("joern-docker joern 1.2.3"));
        assertEquals(
            List.of("joern-cpg", "joern-callgraph", "joern-controlflow", "joern-dataflow", "joern-slices"),
            result.artifacts().stream().map(artifact -> artifact.type()).toList()
        );
        assertTrue(result.semanticFingerprint().startsWith("sha256:"));
        assertEquals(1, result.semanticGraph().nodes().size());
        assertEquals(1, result.semanticGraph().edges().size());
        assertEquals(1, result.semanticGraph().methods().size());
        assertEquals(1, result.semanticGraph().callRelations().size());
        assertEquals(1, result.semanticGraph().controlFlowRelations().size());
        assertEquals(1, result.semanticGraph().dataFlowPaths().size());
        assertEquals(1, result.semanticGraph().anchors().size());
    }

    @Test
    void failedVersionUsesUnknownProviderVersion() throws Exception {
        var runner = new RecordingRunner(tempDir.resolve("joern"));
        runner.failVersion = true;

        var result = adapter(true, runner).analyze(request());

        assertTrue(result.providerName().contains("UNKNOWN"));
        assertTrue(result.semanticFingerprint().startsWith("sha256:"));
    }

    @Test
    void versionCanBeReadFromStderrWhenStdoutIsBlank() throws Exception {
        var runner = new RecordingRunner(tempDir.resolve("joern"));
        runner.versionOnStderr = true;

        var result = adapter(true, runner).analyze(request());

        assertTrue(result.providerName().contains("joern-stderr 2.0.0"));
    }

    @Test
    void blankVersionUsesUnknownProviderVersion() throws Exception {
        var runner = new RecordingRunner(tempDir.resolve("joern"));
        runner.blankVersion = true;

        var result = adapter(true, runner).analyze(request());

        assertTrue(result.providerName().contains("UNKNOWN"));
    }

    @Test
    void createsEmptySlicesArtifactWhenJoernSliceDoesNotProduceOne() throws Exception {
        var runner = new RecordingRunner(tempDir.resolve("joern"));
        runner.skipSlices = true;

        var result = adapter(true, runner).analyze(request());

        assertEquals(0, result.semanticGraph().anchors().size());
        assertTrue(Files.readString(tempDir.resolve("joern").resolve("slices.json"), StandardCharsets.UTF_8).contains("\"anchors\":[]"));
    }

    @Test
    void failedAnalysisCommandThrowsWhenConfigured() throws Exception {
        var runner = new RecordingRunner(tempDir.resolve("joern"));
        runner.failParse = true;

        assertThrows(JoernDockerAnalysisException.class, () -> adapter(true, runner).analyze(request()));
    }

    @Test
    void failedAnalysisCommandCanBeToleratedWhenConfigured() throws Exception {
        var runner = new RecordingRunner(tempDir.resolve("joern"));
        runner.failParse = true;

        var result = adapter(false, runner).analyze(request());

        assertTrue(result.providerName().contains("joern 1.2.3"));
        assertEquals(1, result.semanticGraph().nodes().size());
    }

    @Test
    void constructorAndRequestAreRequired() {
        var settings = settings(true);
        var builder = new JoernDockerCommandBuilder();
        var runner = new RecordingRunner(tempDir.resolve("joern"));
        var collector = new FileSystemJoernDockerArtifactCollector();

        assertThrows(NullPointerException.class, () -> new JoernDockerSemanticAnalysisAdapter(null, builder, runner, collector));
        assertThrows(NullPointerException.class, () -> new JoernDockerSemanticAnalysisAdapter(settings, null, runner, collector));
        assertThrows(NullPointerException.class, () -> new JoernDockerSemanticAnalysisAdapter(settings, builder, null, collector));
        assertThrows(NullPointerException.class, () -> new JoernDockerSemanticAnalysisAdapter(settings, builder, runner, null));
        assertThrows(NullPointerException.class, () -> adapter(true, runner).analyze(null));
    }

    private JoernDockerSemanticAnalysisAdapter adapter(boolean failOnError, RecordingRunner runner) {
        return new JoernDockerSemanticAnalysisAdapter(
            settings(failOnError),
            new JoernDockerCommandBuilder(),
            runner,
            new FileSystemJoernDockerArtifactCollector()
        );
    }

    private JoernDockerSettings settings(boolean failOnError) {
        return new JoernDockerSettings(
            "docker",
            new JoernDockerImage("ghcr.io/joernio/joern@sha256:" + "a".repeat(64)),
            tempDir.resolve("joern"),
            Duration.ofSeconds(30),
            failOnError
        );
    }

    private SemanticAnalysisRequest request() throws Exception {
        var sourceRoot = Files.createDirectories(tempDir.resolve("src"));
        return new SemanticAnalysisRequest(
            new AnalysisRunId("analysis-1"),
            new RepositorySource(
                new RepositoryMetadata("project-a", "file:///workspace/project", "main", "abcdef"),
                List.of(sourceRoot.toString())
            ),
            List.of(new SourceFact(
                "type",
                new SourceLocation("src/main/java/App.java", "com.example.App", "main", 1),
                "App",
                "class App"
            ))
        );
    }

    private static final class RecordingRunner implements JoernDockerCommandRunner {
        private final List<List<String>> commands = new ArrayList<>();
        private final Path outputDirectory;
        private boolean failVersion;
        private boolean failParse;
        private boolean versionOnStderr;
        private boolean blankVersion;
        private boolean skipSlices;

        private RecordingRunner(Path outputDirectory) {
            this.outputDirectory = outputDirectory;
        }

        @Override
        public JoernDockerCommandResult run(JoernDockerCommand command) {
            commands.add(command.arguments());
            try {
                Files.createDirectories(outputDirectory);
                if (command.arguments().contains("--version")) {
                    if (failVersion) {
                        return new JoernDockerCommandResult(1, "", "version failed");
                    }
                    if (versionOnStderr) {
                        return new JoernDockerCommandResult(0, "", "joern-stderr 2.0.0");
                    }
                    if (blankVersion) {
                        return new JoernDockerCommandResult(0, "", "");
                    }
                    return new JoernDockerCommandResult(0, "joern 1.2.3", "");
                }
                if (command.arguments().contains("joern-parse")) {
                    Files.writeString(outputDirectory.resolve("cpg.bin"), "cpg", StandardCharsets.UTF_8);
                    return failParse
                        ? new JoernDockerCommandResult(1, "", "parse failed")
                        : new JoernDockerCommandResult(0, "", "");
                }
                if (command.arguments().contains("callgraph.sc")) {
                    Files.writeString(outputDirectory.resolve("callgraph.json"), callgraph(), StandardCharsets.UTF_8);
                    return new JoernDockerCommandResult(0, "", "");
                }
                if (command.arguments().contains("controlflow.sc")) {
                    Files.writeString(outputDirectory.resolve("controlflow.json"), controlflow(), StandardCharsets.UTF_8);
                    return new JoernDockerCommandResult(0, "", "");
                }
                if (command.arguments().contains("joern-slice")) {
                    Files.writeString(outputDirectory.resolve("dataflow.json"), dataflow(), StandardCharsets.UTF_8);
                    if (!skipSlices) {
                        Files.writeString(outputDirectory.resolve("slices.json"), slices(), StandardCharsets.UTF_8);
                    }
                    return new JoernDockerCommandResult(0, "", "");
                }
                return new JoernDockerCommandResult(1, "", "unexpected command");
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
        }

        private static String callgraph() {
            return """
                {
                  "nodes": [
                    {
                      "id": "node-1",
                      "type": "CALL",
                      "file": "Demo.java",
                      "fqcn": "demo.Demo",
                      "method": "run",
                      "signature": "void run()",
                      "line": 12,
                      "code": "call()"
                    }
                  ],
                  "edges": [
                    {
                      "id": "edge-1",
                      "source": "node-1",
                      "target": "node-2",
                      "type": "CALL"
                    }
                  ],
                  "methods": [
                    {
                      "id": "method-1",
                      "file": "Demo.java",
                      "fqcn": "demo.Demo",
                      "name": "run",
                      "signature": "void run()",
                      "line": 12
                    }
                  ],
                  "calls": [
                    {
                      "caller": "method-1",
                      "callee": "method-2",
                      "node": "node-1"
                    }
                  ]
                }
                """;
        }

        private static String controlflow() {
            return """
                {
                  "relations": [
                    {
                      "source": "node-1",
                      "target": "node-2",
                      "type": "NEXT"
                    }
                  ]
                }
                """;
        }

        private static String dataflow() {
            return """
                {
                  "paths": [
                    {
                      "id": "path-1",
                      "source": "node-1",
                      "target": "node-2",
                      "steps": [
                        {
                          "node": "node-1",
                          "order": 0,
                          "kind": "SOURCE"
                        }
                      ]
                    }
                  ]
                }
                """;
        }

        private static String slices() {
            return """
                {
                  "anchors": [
                    {
                      "scanEventKey": "demo.Demo#run:12:METHOD_ENTER",
                      "node": "node-1",
                      "file": "Demo.java",
                      "fqcn": "demo.Demo",
                      "method": "run",
                      "signature": "void run()",
                      "line": 12,
                      "code": "call()",
                      "confidence": 0.95,
                      "strategy": "FQCN_METHOD_LINE_CODE"
                    }
                  ]
                }
                """;
        }
    }
}
