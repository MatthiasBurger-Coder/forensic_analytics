package de.burger.forensics.analytics.services.joerncpganalysis.adapter.out.filesystem;

import de.burger.forensics.analytics.services.joerncpganalysis.application.port.JoernWorkspacePort;
import de.burger.forensics.analytics.services.joerncpganalysis.application.port.ResolvedJoernWorkspace;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalyzeJoernCpgCommand;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
        if (!Files.isDirectory(workspacePath)) {
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
                if (!Files.isDirectory(sourceRootPath)) {
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
            return stream
                .filter(Files::isRegularFile)
                .mapToLong(FileSystemJoernWorkspaceAdapter::size)
                .sum();
        } catch (IOException error) {
            throw new UncheckedIOException("Failed to inspect source workspace.", error);
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
