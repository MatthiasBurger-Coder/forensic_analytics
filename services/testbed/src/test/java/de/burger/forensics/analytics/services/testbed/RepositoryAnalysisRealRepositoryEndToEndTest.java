package de.burger.forensics.analytics.services.testbed;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositoryAnalysisRealRepositoryEndToEndTest {
    @Test
    void realRepositoryFixtureBehaviorIsRetainedOnlyAsLegacyRollbackEvidence() throws IOException {
        var testbedReadme = read("services/testbed/README.md");
        var monolithIsolation = read("docs/architecture/monolith-runtime-isolation.md");
        var workflow = read("docs/workflow/workflow.md");

        assertContainsAll(
            testbedReadme,
            "S17",
            "real repository fixture",
            "legacy rollback evidence",
            "target services do not accept local or file repository input"
        );
        assertContainsAll(
            monolithIsolation,
            "AnalyzeRepository",
            "local or file repository checkout",
            "legacy rollback evidence"
        );
        assertContainsAll(
            workflow,
            "S17",
            "explicitly document unsupported/deprecated inputs",
            "local or file repository input is treated as supported without verified target service behavior"
        );
    }

    @Test
    void realRepositoryReplacementCoverageIsServiceOwnedAndIncompleteWhereEvidenceIsMissing() throws IOException {
        var repositorySourceCheckoutTest = read("services/repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/adapter/out/git/GitRepositoryCheckoutAdapterTest.java");
        var repositorySourceWorkspaceTest = read("services/repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/adapter/out/filesystem/FileSystemRepositoryWorkspaceAdapterTest.java");
        var repositorySourceGrpcTest = read("services/repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/adapter/in/grpc/RepositorySourceGrpcEndpointTest.java");
        var orchestratorReadme = read("services/analysis-orchestrator-service/README.md");

        assertContainsAll(
            repositorySourceCheckoutTest,
            "checksOutBranchAndReportsOnlyRelativeSourceRoots",
            "GIT_CHECKOUT_COMPLETED",
            "noneMatch(command -> command.arguments().contains(\"build\"))"
        );
        assertContainsAll(
            repositorySourceWorkspaceTest,
            "preparesOpaqueWorkspaceUnderConfiguredRootAndCleansIt",
            "rejectsEscapedWorkspaceMappingsDuringCleanup"
        );
        assertContainsAll(
            repositorySourceGrpcTest,
            "PackageAvailability.PACKAGE_AVAILABILITY_PENDING",
            "repository-source-service",
            "source-snapshot/"
        );
        assertContainsAll(
            orchestratorReadme,
            "intentionally incomplete",
            "BTM delivery is not ready",
            "does not dispatch repository workers"
        );
        assertFalse(orchestratorReadme.contains("completed local repository analysis"));
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
