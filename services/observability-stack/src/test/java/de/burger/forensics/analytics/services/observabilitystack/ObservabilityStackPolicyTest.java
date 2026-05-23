package de.burger.forensics.analytics.services.observabilitystack;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservabilityStackPolicyTest {
    private final List<String> lines = readPolicy();

    @Test
    void policyKeepsDiagnosticsSeparateFromForensicEvidence() {
        assertEquals("operational-diagnostics-only", scalar("evidenceClassification"));
        assertEquals("forbidden", map("serviceBoundaryRules").get("operationalLogsAsForensicEvidence"));
        assertEquals("forbiddenForTargetServices", map("serviceBoundaryRules").get("sharedJavaLoggingModule"));
        assertEquals("required", map("serviceBoundaryRules").get("serviceLocalDiagnostics"));
    }

    @Test
    void policyAllowsCorrelationAndTraceContextWithoutFabricatingMissingValues() {
        var allowedFields = list("allowedLogFields");
        assertContainsAll(
            allowedFields,
            "correlationId",
            "traceId",
            "spanId",
            "parentSpanId",
            "analysisRunId",
            "runtimeSessionId",
            "incidentId",
            "serviceName",
            "workerName",
            "stepName"
        );

        var missingValues = map("missingValuePolicy");
        assertEquals("unavailable", missingValues.get("correlationId"));
        assertEquals("unavailable", missingValues.get("traceId"));
        assertEquals("notApplicable", missingValues.get("workerName"));
        assertEquals("forbidden", missingValues.get("fabricatedDiagnosticValues"));
    }

    @Test
    void policyRejectsSensitiveOperationalValues() {
        var forbiddenValues = list("forbiddenLogValues");
        assertContainsAll(
            forbiddenValues,
            "methodArguments",
            "methodReturnValues",
            "rawPayloads",
            "sourceContent",
            "stackFrames",
            "rawExceptionMessages",
            "credentials",
            "tokens",
            "privateWorkspacePaths",
            "llmPromptContent"
        );

        var redactionRules = map("redactionRules");
        assertEquals("reject", redactionRules.get("methodArguments"));
        assertEquals("reject", redactionRules.get("methodReturnValues"));
        assertEquals("reject", redactionRules.get("rawExceptionMessages"));
        assertEquals("reject", redactionRules.get("privateWorkspacePaths"));
        assertEquals("reject", redactionRules.get("tokens"));
        assertEquals("reject", redactionRules.get("llmPromptContent"));
    }

    @Test
    void policyDoesNotClaimRuntimeReadinessOrPublicDiagnosticExposure() {
        var diagnosticSurfacePolicy = map("diagnosticSurfacePolicy");
        assertEquals("loopbackOrInternalNetwork", diagnosticSurfacePolicy.get("defaultBinding"));
        assertEquals(
            "forbiddenWithoutExplicitDeploymentValidation",
            diagnosticSurfacePolicy.get("publicExposure")
        );
        assertEquals(
            "requiresExplicitDeploymentValidation",
            diagnosticSurfacePolicy.get("healthMetricsTracingOrLogShippingExposure")
        );

        var runtimeReadiness = map("runtimeReadiness");
        assertEquals("notClaimed", runtimeReadiness.get("dockerCompose"));
        assertEquals("notClaimed", runtimeReadiness.get("dockerSwarm"));
        assertEquals("notClaimed", runtimeReadiness.get("kubernetes"));
        assertEquals("notClaimed", runtimeReadiness.get("prometheus"));
        assertEquals("notClaimed", runtimeReadiness.get("grafana"));
        assertEquals("notClaimed", runtimeReadiness.get("opentelemetryCollector"));
        assertEquals("notClaimed", runtimeReadiness.get("logShipping"));
    }

    private String scalar(String key) {
        return lines.stream()
            .filter(line -> line.startsWith(key + ": "))
            .map(line -> line.substring((key + ": ").length()).strip())
            .findFirst()
            .orElseThrow();
    }

    private List<String> list(String section) {
        var sectionStart = sectionStart(section);
        return lines.stream()
            .skip(sectionStart + 1L)
            .takeWhile(line -> line.startsWith("  - "))
            .map(line -> line.substring("  - ".length()).strip())
            .toList();
    }

    private Map<String, String> map(String section) {
        var sectionStart = sectionStart(section);
        return lines.stream()
            .skip(sectionStart + 1L)
            .takeWhile(line -> line.startsWith("  ") && !line.startsWith("  - "))
            .map(String::strip)
            .map(line -> line.split(": ", 2))
            .collect(java.util.stream.Collectors.toMap(parts -> parts[0], parts -> parts[1]));
    }

    private int sectionStart(String section) {
        var marker = section + ":";
        for (var index = 0; index < lines.size(); index++) {
            if (lines.get(index).equals(marker)) {
                return index;
            }
        }
        throw new IllegalArgumentException("Missing policy section " + section);
    }

    private static void assertContainsAll(List<String> actualValues, String... expectedValues) {
        for (var expectedValue : expectedValues) {
            assertTrue(
                actualValues.contains(expectedValue),
                () -> "Expected policy values to contain " + expectedValue + " but got " + actualValues
            );
        }
    }

    private static List<String> readPolicy() {
        try {
            return Files.readAllLines(findRepositoryRoot().resolve("deployment/observability/service-diagnostics-policy.yaml"));
        } catch (IOException error) {
            throw new IllegalStateException("Unable to read observability diagnostics policy", error);
        }
    }

    private static Path findRepositoryRoot() {
        var current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("settings.gradle.kts"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Unable to locate repository root");
    }
}
