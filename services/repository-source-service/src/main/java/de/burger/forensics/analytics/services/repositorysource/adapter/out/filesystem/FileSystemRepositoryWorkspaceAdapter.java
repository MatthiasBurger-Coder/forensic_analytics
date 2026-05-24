package de.burger.forensics.analytics.services.repositorysource.adapter.out.filesystem;

import de.burger.forensics.analytics.services.repositorysource.application.port.PreparedWorkspace;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryWorkspacePort;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceBranchId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspacePolicy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
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
    private final Map<BranchWorkspaceKey, Path> branchWorkspaces = new HashMap<>();

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
    public synchronized PreparedWorkspace prepareBranchCheckout(
        WorkspaceId workspaceId,
        WorkspaceBranchId workspaceBranchId,
        WorkspacePolicy policy
    ) {
        try {
            var root = ensuredRoot();
            var workspace = createDirectoryInsideRoot(root, root.resolve(workspaceId.value()));
            var branches = createDirectoryInsideRoot(root, workspace.resolve("branches"));
            var branchWorkspace = createDirectoryInsideRoot(root, branches.resolve(workspaceBranchId.value()));
            workspaces.putIfAbsent(workspaceId, workspace);
            branchWorkspaces.put(new BranchWorkspaceKey(workspaceId, workspaceBranchId), branchWorkspace);
            return new PreparedWorkspace(workspaceId, branchWorkspace);
        } catch (IOException error) {
            throw new IllegalStateException("Failed to prepare repository branch workspace", error);
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
            var checked = requireExistingInsideRoot(root, workspace);
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

    private static Path createDirectoryInsideRoot(Path root, Path candidate) throws IOException {
        var normalized = requireInsideRoot(root, candidate);
        if (Files.isSymbolicLink(normalized)) {
            throw new IllegalStateException("Workspace path escaped configured root");
        }
        Files.createDirectories(normalized);
        return requireInsideRoot(root, normalized.toRealPath(LinkOption.NOFOLLOW_LINKS));
    }

    private static Path requireExistingInsideRoot(Path root, Path candidate) throws IOException {
        if (!Files.exists(candidate, LinkOption.NOFOLLOW_LINKS)) {
            return requireInsideRoot(root, candidate);
        }
        return requireInsideRoot(root, candidate.toRealPath(LinkOption.NOFOLLOW_LINKS));
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

    @Override
    public synchronized void cleanupBranchCheckout(WorkspaceId workspaceId, WorkspaceBranchId workspaceBranchId) {
        var branchWorkspace = branchWorkspaces.remove(new BranchWorkspaceKey(workspaceId, workspaceBranchId));
        if (branchWorkspace == null) {
            return;
        }
        try {
            var root = ensuredRoot();
            var checked = requireExistingInsideRoot(root, branchWorkspace);
            if (checked.equals(root)) {
                throw new IllegalStateException("Refusing to clean workspace root");
            }
            deleteRecursively(checked);
        } catch (IOException error) {
            throw new IllegalStateException("Failed to clean repository branch workspace", error);
        }
    }

    private record BranchWorkspaceKey(WorkspaceId workspaceId, WorkspaceBranchId workspaceBranchId) {
    }
}
