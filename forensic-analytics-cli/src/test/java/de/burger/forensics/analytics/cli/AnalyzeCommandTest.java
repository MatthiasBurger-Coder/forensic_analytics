package de.burger.forensics.analytics.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnalyzeCommandTest {
    @TempDir
    Path tempDir;

    @Test
    void createsApplicationCommandFromLocalPath() {
        var command = new AnalyzeCommand(
            tempDir.resolve("project").toString(),
            "baseline",
            tempDir.resolve("out"),
            JoernMode.OFF
        ).toRunRepositoryAnalysisCommand();

        assertEquals("project", command.repositoryMetadata().projectId());
        assertEquals("baseline", command.analysisProfile());
        assertEquals("UNKNOWN", command.repositoryMetadata().branchName());
        assertEquals("UNKNOWN", command.repositoryMetadata().commitHash());
    }

    @Test
    void createsProjectIdFromFileUri() {
        var repository = tempDir.resolve("project").toAbsolutePath().normalize();

        var command = new AnalyzeCommand(
            repository.toUri().toString(),
            "baseline",
            tempDir.resolve("out"),
            JoernMode.DOCKER
        ).toRunRepositoryAnalysisCommand();

        assertEquals("project", command.repositoryMetadata().projectId());
    }

    @Test
    void fallsBackToGenericProjectIdWhenLocationCannotBeParsedAsPath() {
        var command = new AnalyzeCommand(
            "file://[invalid",
            "baseline",
            tempDir.resolve("out"),
            JoernMode.OFF
        ).toRunRepositoryAnalysisCommand();

        assertEquals("repository", command.repositoryMetadata().projectId());
    }

    @Test
    void requiresValidValues() {
        assertThrows(IllegalArgumentException.class, () -> new AnalyzeCommand(null, "baseline", tempDir, JoernMode.OFF));
        assertThrows(IllegalArgumentException.class, () -> new AnalyzeCommand("", "baseline", tempDir, JoernMode.OFF));
        assertThrows(IllegalArgumentException.class, () -> new AnalyzeCommand("repo", null, tempDir, JoernMode.OFF));
        assertThrows(IllegalArgumentException.class, () -> new AnalyzeCommand("repo", "", tempDir, JoernMode.OFF));
        assertThrows(NullPointerException.class, () -> new AnalyzeCommand("repo", "baseline", null, JoernMode.OFF));
        assertThrows(NullPointerException.class, () -> new AnalyzeCommand("repo", "baseline", tempDir, null));
    }
}
