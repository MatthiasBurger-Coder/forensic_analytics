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
    void importsEngineRequestAndWritesSummary() throws Exception {
        var payloadFile = Files.writeString(tempDir.resolve("rules.btm"), "RULE test\n", StandardCharsets.UTF_8);
        var requestFile = Files.writeString(
            tempDir.resolve("engine-request.json"),
            engineRequestJson(payloadFile),
            StandardCharsets.UTF_8
        );
        var standardOutput = new ByteArrayOutputStream();
        var errorOutput = new ByteArrayOutputStream();
        var outputDirectory = tempDir.resolve("request-out");
        var useCase = new RecordingUseCase();

        var exitCode = new ForensicAnalyticsCli(useCase, stream(standardOutput), stream(errorOutput)).run(new String[] {
            "ingest-request",
            "--request", requestFile.toString(),
            "--output", outputDirectory.toString()
        });

        assertEquals(0, exitCode);
        assertFalse(useCase.called());
        var summary = Files.readString(outputDirectory.resolve("engine-request-import-summary.txt"), StandardCharsets.UTF_8);
        assertTrue(summary.contains("requestFile=" + requestFile.toAbsolutePath().normalize()));
        assertTrue(summary.contains("status=COMPLETED"));
        assertTrue(summary.contains("uploadedPayloads=1"));
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
        assertTrue(standardOutput.toString(StandardCharsets.UTF_8).contains("forensic-analytics ingest-request"));
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
        assertTrue(errorOutput.toString(StandardCharsets.UTF_8).contains("Command failed: failed"));
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

    @Test
    void standaloneMainWiringImportsEngineRequestWithoutAnalysisServiceProvider() throws Exception {
        var payloadFile = Files.writeString(tempDir.resolve("standalone-rules.btm"), "RULE standalone\n", StandardCharsets.UTF_8);
        var requestFile = Files.writeString(
            tempDir.resolve("standalone-engine-request.json"),
            engineRequestJson(payloadFile),
            StandardCharsets.UTF_8
        );
        var outputDirectory = tempDir.resolve("standalone-request-out");
        var standardOutput = new ByteArrayOutputStream();
        var errorOutput = new ByteArrayOutputStream();

        var exitCode = ForensicAnalyticsCli.runWithServiceLoader(new String[] {
            "ingest-request",
            "--request", requestFile.toString(),
            "--output", outputDirectory.toString()
        }, stream(standardOutput), stream(errorOutput));

        assertEquals(0, exitCode);
        assertEquals("", errorOutput.toString(StandardCharsets.UTF_8));
        assertTrue(standardOutput.toString(StandardCharsets.UTF_8).contains("uploadedPayloads=1"));
        var summary = Files.readString(outputDirectory.resolve("engine-request-import-summary.txt"), StandardCharsets.UTF_8);
        assertTrue(summary.contains("requestFile=" + requestFile.toAbsolutePath().normalize()));
        assertTrue(summary.contains("status=COMPLETED"));
        assertTrue(summary.contains("uploadedPayloads=1"));
    }

    @Test
    void reportsMissingEngineRequestFile() {
        var standardOutput = new ByteArrayOutputStream();
        var errorOutput = new ByteArrayOutputStream();

        var exitCode = new ForensicAnalyticsCli(new RecordingUseCase(), stream(standardOutput), stream(errorOutput)).run(new String[] {
            "ingest-request",
            "--request", tempDir.resolve("missing-engine-request.json").toString(),
            "--output", tempDir.resolve("request-out").toString()
        });

        assertEquals(1, exitCode);
        assertTrue(errorOutput.toString(StandardCharsets.UTF_8).contains("Command failed: Failed to read engine ingestion request"));
    }

    private static PrintStream stream(ByteArrayOutputStream output) {
        return new PrintStream(output, true, StandardCharsets.UTF_8);
    }

    private static String engineRequestJson(Path payloadFile) {
        return """
            {
              "schemaVersion": "1",
              "buildIdentity": {
                "projectId": "project-a",
                "repositoryUrl": "UNKNOWN",
                "branchName": "UNKNOWN",
                "commitHash": "UNKNOWN",
                "buildId": "UNKNOWN",
                "scanTimestamp": "1970-01-01T00:00:00Z"
              },
              "moduleIdentity": {
                "moduleName": "module-a",
                "modulePath": ":module-a"
              },
              "pluginIdentity": {
                "pluginName": "forensics-tracing",
                "pluginVersion": "1.2.3"
              },
              "payloads": [
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
            """.formatted(jsonPath(payloadFile));
    }

    private static String jsonPath(Path path) {
        return path.toAbsolutePath().normalize().toString().replace('\\', '/');
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
