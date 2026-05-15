package de.burger.forensics.analytics.adapter.repository.source;

import de.burger.forensics.analytics.application.ingestion.RepositoryAnalysisIngestionException;
import de.burger.forensics.analytics.application.ingestion.command.WorkspacePreparationRequest;
import de.burger.forensics.analytics.application.ingestion.port.WorkspacePreparationPort;
import de.burger.forensics.analytics.domain.workspace.PreparedWorkspace;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceLease;
import de.burger.forensics.analytics.domain.workspace.WorkspacePath;
import de.burger.forensics.analytics.domain.workspace.WorkspacePreparationStatus;
import de.burger.forensics.analytics.observability.OperationLogger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class FileSystemWorkspacePreparationAdapter implements WorkspacePreparationPort {
    private final Path workspaceRoot;
    private final OperationLogger operationLogger;

    public FileSystemWorkspacePreparationAdapter(Path workspaceRoot) {
        this(workspaceRoot, OperationLogger.system(FileSystemWorkspacePreparationAdapter.class));
    }

    FileSystemWorkspacePreparationAdapter(Path workspaceRoot, OperationLogger operationLogger) {
        this.workspaceRoot = Objects.requireNonNull(workspaceRoot, "workspaceRoot must not be null")
            .toAbsolutePath()
            .normalize();
        this.operationLogger = Objects.requireNonNull(operationLogger, "operationLogger must not be null");
    }

    @Override
    public PreparedWorkspace prepare(WorkspacePreparationRequest request) {
        var verifiedRequest = Objects.requireNonNull(request, "request must not be null");
        return operationLogger.logged("adapter.repository-source.workspace-prepare", () -> prepareVerified(verifiedRequest));
    }

    private PreparedWorkspace prepareVerified(WorkspacePreparationRequest request) {
        var workspaceId = new WorkspaceId("workspace-" + request.analysisSessionId().value());
        var workspacePath = confinedWorkspacePath(workspaceId);
        try {
            Files.createDirectories(workspacePath);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create workspace " + workspacePath + ".", e);
        }
        var now = Instant.now();
        var expiresAt = request.policy().timeout().isZero()
            ? Optional.<Instant>empty()
            : Optional.of(now.plus(request.policy().timeout()));
        return new PreparedWorkspace(
            workspaceId,
            WorkspacePreparationStatus.READY,
            new WorkspacePath(workspacePath.toString()),
            new WorkspaceLease(request.analysisSessionId().value(), now, expiresAt, WorkspacePreparationStatus.READY),
            List.of("Workspace created")
        );
    }

    @Override
    public PreparedWorkspace cleanup(PreparedWorkspace workspace) {
        var verifiedWorkspace = Objects.requireNonNull(workspace, "workspace must not be null");
        return operationLogger.logged("adapter.repository-source.workspace-cleanup", () -> cleanupVerified(verifiedWorkspace));
    }

    private PreparedWorkspace cleanupVerified(PreparedWorkspace workspace) {
        var workspacePath = confinedCleanupPath(Path.of(workspace.path().value()));
        try {
            deleteRecursively(workspacePath);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to clean workspace " + workspacePath + ".", e);
        }
        return new PreparedWorkspace(
            workspace.workspaceId(),
            WorkspacePreparationStatus.CLEANED,
            workspace.path(),
            new WorkspaceLease(
                workspace.lease().owner(),
                workspace.lease().startedAt(),
                workspace.lease().expiresAt(),
                WorkspacePreparationStatus.CLEANED
            ),
            List.of("Workspace cleaned")
        );
    }

    private Path confinedWorkspacePath(WorkspaceId workspaceId) {
        return confinedPath(workspaceRoot.resolve(workspaceId.value()));
    }

    private Path confinedCleanupPath(Path candidate) {
        var normalized = confinedPath(candidate);
        if (workspaceRoot.equals(normalized)) {
            throw new RepositoryAnalysisIngestionException("Workspace cleanup target must not be the configured workspace root");
        }
        return normalized;
    }

    private Path confinedPath(Path candidate) {
        var normalized = candidate.toAbsolutePath().normalize();
        if (!normalized.startsWith(workspaceRoot)) {
            throw new RepositoryAnalysisIngestionException("Workspace path escapes the configured workspace root");
        }
        return normalized;
    }

    private static void deleteRecursively(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            var ordered = paths.sorted(Comparator.reverseOrder()).toList();
            for (var path : ordered) {
                Files.deleteIfExists(path);
            }
        }
    }
}
