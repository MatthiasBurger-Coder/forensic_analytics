package de.burger.forensics.analytics.services.cliclient.adapter.in.cli;

import de.burger.forensics.analytics.services.cliclient.application.CliClientSubmissionService;
import de.burger.forensics.analytics.services.cliclient.application.port.out.RepositoryAnalysisSubmissionPort;
import de.burger.forensics.analytics.services.cliclient.domain.CliClientSubmissionCommand;
import de.burger.forensics.analytics.services.cliclient.domain.CliClientSubmissionResult;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CliClientRunnerTest {
    @Test
    void gatewaySubmitCallsPublicApiPortAndPrintsPublicFieldsOnly() {
        var submitted = new AtomicReference<CliClientSubmissionCommand>();
        var standardOutput = new ByteArrayOutputStream();
        var errorOutput = new ByteArrayOutputStream();
        var runner = runner(command -> {
            submitted.set(command);
            return acceptedResult();
        }, standardOutput, errorOutput);

        var exitCode = runner.run(gatewaySubmitArgs());

        assertEquals(0, exitCode);
        assertNotNull(submitted.get());
        assertEquals("http://gateway.example/api", submitted.get().publicApiBaseUrl().toString());
        assertEquals("https://example.com/acme/demo.git", submitted.get().repositoryUrl());
        assertEquals("main", submitted.get().branch());
        assertEquals("", submitted.get().commit());
        assertEquals("gateway.v1", submitted.get().schemaVersion());
        assertEquals(List.of("BTM_RULES"), submitted.get().requestedOutputs());
        assertEquals("github", submitted.get().provider());
        assertEquals(List.of(":app", ":lib"), submitted.get().declaredModules());
        assertEquals("correlation-1", submitted.get().correlationId());
        assertEquals("idem-1", submitted.get().idempotencyKey());
        assertEquals(60L, submitted.get().timeoutSeconds());
        assertEquals(100_000L, submitted.get().maxWorkspaceBytes());
        assertTrue(submitted.get().allowShallowClone());
        var output = standardOutput.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("analysisRunId=analysis-run-1"));
        assertTrue(output.contains("status=ACCEPTED"));
        assertTrue(output.contains("btmDeliveryStatus=BTM_DELIVERY_NOT_READY"));
        assertTrue(output.contains("diagnostics=1"));
        assertFalse(output.contains("workspace-"));
        assertFalse(output.contains("/tmp"));
        assertEquals("", errorOutput.toString(StandardCharsets.UTF_8));
    }

    @Test
    void rejectsLegacyLocalCommandsInsteadOfRoutingThemToPublicApi() {
        var submitted = new AtomicReference<CliClientSubmissionCommand>();
        var standardOutput = new ByteArrayOutputStream();
        var errorOutput = new ByteArrayOutputStream();
        var runner = runner(command -> {
            submitted.set(command);
            return acceptedResult();
        }, standardOutput, errorOutput);

        var analyzeExitCode = runner.run(new String[] {"analyze"});
        var ingestExitCode = runner.run(new String[] {"ingest-request"});

        assertEquals(2, analyzeExitCode);
        assertEquals(2, ingestExitCode);
        assertEquals(null, submitted.get());
        var errors = errorOutput.toString(StandardCharsets.UTF_8);
        assertTrue(errors.contains("Unknown command: analyze"));
        assertTrue(errors.contains("Unknown command: ingest-request"));
        assertTrue(errors.contains("cli-client gateway-submit"));
        assertEquals("", standardOutput.toString(StandardCharsets.UTF_8));
    }

    @Test
    void requiresBranchOrCommitForRepositorySubmission() {
        var standardOutput = new ByteArrayOutputStream();
        var errorOutput = new ByteArrayOutputStream();
        var runner = runner(command -> acceptedResult(), standardOutput, errorOutput);

        var exitCode = runner.run(withoutOption(gatewaySubmitArgs(), "--branch"));

        assertEquals(2, exitCode);
        assertTrue(errorOutput.toString(StandardCharsets.UTF_8).contains("gateway-submit requires --branch or --commit."));
        assertEquals("", standardOutput.toString(StandardCharsets.UTF_8));
    }

    @Test
    void validatesTargetGatewaySubmitInputsBeforeCallingPublicApi() {
        var cases = List.of(
            new InvalidCase(
                replaceOption(gatewaySubmitArgs(), "--repo-url", "http://example.com/acme/demo.git"),
                "gateway-submit --repo-url must be an HTTPS URL."
            ),
            new InvalidCase(
                replaceOption(gatewaySubmitArgs(), "--requested-outputs", "SOURCE_FACTS"),
                "Unsupported gateway-submit requested output: SOURCE_FACTS"
            ),
            new InvalidCase(
                replaceOption(gatewaySubmitArgs(), "--correlation-id", "bad correlation"),
                "gateway-submit --correlation-id contains unsupported characters."
            ),
            new InvalidCase(
                replaceOption(gatewaySubmitArgs(), "--timeout-seconds", "0"),
                "gateway-submit --timeout-seconds must be between 1 and 3600."
            ),
            new InvalidCase(
                replaceOption(gatewaySubmitArgs(), "--max-workspace-bytes", "0"),
                "gateway-submit --max-workspace-bytes must be between 1 and 107374182400."
            ),
            new InvalidCase(
                append(gatewaySubmitArgs(), "--unknown", "value"),
                "Unknown gateway-submit option: --unknown"
            ),
            new InvalidCase(
                append(gatewaySubmitArgs(), "--branch", "develop"),
                "Duplicate gateway-submit option: --branch"
            ),
            new InvalidCase(
                withoutOptionValue(gatewaySubmitArgs(), "--branch"),
                "Missing value for gateway-submit option: --branch"
            ),
            new InvalidCase(
                replaceOption(gatewaySubmitArgs(), "--timeout-seconds", "sixty"),
                "Invalid numeric gateway-submit option --timeout-seconds."
            ),
            new InvalidCase(
                replaceOption(gatewaySubmitArgs(), "--allow-shallow-clone", "yes"),
                "Invalid boolean gateway-submit option --allow-shallow-clone."
            )
        );
        for (var invalidCase : cases) {
            var submitted = new AtomicReference<CliClientSubmissionCommand>();
            var standardOutput = new ByteArrayOutputStream();
            var errorOutput = new ByteArrayOutputStream();
            var runner = runner(command -> {
                submitted.set(command);
                return acceptedResult();
            }, standardOutput, errorOutput);

            var exitCode = runner.run(invalidCase.args());

            assertEquals(2, exitCode, invalidCase.expectedMessage());
            assertEquals(null, submitted.get(), invalidCase.expectedMessage());
            assertTrue(
                errorOutput.toString(StandardCharsets.UTF_8).contains(invalidCase.expectedMessage()),
                invalidCase.expectedMessage()
            );
            assertEquals("", standardOutput.toString(StandardCharsets.UTF_8), invalidCase.expectedMessage());
        }
    }

    @Test
    void reportsPublicApiErrorsWithoutPrivateDetails() {
        var standardOutput = new ByteArrayOutputStream();
        var errorOutput = new ByteArrayOutputStream();
        var runner = runner(command -> {
            throw new IllegalStateException(
                "Public API error status=409 code=CONFLICT retryable=false correlationId=correlation-1"
            );
        }, standardOutput, errorOutput);

        var exitCode = runner.run(gatewaySubmitArgs());

        assertEquals(1, exitCode);
        assertEquals("", standardOutput.toString(StandardCharsets.UTF_8));
        var errors = errorOutput.toString(StandardCharsets.UTF_8);
        assertTrue(errors.contains("code=CONFLICT"));
        assertTrue(errors.contains("retryable=false"));
        assertFalse(errors.contains("/tmp"));
        assertFalse(errors.contains("Authorization"));
    }

    @Test
    void printsHelpWithoutCallingPublicApi() {
        var submitted = new AtomicReference<CliClientSubmissionCommand>();
        var standardOutput = new ByteArrayOutputStream();
        var errorOutput = new ByteArrayOutputStream();
        var runner = runner(command -> {
            submitted.set(command);
            return acceptedResult();
        }, standardOutput, errorOutput);

        var exitCode = runner.run(new String[] {"--help"});

        assertEquals(0, exitCode);
        assertEquals(null, submitted.get());
        assertTrue(standardOutput.toString(StandardCharsets.UTF_8).contains("cli-client gateway-submit"));
        assertEquals("", errorOutput.toString(StandardCharsets.UTF_8));
    }

    @Test
    void parsesHelpAliasMissingCommandAndFalseBoolean() {
        var submitted = new AtomicReference<CliClientSubmissionCommand>();
        var standardOutput = new ByteArrayOutputStream();
        var errorOutput = new ByteArrayOutputStream();
        var runner = runner(command -> {
            submitted.set(command);
            return acceptedResult();
        }, standardOutput, errorOutput);

        var helpExitCode = runner.run(new String[] {"help"});
        var missingExitCode = runner.run(new String[0]);
        var falseBooleanExitCode = runner.run(append(
            replaceOption(withoutOption(gatewaySubmitArgs(), "--branch"), "--allow-shallow-clone", "false"),
            "--commit",
            "abc123"
        ));

        assertEquals(0, helpExitCode);
        assertEquals(2, missingExitCode);
        assertEquals(0, falseBooleanExitCode);
        assertNotNull(submitted.get());
        assertEquals("", submitted.get().branch());
        assertEquals("false", Boolean.toString(submitted.get().allowShallowClone()));
        assertTrue(standardOutput.toString(StandardCharsets.UTF_8).contains("cli-client gateway-submit"));
        assertTrue(errorOutput.toString(StandardCharsets.UTF_8).contains("Missing command."));
    }

    @Test
    void reportsParserOnlyValidationBranches() {
        var cases = List.of(
            new InvalidCase(
                withoutOption(gatewaySubmitArgs(), "--gateway"),
                "Missing required gateway-submit option: --gateway"
            ),
            new InvalidCase(
                replaceOption(gatewaySubmitArgs(), "--gateway", "https://[invalid"),
                "Invalid gateway-submit option --gateway."
            ),
            new InvalidCase(
                replaceOption(gatewaySubmitArgs(), "--declared-modules", " , "),
                "Missing value for gateway-submit option: --declared-modules"
            ),
            new InvalidCase(
                append(withoutOptionValue(gatewaySubmitArgs(), "--branch"), "--commit", "abc123"),
                "Missing value for gateway-submit option: --branch"
            )
        );
        for (var invalidCase : cases) {
            var standardOutput = new ByteArrayOutputStream();
            var errorOutput = new ByteArrayOutputStream();
            var runner = runner(command -> acceptedResult(), standardOutput, errorOutput);

            var exitCode = runner.run(invalidCase.args());

            assertEquals(2, exitCode, invalidCase.expectedMessage());
            assertTrue(
                errorOutput.toString(StandardCharsets.UTF_8).contains(invalidCase.expectedMessage()),
                invalidCase.expectedMessage()
            );
            assertEquals("", standardOutput.toString(StandardCharsets.UTF_8), invalidCase.expectedMessage());
        }
    }

    private static CliClientRunner runner(
        RepositoryAnalysisSubmissionPort submissionPort,
        ByteArrayOutputStream standardOutput,
        ByteArrayOutputStream errorOutput
    ) {
        return new CliClientRunner(
            new CliClientSubmissionService(submissionPort),
            stream(standardOutput),
            stream(errorOutput)
        );
    }

    private static PrintStream stream(ByteArrayOutputStream output) {
        return new PrintStream(output, true, StandardCharsets.UTF_8);
    }

    private static CliClientSubmissionResult acceptedResult() {
        return new CliClientSubmissionResult(
            "analysis-run-1",
            "ACCEPTED",
            "/repository-analyses/analysis-run-1",
            "/repository-analyses/analysis-run-1/jobs",
            "BTM_DELIVERY_NOT_READY",
            "BtmArtifactDeliveryService",
            "correlation-1",
            1
        );
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

    private static String[] withoutOption(String[] args, String option) {
        var values = new ArrayList<String>();
        for (var index = 0; index < args.length; index++) {
            if (option.equals(args[index])) {
                index++;
                continue;
            }
            values.add(args[index]);
        }
        return values.toArray(String[]::new);
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

    private static String[] append(String[] args, String option, String value) {
        var values = new ArrayList<>(List.of(args));
        values.add(option);
        values.add(value);
        return values.toArray(String[]::new);
    }

    private static String[] withoutOptionValue(String[] args, String option) {
        var values = new ArrayList<String>();
        for (var index = 0; index < args.length; index++) {
            values.add(args[index]);
            if (option.equals(args[index])) {
                index++;
            }
        }
        return values.toArray(String[]::new);
    }

    private record InvalidCase(String[] args, String expectedMessage) {
    }
}
