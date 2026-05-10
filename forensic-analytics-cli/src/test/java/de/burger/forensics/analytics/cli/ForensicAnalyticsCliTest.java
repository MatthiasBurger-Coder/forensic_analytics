package de.burger.forensics.analytics.cli;

import de.burger.forensics.analytics.application.analysis.RunRepositoryAnalysisUseCase;
import de.burger.forensics.analytics.application.analysis.command.RunRepositoryAnalysisCommand;
import de.burger.forensics.analytics.application.analysis.result.RuleGenerationResult;
import de.burger.forensics.analytics.application.analysis.result.RunRepositoryAnalysisResult;
import de.burger.forensics.analytics.application.analysis.result.SemanticAnalysisResult;
import de.burger.forensics.analytics.domain.artifact.ArtifactReference;
import de.burger.forensics.analytics.domain.source.SourceFact;
import de.burger.forensics.analytics.domain.source.SourceLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForensicAnalyticsCliTest {
    @TempDir
    Path tempDir;

    @Test
    void runsUseCaseAndWritesSummary() throws Exception {
        var useCase = new RecordingUseCase();
        var standardOutput = new ByteArrayOutputStream();
        var errorOutput = new ByteArrayOutputStream();
        var outputDirectory = tempDir.resolve("analysis-out");

        var exitCode = new ForensicAnalyticsCli(useCase, stream(standardOutput), stream(errorOutput)).run(new String[] {
            "analyze",
            "--repo", tempDir.resolve("project").toString(),
            "--profile", "baseline",
            "--output", outputDirectory.toString(),
            "--joern-mode", "off"
        });

        assertEquals(0, exitCode);
        assertNotNull(useCase.command);
        assertEquals("project", useCase.command.repositoryMetadata().projectId());
        assertEquals("baseline", useCase.command.analysisProfile());
        assertEquals("UNKNOWN", useCase.command.repositoryMetadata().branchName());
        assertEquals("UNKNOWN", useCase.command.repositoryMetadata().commitHash());
        var summary = Files.readString(outputDirectory.resolve("analysis-summary.txt"), StandardCharsets.UTF_8);
        assertTrue(summary.contains("status=COMPLETED"));
        assertTrue(summary.contains("joernMode=off"));
        assertTrue(summary.contains("sourceFacts=1"));
        assertTrue(summary.contains("semanticArtifacts=1"));
        assertTrue(standardOutput.toString(StandardCharsets.UTF_8).contains("summaryPath="));
        assertEquals("", errorOutput.toString(StandardCharsets.UTF_8));
    }

    @Test
    void printsHelpWithoutUseCase() {
        var standardOutput = new ByteArrayOutputStream();
        var errorOutput = new ByteArrayOutputStream();
        var useCase = new RecordingUseCase();

        var exitCode = new ForensicAnalyticsCli(useCase, stream(standardOutput), stream(errorOutput)).run(new String[] {"--help"});

        assertEquals(0, exitCode);
        assertTrue(standardOutput.toString(StandardCharsets.UTF_8).contains("forensic-analytics analyze"));
        assertFalse(useCase.called());
    }

    @Test
    void reportsUsageErrors() {
        var standardOutput = new ByteArrayOutputStream();
        var errorOutput = new ByteArrayOutputStream();
        var useCase = new RecordingUseCase();

        var exitCode = new ForensicAnalyticsCli(useCase, stream(standardOutput), stream(errorOutput)).run(new String[] {"analyze"});

        assertEquals(2, exitCode);
        assertTrue(errorOutput.toString(StandardCharsets.UTF_8).contains("Missing required analyze option: --repo"));
        assertFalse(useCase.called());
    }

    @Test
    void reportsUseCaseFailures() {
        var standardOutput = new ByteArrayOutputStream();
        var errorOutput = new ByteArrayOutputStream();
        var cli = ForensicAnalyticsCli.withUseCaseFactory(
            command -> new FailingUseCase(),
            stream(standardOutput),
            stream(errorOutput)
        );

        var exitCode = cli.run(new String[] {
            "analyze",
            "--repo", "project",
            "--profile", "baseline",
            "--output", tempDir.resolve("analysis-out").toString(),
            "--joern-mode", "docker"
        });

        assertEquals(1, exitCode);
        assertTrue(errorOutput.toString(StandardCharsets.UTF_8).contains("Analysis failed: failed"));
    }

    @Test
    void reportsMissingServiceProviderForStandaloneMainWiring() {
        var standardOutput = new ByteArrayOutputStream();
        var errorOutput = new ByteArrayOutputStream();

        var exitCode = ForensicAnalyticsCli.runWithServiceLoader(new String[] {
            "analyze",
            "--repo", "project",
            "--profile", "baseline",
            "--output", tempDir.resolve("analysis-out").toString(),
            "--joern-mode", "off"
        }, stream(standardOutput), stream(errorOutput));

        assertEquals(1, exitCode);
        assertTrue(errorOutput.toString(StandardCharsets.UTF_8).contains("No RunRepositoryAnalysisUseCase service provider"));
    }

    private static PrintStream stream(ByteArrayOutputStream output) {
        return new PrintStream(output, true, StandardCharsets.UTF_8);
    }

    private static final class RecordingUseCase implements RunRepositoryAnalysisUseCase {
        private RunRepositoryAnalysisCommand command;

        @Override
        public RunRepositoryAnalysisResult run(RunRepositoryAnalysisCommand command) {
            this.command = command;
            return RunRepositoryAnalysisResult.completed(
                command.analysisRunId(),
                command.repositoryMetadata(),
                command.analysisProfile(),
                List.of(new SourceFact(
                    "class",
                    new SourceLocation("src/main/java/App.java", "com.example.App", "main", 1),
                    "com.example.App",
                    "class App"
                )),
                new SemanticAnalysisResult("fake-semantic", List.of(new ArtifactReference("cpg.bin", "joern-cpg", "abc", 1))),
                new RuleGenerationResult(List.of(new ArtifactReference("rules.btm", "byteman-rules", "def", 2)))
            );
        }

        private boolean called() {
            return command != null;
        }
    }

    private static final class FailingUseCase implements RunRepositoryAnalysisUseCase {
        @Override
        public RunRepositoryAnalysisResult run(RunRepositoryAnalysisCommand command) {
            throw new IllegalStateException("failed");
        }
    }
}
