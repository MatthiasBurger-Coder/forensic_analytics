package de.burger.forensics.analytics.services.testbed;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositoryAnalysisMiniEndToEndTest {
    @Test
    void miniRepositoryAnalyzeRepositoryFlowIsExplicitlyDeprecatedForTargetServices() throws IOException {
        var ingestionEndpoint = read("services/ingestion-service/src/main/java/de/burger/forensics/analytics/services/ingestion/adapter/in/grpc/ForensicIngestionGrpcEndpoint.java");
        var ingestionTest = read("services/ingestion-service/src/test/java/de/burger/forensics/analytics/services/ingestion/adapter/in/grpc/ForensicIngestionGrpcEndpointTest.java");
        var repositorySourceTest = read("services/repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/adapter/in/grpc/RepositorySourceGrpcEndpointTest.java");
        var orchestratorTest = read("services/analysis-orchestrator-service/src/test/java/de/burger/forensics/analytics/services/analysisorchestrator/adapter/in/grpc/AnalysisJobGrpcEndpointTest.java");

        assertContainsAll(
            ingestionEndpoint,
            "AnalyzeRepository is not implemented by ingestion-service; repository checkout is owned by repository-source-service",
            "Status.UNIMPLEMENTED"
        );
        assertContainsAll(
            ingestionTest,
            "analyzeRepositoryIsExplicitlyOutsideIngestionScope",
            "AnalyzeRepository is not implemented by ingestion-service; repository checkout is owned by repository-source-service"
        );
        assertContainsAll(
            repositorySourceTest,
            "rejectsLegacyLocalRepositoryInputsAtGrpcBoundaryWithoutLeakingPaths",
            "file:///tmp/repo",
            "/tmp/repo",
            "Invalid repository source request"
        );
        assertContainsAll(
            orchestratorTest,
            "acceptsRepositoryToBtmAsPendingStatusWithoutWorkerExecution",
            "REPOSITORY_TO_BTM_WAITING_FOR_REPOSITORY",
            "assertNoWorkersCanLeaseJobs"
        );
    }

    @Test
    void miniRepositoryTargetCoverageDoesNotClaimLegacySessionRegistrationParity() throws IOException {
        var repositorySourceReadme = read("services/repository-source-service/README.md");
        var ingestionReadme = read("services/ingestion-service/README.md");
        var orchestratorReadme = read("services/analysis-orchestrator-service/README.md");
        var testbedReadme = read("services/testbed/README.md");

        assertContainsAll(
            repositorySourceReadme,
            "clean HTTPS repository URLs only",
            "Local paths",
            "file:",
            "explicitly deprecated"
        );
        assertContainsAll(
            ingestionReadme,
            "AnalyzeRepository",
            "UNIMPLEMENTED",
            "repository checkout is not an ingestion responsibility"
        );
        assertContainsAll(
            orchestratorReadme,
            "StartRepositoryToBtm",
            "waiting for repository source handoff",
            "does not dispatch repository workers"
        );
        assertContainsAll(
            testbedReadme,
            "S03",
            "AnalyzeRepository",
            "deprecated",
            "not session-registration parity"
        );
        assertFalse(testbedReadme.contains("S03 proves completed local repository analysis parity"));
        assertFalse(testbedReadme.contains("legacy module remains active"));
        assertFalse(testbedReadme.contains("current-quality-gate evidence"));
        assertFalse(testbedReadme.contains("root quality gate still includes `forensic-analytics-testbed`"));
    }

    private static void assertContainsAll(String content, String... expectedFragments) {
        var normalizedContent = normalize(content);
        for (var fragment : expectedFragments) {
            assertTrue(normalizedContent.contains(normalize(fragment)), () -> "Missing expected fragment: " + fragment);
        }
    }

    private static String normalize(String value) {
        return value.replaceAll("\\s+", " ");
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
