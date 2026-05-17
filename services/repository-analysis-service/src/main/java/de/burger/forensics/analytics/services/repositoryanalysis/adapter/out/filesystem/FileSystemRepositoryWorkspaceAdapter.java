package de.burger.forensics.analytics.services.repositoryanalysis.adapter.out.filesystem;

import de.burger.forensics.analytics.services.repositoryanalysis.application.port.PreparedWorkspace;
import de.burger.forensics.analytics.services.repositoryanalysis.application.port.RepositoryWorkspacePort;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.WorkspaceId;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.WorkspacePolicy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static java.nio.file.FileVisitResult.CONTINUE;

public final class FileSystemRepositoryWorkspaceAdapter implements RepositoryWorkspacePort {
    private final Path configuredRoot;
    private final Map<WorkspaceId, Path> workspaces = new HashMap<>();

    public FileSystemRepositoryWorkspaceAdapter(Path configuredRoot) {
        this(configuredRoot, new HashMap<>());
    }

    FileSystemRepositoryWorkspaceAdapter(Path configuredRoot, Map<WorkspaceId, Path> workspaces) {
        this.configuredRoot = Objects.requireNonNull(configuredRoot, "workspace root must not be null");
        this.workspaces.putAll(Objects.requireNonNull(workspaces, "workspaces must not be null"));
    }

    @Override
    public synchronized PreparedWorkspace prepare(AnalysisRunId analysisRunId, WorkspacePolicy policy) {
        try {
            var root = ensuredRoot();
            var workspaceId = new WorkspaceId("workspace-" + UUID.randomUUID());
            var workspace = Files.createDirectory(root.resolve(workspaceId.value()));
            var checked = requireInsideRoot(root, workspace.toRealPath());
            workspaces.put(workspaceId, checked);
            return new PreparedWorkspace(workspaceId, checked);
        } catch (IOException error) {
            throw new IllegalStateException("Failed to prepare repository workspace", error);
        }
    }

    @Override
    public synchronized void cleanup(WorkspaceId workspaceId) {
        var workspace = workspaces.remove(workspaceId);
        if (workspace == null) {
            return;
        }
        try {
            var root = ensuredRoot();
            var checked = requireInsideRoot(root, workspace.toAbsolutePath().normalize());
            if (checked.equals(root)) {
                throw new IllegalStateException("Refusing to clean workspace root");
            }
            deleteRecursively(checked);
        } catch (IOException error) {
            throw new IllegalStateException("Failed to clean repository workspace", error);
        }
    }

    private Path ensuredRoot() throws IOException {
        Files.createDirectories(configuredRoot);
        return configuredRoot.toRealPath();
    }

    private static Path requireInsideRoot(Path root, Path candidate) {
        var normalized = candidate.toAbsolutePath().normalize();
        if (!normalized.startsWith(root)) {
            throw new IllegalStateException("Workspace path escaped configured root");
        }
        return normalized;
    }

    private static void deleteRecursively(Path target) throws IOException {
        if (!Files.exists(target)) {
            return;
        }
        Files.walkFileTree(target, new SimpleFileVisitor<>() {
            @Override
            public java.nio.file.FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return CONTINUE;
            }

            @Override
            public java.nio.file.FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                Files.deleteIfExists(dir);
                return CONTINUE;
            }
        });
    }
}
