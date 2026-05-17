package de.burger.forensics.analytics.services.joerncpganalysis.application.port;

import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.SourceSnapshotId;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record ResolvedJoernWorkspace(
    SourceSnapshotId sourceSnapshotId,
    String workspaceId,
    Path workspacePath,
    List<Path> sourceRootPaths,
    long workspaceBytes
) {
    public ResolvedJoernWorkspace {
        sourceSnapshotId = Objects.requireNonNull(sourceSnapshotId, "source snapshot id must not be null");
        if (workspaceId == null || workspaceId.isBlank()) {
            throw new IllegalArgumentException("workspace id must not be blank");
        }
        workspacePath = Objects.requireNonNull(workspacePath, "workspace path must not be null").toAbsolutePath().normalize();
        sourceRootPaths = List.copyOf(Objects.requireNonNull(sourceRootPaths, "source root paths must not be null"));
        if (sourceRootPaths.isEmpty()) {
            throw new IllegalArgumentException("source root paths must not be empty");
        }
        if (workspaceBytes < 0) {
            throw new IllegalArgumentException("workspace bytes must not be negative");
        }
    }
}
