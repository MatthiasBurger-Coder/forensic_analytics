package de.burger.forensics.analytics.services.analysisorchestrator.quality;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

class AnalysisOrchestratorServiceBuildIsolationTest {
    @Test
    void buildDoesNotDependOnMonolithOrOtherServiceImplementationProjects() throws IOException {
        var modulePathFromRoot = Path.of("services/analysis-orchestrator-service/build.gradle.kts");
        var buildFilePath = Files.exists(modulePathFromRoot) ? modulePathFromRoot : Path.of("build.gradle.kts");
        var buildFile = Files.readString(buildFilePath);

        assertFalse(buildFile.contains("project("));
    }
}
