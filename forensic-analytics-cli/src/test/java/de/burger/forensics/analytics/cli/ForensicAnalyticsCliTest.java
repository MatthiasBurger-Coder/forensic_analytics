package de.burger.forensics.analytics.cli;

import de.burger.forensics.analytics.application.analysis.RunRepositoryAnalysisUseCase;
import de.burger.forensics.analytics.application.analysis.command.RunRepositoryAnalysisCommand;
import de.burger.forensics.analytics.application.analysis.result.RuleGenerationResult;
import de.burger.forensics.analytics.application.analysis.result.RunRepositoryAnalysisResult;
import de.burger.forensics.analytics.application.analysis.result.SemanticAnalysisResult;
import de.burger.forensics.analytics.domain.artifact.ArtifactReference;
import de.burger.forensics.analytics.domain.semantic.SemanticGraph;
import de.burger.forensics.analytics.domain.source.SourceFact;
import de.burger.forensics.analytics.domain.source.SourceLocation;
import de.burger.forensics.analytics.observability.CorrelationContext;
import de.burger.forensics.analytics.observability.OperationLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForensicAnalyticsCliTest {
    @TempDir
    Path tempDir;

    @Test
    void cliGatewayContractKeepsCompatibilityModeExplicitAndRedacted() throws Exception {
        var contract = Files.readString(findCliGatewayContract(), StandardCharsets.UTF_8);

        assertContains(contract, "Contract version | `gateway-cli-v1`");
        assertContains(contract, "`gateway-submit` is the explicit compatibility command");
        assertContains(contract, "the target CLI is `cli-client`");
        assertContains(contract, "current predecessor implementation is `forensic-analytics-cli`");
        assertContains(contract, "S09 target-client path keeps the compatibility command name");
        assertContains(contract, "`analyze` or `ingest-request`");
        assertContains(contract, "commands to the public API");
        assertContains(contract, "Local/private host and network-range policy remains owned by");
        assertContains(contract, "Status reads are out of S09");
        assertContains(contract, "fixed client value `{}`");
        assertContains(contract, "StartRepositoryAnalysisRequest.repositoryUrl");
        assertContains(contract, "X-Correlation-Id");
        assertContains(contract, "Idempotency-Key");
        assertContains(contract, "RepositoryToBtmSubmission");
        assertContains(contract, "RepositoryToBtmStatus");
        assertContains(contract, "workspace IDs or workspace paths");
        assertContains(contract, "raw Git stdout or stderr");
        assertContains(contract, "must not depend on `query-report-api-service` implementation classes");
        assertContains(contract, "predecessor Gateway implementation classes");
        assertContains(contract, "contracts/openapi/gateway-api.yaml");
    }

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
        assertTrue(summary.contains("semanticFingerprint=sha256:semantic"));
        assertTrue(summary.contains("semanticArtifacts=1"));
        assertTrue(summary.contains("semanticNodes=0"));
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
        assertTrue(standardOutput.toString(StandardCharsets.UTF_8).contains("forensic-analytics gateway-submit"));
        assertFalse(useCase.called());
    }

    @Test
    void gatewaySubmitUsesGatewayClientWithoutAnalysisUseCase() {
        var submitted = new AtomicReference<GatewaySubmitCommand>();
        var standardOutput = new ByteArrayOutputStream();
        var errorOutput = new ByteArrayOutputStream();
        var cli = ForensicAnalyticsCli.withFactories(
            command -> {
                throw new AssertionError("analysis use case must not be used by gateway-submit");
            },
            command -> {
                throw new AssertionError("request importer must not be used by gateway-submit");
            },
            factoryCommand -> clientCommand -> {
                assertEquals(factoryCommand, clientCommand);
                submitted.set(clientCommand);
                return new GatewaySubmissionResult(
                    "analysis-run-1",
                    "ACCEPTED",
                    "/repository-analyses/analysis-run-1",
                    "/repository-analyses/analysis-run-1/jobs",
                    "BTM_DELIVERY_NOT_READY",
                    "BtmArtifactDeliveryService",
                    "correlation-1",
                    1
                );
            },
            stream(standardOutput),
            stream(errorOutput),
            OperationLogger.noop()
        );

        var exitCode = cli.run(gatewaySubmitArgs());

        assertEquals(0, exitCode);
        assertNotNull(submitted.get());
        assertEquals("https://example.com/acme/demo.git", submitted.get().repositoryUrl());
        assertEquals("main", submitted.get().branch());
        assertEquals("", submitted.get().commit());
        assertEquals(List.of("BTM_RULES"), submitted.get().requestedOutputs());
        assertEquals(List.of(":app", ":lib"), submitted.get().declaredModules());
        assertEquals("correlation-1", submitted.get().correlationId());
        assertEquals("idem-1", submitted.get().idempotencyKey());
        assertTrue(standardOutput.toString(StandardCharsets.UTF_8).contains("analysisRunId=analysis-run-1"));
        assertTrue(standardOutput.toString(StandardCharsets.UTF_8).contains("status=ACCEPTED"));
        assertTrue(standardOutput.toString(StandardCharsets.UTF_8).contains("btmDeliveryStatus=BTM_DELIVERY_NOT_READY"));
        assertTrue(standardOutput.toString(StandardCharsets.UTF_8).contains("diagnostics=1"));
        assertFalse(standardOutput.toString(StandardCharsets.UTF_8).contains("workspace-"));
        assertFalse(standardOutput.toString(StandardCharsets.UTF_8).contains("/tmp"));
        assertEquals("", errorOutput.toString(StandardCharsets.UTF_8));
    }

    @Test
    void gatewaySubmitMapsGatewayFailureWithoutAnalysisUseCase() {
        var standardOutput = new ByteArrayOutputStream();
        var errorOutput = new ByteArrayOutputStream();
        var cli = ForensicAnalyticsCli.withFactories(
            command -> {
                throw new AssertionError("analysis use case must not be used by gateway-submit");
            },
            command -> {
                throw new AssertionError("request importer must not be used by gateway-submit");
            },
            command -> ignored -> {
                throw new CliGatewayException("Gateway error status=409 code=CONFLICT retryable=false correlationId=correlation-1");
            },
            stream(standardOutput),
            stream(errorOutput),
            OperationLogger.noop()
        );

        var exitCode = cli.run(gatewaySubmitArgs());

        assertEquals(1, exitCode);
        assertEquals("", standardOutput.toString(StandardCharsets.UTF_8));
        assertTrue(errorOutput.toString(StandardCharsets.UTF_8).contains("code=CONFLICT"));
        assertTrue(errorOutput.toString(StandardCharsets.UTF_8).contains("retryable=false"));
        assertFalse(errorOutput.toString(StandardCharsets.UTF_8).contains("/tmp"));
    }

    @Test
    void gatewaySubmitRequiresExplicitBranchOrCommit() {
        var standardOutput = new ByteArrayOutputStream();
        var errorOutput = new ByteArrayOutputStream();
        var args = gatewaySubmitArgs();
        var withoutBranch = new ArrayList<String>();
        for (var source = 0; source < args.length; source++) {
            if ("--branch".equals(args[source])) {
                source++;
                continue;
            }
            withoutBranch.add(args[source]);
        }

        var exitCode = new ForensicAnalyticsCli(new RecordingUseCase(), stream(standardOutput), stream(errorOutput)).run(
            withoutBranch.toArray(String[]::new)
        );

        assertEquals(2, exitCode);
        assertTrue(errorOutput.toString(StandardCharsets.UTF_8).contains("gateway-submit requires --branch or --commit"));
    }

    @Test
    void gatewaySubmitValidationErrorsDoNotEchoSecretLikeInputValues() {
        var cases = List.of(
            new InvalidGatewaySubmitCase(
                replaceOption(gatewaySubmitArgs(), "--gateway", "https://token:secret@gateway.example/[invalid"),
                "Invalid gateway-submit option --gateway."
            ),
            new InvalidGatewaySubmitCase(
                replaceOption(gatewaySubmitArgs(), "--repo-url", "https://token:secret@[invalid"),
                "Invalid gateway-submit --repo-url."
            ),
            new InvalidGatewaySubmitCase(
                replaceOption(gatewaySubmitArgs(), "--timeout-seconds", "secret-timeout-token"),
                "Invalid numeric gateway-submit option --timeout-seconds."
            ),
            new InvalidGatewaySubmitCase(
                replaceOption(gatewaySubmitArgs(), "--allow-shallow-clone", "secret-boolean-token"),
                "Invalid boolean gateway-submit option --allow-shallow-clone."
            )
        );
        for (var invalidCase : cases) {
            var standardOutput = new ByteArrayOutputStream();
            var errorOutput = new ByteArrayOutputStream();

            var exitCode = new ForensicAnalyticsCli(new RecordingUseCase(), stream(standardOutput), stream(errorOutput)).run(
                invalidCase.args()
            );

            assertEquals(2, exitCode, invalidCase.expectedMessage());
            var errors = errorOutput.toString(StandardCharsets.UTF_8);
            assertTrue(errors.contains(invalidCase.expectedMessage()), invalidCase.expectedMessage());
            assertFalse(errors.contains("secret"), invalidCase.expectedMessage());
            assertFalse(errors.contains("token"), invalidCase.expectedMessage());
            assertEquals("", standardOutput.toString(StandardCharsets.UTF_8), invalidCase.expectedMessage());
        }
    }

    @Test
    void statusAndReportCommandsRemainUnavailableUntilCliContractDefinesMappings() {
        var standardOutput = new ByteArrayOutputStream();
        var errorOutput = new ByteArrayOutputStream();
        var cli = new ForensicAnalyticsCli(new RecordingUseCase(), stream(standardOutput), stream(errorOutput));

        var statusExitCode = cli.run(new String[] {"status"});
        var reportExitCode = cli.run(new String[] {"report"});

        assertEquals(2, statusExitCode);
        assertEquals(2, reportExitCode);
        var errors = errorOutput.toString(StandardCharsets.UTF_8);
        assertTrue(errors.contains("Unknown command: status"));
        assertTrue(errors.contains("Unknown command: report"));
        assertEquals("", standardOutput.toString(StandardCharsets.UTF_8));
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
    void logsCommandFailuresWithoutChangingErrorOutputContract() {
        var standardOutput = new ByteArrayOutputStream();
        var errorOutput = new ByteArrayOutputStream();
        var operationLogger = new RecordingOperationLogger();
        var cli = ForensicAnalyticsCli.withFactories(
            command -> new FailingUseCase(),
            command -> {
                throw new AssertionError("request importer must not be used");
            },
            stream(standardOutput),
            stream(errorOutput),
            operationLogger
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
        assertEquals(List.of("STARTED", "FAILED"), operationLogger.phases());
        assertEquals("cli.analyze", operationLogger.events.get(0).operation());
        assertEquals("IllegalStateException", operationLogger.events.get(1).errorType());
    }

    @Test
    void logsParsedCommandWithoutChangingStandardOutputContract() {
        var useCase = new RecordingUseCase();
        var standardOutput = new ByteArrayOutputStream();
        var errorOutput = new ByteArrayOutputStream();
        var operationLogger = new RecordingOperationLogger();
        var cli = ForensicAnalyticsCli.withFactories(
            ignored -> useCase,
            command -> {
                throw new AssertionError("request importer must not be used");
            },
            stream(standardOutput),
            stream(errorOutput),
            operationLogger
        );

        var exitCode = cli.run(new String[] {
            "analyze",
            "--repo", "project",
            "--profile", "baseline",
            "--output", tempDir.resolve("analysis-out").toString(),
            "--joern-mode", "off"
        });

        assertEquals(0, exitCode);
        assertTrue(standardOutput.toString(StandardCharsets.UTF_8).contains("summaryPath="));
        assertEquals("", errorOutput.toString(StandardCharsets.UTF_8));
        assertEquals(List.of("STARTED", "SUCCEEDED"), operationLogger.phases());
        assertEquals("cli.analyze", operationLogger.events.get(0).operation());
        assertFalse(operationLogger.events.get(0).correlationId().isBlank());
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

    @Test
    void reportsMissingEnginePayloadFile() throws Exception {
        var requestFile = Files.writeString(
            tempDir.resolve("engine-request.json"),
            engineRequestJson(tempDir.resolve("missing-rules.btm")),
            StandardCharsets.UTF_8
        );
        var standardOutput = new ByteArrayOutputStream();
        var errorOutput = new ByteArrayOutputStream();

        var exitCode = new ForensicAnalyticsCli(new RecordingUseCase(), stream(standardOutput), stream(errorOutput)).run(new String[] {
            "ingest-request",
            "--request", requestFile.toString(),
            "--output", tempDir.resolve("request-out").toString()
        });

        assertEquals(1, exitCode);
        assertTrue(errorOutput.toString(StandardCharsets.UTF_8).contains("Command failed: Engine payload file does not exist"));
    }

    @Test
    void reportsUnknownEnginePayloadKind() throws Exception {
        var payloadFile = Files.writeString(tempDir.resolve("rules.btm"), "RULE test\n", StandardCharsets.UTF_8);
        var requestFile = Files.writeString(
            tempDir.resolve("engine-request.json"),
            engineRequestJson(payloadFile, "UNKNOWN_KIND"),
            StandardCharsets.UTF_8
        );
        var standardOutput = new ByteArrayOutputStream();
        var errorOutput = new ByteArrayOutputStream();

        var exitCode = new ForensicAnalyticsCli(new RecordingUseCase(), stream(standardOutput), stream(errorOutput)).run(new String[] {
            "ingest-request",
            "--request", requestFile.toString(),
            "--output", tempDir.resolve("request-out").toString()
        });

        assertEquals(1, exitCode);
        assertTrue(errorOutput.toString(StandardCharsets.UTF_8).contains("Command failed: Unsupported engine payload kind: UNKNOWN_KIND"));
    }

    private static PrintStream stream(ByteArrayOutputStream output) {
        return new PrintStream(output, true, StandardCharsets.UTF_8);
    }

    private static Path findCliGatewayContract() {
        var current = Path.of("").toAbsolutePath();
        while (current != null) {
            var candidate = current.resolve("contracts/cli/gateway-cli-contract.md");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("contracts/cli/gateway-cli-contract.md not found from test working directory");
    }

    private static void assertContains(String content, String expected) {
        assertTrue(content.contains(expected), () -> "Expected contract content to contain: " + expected);
    }

    private static String[] gatewaySubmitArgs() {
        return new String[] {
            "gateway-submit",
            "--gateway", "http://gateway.example/api",
            "--repo-url", "https://example.com/acme/demo.git",
            "--branch", "main",
            "--request-id", "request-1",
            "--schema-version", "gateway.v1",
            "--requested-outputs", "BTM_RULES",
            "--provider", "github",
            "--build-tool", "gradle",
            "--build-id", "build-1",
            "--root-project", "demo",
            "--declared-modules", ":app,:lib",
            "--correlation-id", "correlation-1",
            "--idempotency-key", "idem-1",
            "--timeout-seconds", "60",
            "--max-workspace-bytes", "100000",
            "--allow-shallow-clone", "true"
        };
    }

    private static String[] replaceOption(String[] args, String option, String replacement) {
        var values = args.clone();
        for (var index = 0; index < values.length - 1; index++) {
            if (option.equals(values[index])) {
                values[index + 1] = replacement;
                return values;
            }
        }
        throw new IllegalArgumentException("Missing option: " + option);
    }

    private record InvalidGatewaySubmitCase(String[] args, String expectedMessage) {
    }

    private static String engineRequestJson(Path payloadFile) {
        return engineRequestJson(payloadFile, "RULE_ARTIFACTS");
    }

    private static final class RecordingOperationLogger implements OperationLogger {
        private final List<Event> events = new CopyOnWriteArrayList<>();

        @Override
        public void started(String operation) {
            events.add(new Event(
                operation,
                "STARTED",
                CorrelationContext.current().map(correlationId -> correlationId.value()).orElse(""),
                -1L,
                ""
            ));
        }

        @Override
        public void succeeded(String operation, long durationMillis) {
            events.add(new Event(
                operation,
                "SUCCEEDED",
                CorrelationContext.current().map(correlationId -> correlationId.value()).orElse(""),
                durationMillis,
                ""
            ));
        }

        @Override
        public void failed(String operation, long durationMillis, Throwable error) {
            events.add(new Event(
                operation,
                "FAILED",
                CorrelationContext.current().map(correlationId -> correlationId.value()).orElse(""),
                durationMillis,
                error.getClass().getSimpleName()
            ));
        }

        private List<String> phases() {
            return events.stream().map(Event::phase).toList();
        }
    }

    private record Event(
        String operation,
        String phase,
        String correlationId,
        long durationMillis,
        String errorType
    ) {
    }

    private static String engineRequestJson(Path payloadFile, String kind) {
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
                  "kind": "%s",
                  "contentType": "text/x-byteman",
                  "file": "%s",
                  "attributes": {
                    "artifact": "btm-rules"
                  }
                }
              ]
            }
            """.formatted(kind, jsonPath(payloadFile));
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
                new SemanticAnalysisResult(
                    "fake-semantic",
                    "sha256:semantic",
                    List.of(new ArtifactReference("cpg.bin", "joern-cpg", "abc", 1)),
                    SemanticGraph.empty()
                ),
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
