package de.burger.forensics.analytics.services.joerncpganalysis.adapter.out.filesystem;

import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisJobId;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalyzeJoernCpgCommand;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.JoernCpgPolicy;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.RequestMetadata;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.SourceRoot;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.SourceSnapshotId;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.SourceWorkspace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileSystemJoernWorkspaceAdapterTest {
    @TempDir
    Path tempDir;

    @Test
    void resolvesOpaqueWorkspaceAndCountsWorkspaceBytes() throws Exception {
        var sourceRoot = tempDir.resolve("workspace-1/src/main/java");
        Files.createDirectories(sourceRoot);
        Files.writeString(sourceRoot.resolve("App.java"), "class App {}\n");

        var resolved = new FileSystemJoernWorkspaceAdapter(tempDir).resolve(command("workspace-1", "src/main/java", "JAVA"));

        assertEquals(new SourceSnapshotId("snapshot-1"), resolved.sourceSnapshotId());
        assertEquals("workspace-1", resolved.workspaceId());
        assertEquals(tempDir.resolve("workspace-1").toAbsolutePath().normalize(), resolved.workspacePath());
        assertEquals(List.of(sourceRoot.toAbsolutePath().normalize()), resolved.sourceRootPaths());
        assertTrue(resolved.workspaceBytes() > 0);
    }

    @Test
    void rejectsUnavailableWorkspaceUnsupportedLanguagesAndMissingRoots() throws Exception {
        var sourceRoot = tempDir.resolve("workspace-1/src/main/java");
        Files.createDirectories(sourceRoot);

        var adapter = new FileSystemJoernWorkspaceAdapter(tempDir);

        assertThrows(IllegalArgumentException.class, () -> adapter.resolve(command("missing-workspace", "src/main/java", "java")));
        assertThrows(IllegalArgumentException.class, () -> adapter.resolve(command("workspace-1", "src/test/java", "java")));
        assertThrows(IllegalArgumentException.class, () -> adapter.resolve(command("workspace-1", "src/main/java", "kotlin")));
    }

    private static AnalyzeJoernCpgCommand command(String workspaceId, String sourceRoot, String language) {
        return new AnalyzeJoernCpgCommand(
            new RequestMetadata(
                "request-1",
                "idempotency-1",
                "joern-cpg-analysis-v1",
                "correlation-1",
                new AnalysisRunId("run-1"),
                new AnalysisJobId("job-1"),
                new SourceSnapshotId("snapshot-1"),
                "joern-cpg-analysis-service-test",
                Map.of("tenant", "demo")
            ),
            new JoernCpgPolicy(
                2,
                1_000_000,
                1_000_000,
                60,
                "ghcr.io/joernio/joern@sha256:" + "a".repeat(64),
                "queries-v1",
                true,
                false,
                false
            ),
            new SourceWorkspace(workspaceId, List.of(new SourceRoot(sourceRoot, language)), List.of())
        );
    }
}
