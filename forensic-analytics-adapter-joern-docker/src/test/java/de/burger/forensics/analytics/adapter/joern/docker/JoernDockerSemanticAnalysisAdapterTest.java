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
    }

    @Test
    void failedVersionUsesUnknownProviderVersion() throws Exception {
        var runner = new RecordingRunner(tempDir.resolve("joern"));
        runner.failVersion = true;

        var result = adapter(true, runner).analyze(request());

        assertTrue(result.providerName().contains("UNKNOWN"));
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

        private RecordingRunner(Path outputDirectory) {
            this.outputDirectory = outputDirectory;
        }

        @Override
        public JoernDockerCommandResult run(JoernDockerCommand command) {
            commands.add(command.arguments());
            try {
                Files.createDirectories(outputDirectory);
                if (command.arguments().contains("--version")) {
                    return failVersion
                        ? new JoernDockerCommandResult(1, "", "version failed")
                        : new JoernDockerCommandResult(0, "joern 1.2.3", "");
                }
                if (command.arguments().contains("joern-parse")) {
                    Files.writeString(outputDirectory.resolve("cpg.bin"), "cpg", StandardCharsets.UTF_8);
                    return failParse
                        ? new JoernDockerCommandResult(1, "", "parse failed")
                        : new JoernDockerCommandResult(0, "", "");
                }
                if (command.arguments().contains("callgraph.sc")) {
                    Files.writeString(outputDirectory.resolve("callgraph.json"), "callgraph", StandardCharsets.UTF_8);
                    return new JoernDockerCommandResult(0, "", "");
                }
                if (command.arguments().contains("controlflow.sc")) {
                    Files.writeString(outputDirectory.resolve("controlflow.json"), "controlflow", StandardCharsets.UTF_8);
                    return new JoernDockerCommandResult(0, "", "");
                }
                if (command.arguments().contains("joern-slice")) {
                    Files.writeString(outputDirectory.resolve("dataflow.json"), "dataflow", StandardCharsets.UTF_8);
                    return new JoernDockerCommandResult(0, "", "");
                }
                return new JoernDockerCommandResult(1, "", "unexpected command");
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
