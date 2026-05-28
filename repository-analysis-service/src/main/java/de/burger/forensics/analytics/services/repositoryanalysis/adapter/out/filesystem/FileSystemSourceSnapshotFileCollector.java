package de.burger.forensics.analytics.services.repositoryanalysis.adapter.out.filesystem;

import de.burger.forensics.analytics.services.repositoryanalysis.application.port.SourceSnapshotFileCollectorPort;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceRoot;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceSnapshotHandoffPolicy;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceSnapshotSourceFile;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.WorkspaceId;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.sha256Hex;

public final class FileSystemSourceSnapshotFileCollector implements SourceSnapshotFileCollectorPort {
    private final Path configuredRoot;

    public FileSystemSourceSnapshotFileCollector(Path configuredRoot) {
        this.configuredRoot = Objects.requireNonNull(configuredRoot, "workspace root must not be null");
    }

    @Override
    public List<SourceSnapshotSourceFile> collect(
        WorkspaceId workspaceId,
        List<SourceRoot> sourceRoots,
        SourceSnapshotHandoffPolicy policy
    ) {
        try {
            var root = configuredRoot.toRealPath();
            var repositoryRoot = repositoryRoot(root, workspaceId);
            var files = new ArrayList<SourceSnapshotSourceFile>();
            for (SourceRoot sourceRoot : sourceRoots) {
                collectSourceRoot(repositoryRoot, sourceRoot, files, policy);
            }
            var sortedFiles = files.stream()
                .sorted(Comparator.comparing(SourceSnapshotSourceFile::sourcePath))
                .toList();
            if (sortedFiles.isEmpty()) {
                throw new IllegalStateException("Source snapshot contains no Java source files");
            }
            return sortedFiles;
        } catch (IOException | UncheckedIOException error) {
            throw new IllegalStateException("Source snapshot files could not be collected");
        }
    }

    private static Path repositoryRoot(Path root, WorkspaceId workspaceId) {
        var workspace = root.resolve(workspaceId.value()).normalize();
        var repository = workspace.resolve("repository").normalize();
        if (!repository.startsWith(root) || !Files.isDirectory(repository, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("Repository workspace is not available for source snapshot handoff");
        }
        return repository;
    }

    private static void collectSourceRoot(
        Path repositoryRoot,
        SourceRoot sourceRoot,
        List<SourceSnapshotSourceFile> files,
        SourceSnapshotHandoffPolicy policy
    ) throws IOException {
        if (!"java".equalsIgnoreCase(sourceRoot.language())) {
            return;
        }
        var sourceRootPath = repositoryRoot.resolve(sourceRoot.relativePath()).normalize();
        if (!sourceRootPath.startsWith(repositoryRoot) || !Files.isDirectory(sourceRootPath, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        try (var stream = Files.walk(sourceRootPath)) {
            var sourceFiles = stream
                .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                .filter(path -> path.getFileName().toString().endsWith(".java"))
                .iterator();
            while (sourceFiles.hasNext()) {
                addFile(repositoryRoot, sourceRoot, sourceRootPath, sourceFiles.next(), files, policy);
            }
        }
    }

    private static void addFile(
        Path repositoryRoot,
        SourceRoot sourceRoot,
        Path sourceRootPath,
        Path sourceFile,
        List<SourceSnapshotSourceFile> files,
        SourceSnapshotHandoffPolicy policy
    ) throws IOException {
        if (!sourceFile.normalize().startsWith(repositoryRoot)) {
            throw new IllegalStateException("Source snapshot file escaped repository root");
        }
        if (files.size() >= policy.maxFiles()) {
            throw new IllegalArgumentException("source file count exceeds handoff policy");
        }
        var sizeBytes = Files.size(sourceFile);
        var nextTotal = files.stream()
            .mapToLong(SourceSnapshotSourceFile::sizeBytes)
            .sum() + sizeBytes;
        if (nextTotal > policy.maxSourceBytes()) {
            throw new IllegalArgumentException("source byte count exceeds handoff policy");
        }
        var content = Files.readString(sourceFile, StandardCharsets.UTF_8);
        var relativePath = sourceRootPath.relativize(sourceFile).toString().replace('\\', '/');
        files.add(new SourceSnapshotSourceFile(
            sourceRoot.relativePath(),
            relativePath,
            content,
            sha256Hex(content),
            sizeBytes
        ));
    }
}
