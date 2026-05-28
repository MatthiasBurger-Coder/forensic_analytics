package de.burger.forensics.analytics.services.joernanalysis.adapter.out.filesystem;

import de.burger.forensics.analytics.services.joernanalysis.application.port.JoernWorkspacePort;
import de.burger.forensics.analytics.services.joernanalysis.application.port.ResolvedJoernWorkspace;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalyzeJoernCpgCommand;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Objects;

public final class FileSystemJoernWorkspaceAdapter implements JoernWorkspacePort {
    private final Path workspaceRoot;

    public FileSystemJoernWorkspaceAdapter(Path workspaceRoot) {
        this.workspaceRoot = Objects.requireNonNull(workspaceRoot, "workspace root must not be null")
            .toAbsolutePath()
            .normalize();
    }

    @Override
    public ResolvedJoernWorkspace resolve(AnalyzeJoernCpgCommand command) {
        var workspace = command.workspace();
        var workspacePath = workspaceRoot.resolve(workspace.workspaceId()).normalize();
        if (!workspacePath.startsWith(workspaceRoot)) {
            throw new IllegalArgumentException("workspace id resolves outside service workspace root");
        }
        if (!isDirectoryWithoutLinks(workspacePath)) {
            throw new IllegalArgumentException("source workspace is not available");
        }
        var sourceRootPaths = workspace.sourceRoots().stream()
            .map(sourceRoot -> {
                if (!"java".equals(sourceRoot.language())) {
                    throw new IllegalArgumentException("Joern CPG analysis currently supports Java source roots only");
                }
                var sourceRootPath = workspacePath.resolve(sourceRoot.relativePath()).normalize();
                if (!sourceRootPath.startsWith(workspacePath)) {
                    throw new IllegalArgumentException("source root resolves outside workspace");
                }
                if (!isDirectoryWithoutLinks(sourceRootPath)) {
                    throw new IllegalArgumentException("source root is not available");
                }
                return sourceRootPath;
            })
            .toList();
        return new ResolvedJoernWorkspace(
            command.metadata().sourceSnapshotId(),
            workspace.workspaceId(),
            workspacePath,
            sourceRootPaths,
            directoryBytes(workspacePath)
        );
    }

    private static long directoryBytes(Path directory) {
        try (var stream = Files.walk(directory)) {
            var visited = new HashSet<Path>();
            return stream
                .peek(path -> validateArchiveSafePath(directory, path, visited))
                .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                .mapToLong(FileSystemJoernWorkspaceAdapter::size)
                .sum();
        } catch (IOException error) {
            throw new UncheckedIOException("Failed to inspect source workspace.", error);
        }
    }

    private static boolean isDirectoryWithoutLinks(Path path) {
        try {
            var attributes = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            return attributes.isDirectory();
        } catch (NoSuchFileException error) {
            return false;
        } catch (IOException error) {
            throw new UncheckedIOException("Failed to inspect source workspace.", error);
        }
    }

    private static void validateArchiveSafePath(Path root, Path path, HashSet<Path> visited) {
        try {
            var normalized = path.toAbsolutePath().normalize();
            if (!normalized.startsWith(root.toAbsolutePath().normalize()) || !visited.add(normalized)) {
                throw new IllegalArgumentException("source workspace contains an unsafe duplicate or escaped path");
            }
            var attributes = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (attributes.isSymbolicLink()) {
                throw new IllegalArgumentException("source workspace must not contain symbolic links");
            }
            if (!attributes.isDirectory() && !attributes.isRegularFile()) {
                throw new IllegalArgumentException("source workspace must not contain special files");
            }
            if (attributes.isRegularFile() && hardLinkCount(path) > 1) {
                throw new IllegalArgumentException("source workspace must not contain hard links");
            }
        } catch (IOException error) {
            throw new UncheckedIOException("Failed to inspect source workspace.", error);
        }
    }

    private static long hardLinkCount(Path path) throws IOException {
        try {
            var links = Files.getAttribute(path, "unix:nlink", LinkOption.NOFOLLOW_LINKS);
            return links instanceof Number number ? number.longValue() : 1;
        } catch (UnsupportedOperationException error) {
            return 1;
        }
    }

    private static long size(Path file) {
        try {
            return Files.size(file);
        } catch (IOException error) {
            throw new UncheckedIOException("Failed to inspect source file size.", error);
        }
    }
}
