package de.burger.forensics.analytics.persistence.storage;

import de.burger.forensics.analytics.domain.repository.SourceSnapshot;
import de.burger.forensics.analytics.domain.workspace.ProjectStorageArea;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceProject;

import java.nio.file.Path;
import java.util.Objects;

public final class IsolatedProjectStoragePathResolver {
    private final Path rootDirectory;

    public IsolatedProjectStoragePathResolver(Path rootDirectory) {
        this.rootDirectory = Objects.requireNonNull(rootDirectory, "rootDirectory must not be null")
            .toAbsolutePath()
            .normalize();
    }

    public Path projectArea(WorkspaceProject project, ProjectStorageArea area) {
        Objects.requireNonNull(project, "project must not be null");
        Objects.requireNonNull(area, "area must not be null");
        var workspaceSegment = safeSegment(project.workspaceId().value(), "workspace id");
        var projectSegment = safeSegment(project.id().value(), "project id");
        return confined(rootDirectory.resolve("workspaces")
            .resolve(workspaceSegment)
            .resolve("projects")
            .resolve(projectSegment)
            .resolve(area.directoryName()));
    }

    public Path projectFile(WorkspaceProject project, ProjectStorageArea area, String storedFileName) {
        var fileName = safeSegment(storedFileName, "stored file name");
        return confined(projectArea(project, area).resolve(fileName));
    }

    public Path sourceSnapshotArtifact(WorkspaceProject project, SourceSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        return projectFile(project, ProjectStorageArea.EVIDENCE_ORIGINAL, snapshot.sourceArtifact().path());
    }

    public Path sharedArea(WorkspaceId workspaceId) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        var workspaceSegment = safeSegment(workspaceId.value(), "workspace id");
        return confined(rootDirectory.resolve("workspaces").resolve(workspaceSegment).resolve("shared"));
    }

    public Path sharedFile(WorkspaceId workspaceId, String storedFileName) {
        var fileName = safeSegment(storedFileName, "stored file name");
        return confined(sharedArea(workspaceId).resolve(fileName));
    }

    private Path confined(Path candidate) {
        var normalized = candidate.toAbsolutePath().normalize();
        if (!normalized.startsWith(rootDirectory)) {
            throw new IllegalArgumentException("resolved storage path escapes root directory");
        }
        return normalized;
    }

    private static String safeSegment(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        if (".".equals(value) || "..".equals(value) || value.contains("/") || value.contains("\\")) {
            throw new IllegalArgumentException(fieldName + " must be a single path segment");
        }
        return value;
    }
}
