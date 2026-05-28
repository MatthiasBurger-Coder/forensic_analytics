package de.burger.forensics.analytics.services.analysisorchestrator.quality;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisOrchestratorServiceBuildIsolationTest {
    private static final Pattern SHARED_DOMAIN_OR_APPLICATION_MODULE =
        Pattern.compile("project\\(\\s*\":forensic-analytics-(domain|application)\"\\s*\\)");

    private static final Pattern SERVICE_IMPLEMENTATION_MODULE =
        Pattern.compile("project\\(\\s*\":([^\"]+-service|cli-client|observability-stack|testbed)\"\\s*\\)");

    private static final List<String> NON_PRODUCTIVE_ROOTS =
        List.of("graph-replay-service", "observability-stack", "report-generation-service", "testbed");

    @Test
    void buildDoesNotDependOnMonolithOrOtherServiceImplementationProjects() throws IOException {
        var modulePathFromRoot = Path.of("analysis-orchestrator-service/build.gradle.kts");
        var buildFilePath = Files.exists(modulePathFromRoot) ? modulePathFromRoot : Path.of("build.gradle.kts");
        var buildFile = Files.readString(buildFilePath);

        assertFalse(buildFile.contains("project("));
    }

    @Test
    void productiveServiceBuildFilesDoNotDependOnSharedDomainApplicationOrServiceProjects() throws IOException {
        var root = repositoryRoot();
        var violations = new ArrayList<String>();

        try (var serviceDirectories = Files.list(root)) {
            serviceDirectories
                .filter(Files::isDirectory)
                .filter(serviceDirectory -> !NON_PRODUCTIVE_ROOTS.contains(serviceDirectory.getFileName().toString()))
                .map(serviceDirectory -> serviceDirectory.resolve("build.gradle.kts"))
                .filter(Files::isRegularFile)
                .sorted()
                .forEach(buildFile -> violations.addAll(buildFileViolations(root, buildFile)));
        }

        assertTrue(
            violations.isEmpty(),
            () -> "Productive service build files must not depend on shared domain/application modules or service "
                + "implementation projects:%n%s".formatted(String.join(System.lineSeparator(), violations))
        );
    }

    private static Path repositoryRoot() {
        var current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("settings.gradle.kts"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Repository root with settings.gradle.kts was not found.");
    }

    private static List<String> buildFileViolations(Path root, Path buildFile) {
        try {
            var content = Files.readString(buildFile);
            var relativePath = root.relativize(buildFile).toString();
            var violations = new ArrayList<String>();
            if (SHARED_DOMAIN_OR_APPLICATION_MODULE.matcher(content).find()) {
                violations.add(relativePath + " references forensic-analytics-domain/application");
            }
            if (SERVICE_IMPLEMENTATION_MODULE.matcher(content).find()) {
                violations.add(relativePath + " references another service implementation project");
            }
            return violations;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
