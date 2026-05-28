package de.burger.forensics.analytics.services.testbed;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositoryAnalysisTestbedTest {
    @Test
    void targetCliDocumentsLegacyLocalCommandDeprecationWithoutInProcessRouting() throws IOException {
        var cliContract = read("contracts/cli/gateway-cli-contract.md");
        var cliReadme = read("cli-client/README.md");
        var cliRunnerTest = read("cli-client/src/test/java/de/burger/forensics/analytics/services/cliclient/adapter/in/cli/CliClientRunnerTest.java");

        assertContainsAll(
            cliContract,
            "S16",
            "deprecated target behavior",
            "legacy in-process adapters",
            "not silently routed",
            "public API"
        );
        assertContainsAll(
            cliReadme,
            "S16",
            "deprecated target behavior",
            "does not implement analysis execution",
            "does not implement engine-request import"
        );
        assertContainsAll(
            cliRunnerTest,
            "rejectsLegacyLocalCommandsInsteadOfRoutingThemToPublicApi",
            "\"analyze\"",
            "\"ingest-request\"",
            "\"status\"",
            "\"report\""
        );
    }

    @Test
    void publicApiEvidenceRemainsAcceptedPendingAndDoesNotClaimBtmReportOrReplayParity() throws IOException {
        var queryReportReadme = read("query-report-api-service/README.md");
        var queryReportHttpTest = read("query-report-api-service/src/test/java/de/burger/forensics/analytics/services/queryreportapi/adapter/in/http/QueryReportApiHttpAdapterTest.java");

        assertContainsAll(
            queryReportReadme,
            "accepted/pending state",
            "does not claim worker execution",
            "generated BTM bytes",
            "report readiness",
            "S16"
        );
        assertContainsAll(
            queryReportHttpTest,
            "extensionRoutesRemainUnavailableWithoutBtmReportOrReplayParity",
            "/api/repository-analyses/analysis-run-1/jobs",
            "/api/repository-analyses/analysis-run-1/results",
            "/api/repository-analyses/analysis-run-1/replay",
            "/api/repository-analyses/analysis-run-1/reports"
        );
    }

    @Test
    void workerContractsExposeArtifactBoundariesWithoutLegacyGraphOrRuleParity() throws IOException {
        var javaAstContract = read("contracts/grpc/java-ast-analysis.proto");
        var joernContract = read("contracts/grpc/joern-cpg-analysis.proto");

        assertContainsAll(
            javaAstContract,
            "source_fact_artifact",
            "must not claim runtime",
            "graph edges",
            "BTM"
        );
        assertFalse(javaAstContract.contains("semantic_graph"));
        assertFalse(javaAstContract.contains("rule_artifacts"));
        assertContainsAll(
            joernContract,
            "semantic_artifacts",
            "semantic artifact metadata only",
            "not claim runtime execution"
        );
        assertFalse(joernContract.contains("semantic_graph"));
        assertFalse(joernContract.contains("call_relations"));
        assertFalse(joernContract.contains("data_flow_paths"));
    }

    @Test
    void joernDockerSmokeIsNotDefaultTargetRuntimeReadiness() throws IOException {
        var joernReadme = read("joern-analysis-service/README.md");
        var testbedReadme = read("testbed/README.md");

        assertContainsAll(
            joernReadme,
            "Docker image builds and Joern runtime smoke tests are optional external",
            "S16",
            "not target runtime readiness"
        );
        assertContainsAll(
            testbedReadme,
            "S16",
            "RepositoryAnalysisTestbedTest",
            "deprecation evidence",
            "completed local",
            "analysis parity"
        );
    }

    private static void assertContainsAll(String content, String... expectedFragments) {
        for (var fragment : expectedFragments) {
            assertTrue(content.contains(fragment), () -> "Missing expected fragment: " + fragment);
        }
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(repositoryRoot().resolve(relativePath), StandardCharsets.UTF_8);
    }

    private static Path repositoryRoot() {
        var current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("settings.gradle.kts"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Repository root not found from current working directory");
    }
}
