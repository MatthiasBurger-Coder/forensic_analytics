package de.burger.forensics.analytics.adapter.repository.source;

import de.burger.forensics.analytics.application.ingestion.RepositoryAnalysisIngestionException;
import de.burger.forensics.analytics.application.ingestion.command.WorkspacePreparationRequest;
import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.workspace.PreparedWorkspace;
import de.burger.forensics.analytics.domain.workspace.WorkspaceCleanupPolicy;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceLease;
import de.burger.forensics.analytics.domain.workspace.WorkspacePath;
import de.burger.forensics.analytics.domain.workspace.WorkspacePolicy;
import de.burger.forensics.analytics.domain.workspace.WorkspacePreparationStatus;
import de.burger.forensics.analytics.observability.OperationLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileSystemWorkspacePreparationAdapterTest {
    @TempDir
    Path tempDir;

    @Test
    void createsWorkspaceUnderConfiguredRootAndCleansItExplicitly() throws Exception {
        var workspaceRoot = tempDir.resolve("workspaces");
        var adapter = new FileSystemWorkspacePreparationAdapter(workspaceRoot);

        var workspace = adapter.prepare(new WorkspacePreparationRequest(
            new AnalysisRunId("analysis-1"),
            workspacePolicy()
        ));

        var workspacePath = Path.of(workspace.path().value());
        assertEquals(new WorkspaceId("workspace-analysis-1"), workspace.workspaceId());
        assertEquals(WorkspacePreparationStatus.READY, workspace.status());
        assertTrue(workspacePath.startsWith(workspaceRoot.toAbsolutePath().normalize()));
        assertTrue(Files.isDirectory(workspacePath));

        Files.writeString(workspacePath.resolve("checkout-marker.txt"), "fixture", StandardCharsets.UTF_8);

        var cleaned = adapter.cleanup(workspace);

        assertEquals(WorkspacePreparationStatus.CLEANED, cleaned.status());
        assertEquals(WorkspacePreparationStatus.CLEANED, cleaned.lease().status());
        assertFalse(Files.exists(workspacePath));
        assertTrue(Files.isDirectory(workspaceRoot));
    }

    @Test
    void logsWorkspacePreparationLifecycle() {
        var logger = new RecordingOperationLogger();
        var adapter = new FileSystemWorkspacePreparationAdapter(tempDir.resolve("workspaces"), logger);

        adapter.prepare(new WorkspacePreparationRequest(new AnalysisRunId("analysis-log"), workspacePolicy()));

        assertEquals(
            List.of("started:adapter.repository-source.workspace-prepare", "succeeded:adapter.repository-source.workspace-prepare"),
            logger.events()
        );
    }

    @Test
    void refusesCleanupOutsideConfiguredRootOrAtRootItself() throws Exception {
        var workspaceRoot = Files.createDirectories(tempDir.resolve("workspaces"));
        var adapter = new FileSystemWorkspacePreparationAdapter(workspaceRoot);
        var outside = Files.createDirectories(tempDir.resolve("outside-workspace"));

        assertThrows(
            RepositoryAnalysisIngestionException.class,
            () -> adapter.cleanup(preparedWorkspace(new WorkspacePath(outside.toString())))
        );
        assertThrows(
            RepositoryAnalysisIngestionException.class,
            () -> adapter.cleanup(preparedWorkspace(new WorkspacePath(workspaceRoot.toString())))
        );
        assertTrue(Files.isDirectory(workspaceRoot));
        assertTrue(Files.isDirectory(outside));
    }

    @Test
    void zeroTimeoutCreatesOpenEndedLeaseAndMissingCleanupTargetIsNoop() throws Exception {
        var workspaceRoot = tempDir.resolve("workspaces");
        var adapter = new FileSystemWorkspacePreparationAdapter(workspaceRoot);

        var workspace = adapter.prepare(new WorkspacePreparationRequest(
            new AnalysisRunId("analysis-2"),
            new WorkspacePolicy(true, false, false, false, Duration.ZERO, 0L, WorkspaceCleanupPolicy.DELETE_ON_COMPLETION)
        ));
        var missingWorkspace = preparedWorkspace(new WorkspacePath(workspaceRoot.resolve("missing-workspace").toString()));

        assertEquals(Optional.empty(), workspace.lease().expiresAt());
        assertEquals(WorkspacePreparationStatus.CLEANED, adapter.cleanup(missingWorkspace).status());
        assertTrue(Files.isDirectory(workspaceRoot));
    }

    private static WorkspacePolicy workspacePolicy() {
        return new WorkspacePolicy(
            true,
            false,
            false,
            false,
            Duration.ofSeconds(60),
            0L,
            WorkspaceCleanupPolicy.DELETE_ON_COMPLETION
        );
    }

    private static PreparedWorkspace preparedWorkspace(WorkspacePath workspacePath) {
        return new PreparedWorkspace(
            new WorkspaceId("workspace-1"),
            WorkspacePreparationStatus.READY,
            workspacePath,
            new WorkspaceLease("analysis-1", Instant.parse("2026-05-14T12:00:00Z"), Optional.empty(), WorkspacePreparationStatus.READY),
            List.of("fixture workspace")
        );
    }

    private static final class RecordingOperationLogger implements OperationLogger {
        private final List<String> events = new ArrayList<>();

        @Override
        public void started(String operation) {
            events.add("started:" + operation);
        }

        @Override
        public void succeeded(String operation, long durationMillis) {
            events.add("succeeded:" + operation);
        }

        @Override
        public void failed(String operation, long durationMillis, Throwable error) {
            events.add("failed:" + operation);
        }

        private List<String> events() {
            return List.copyOf(events);
        }
    }
}
