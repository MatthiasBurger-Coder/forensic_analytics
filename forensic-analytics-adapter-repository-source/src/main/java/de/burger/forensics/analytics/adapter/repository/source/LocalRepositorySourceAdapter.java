package de.burger.forensics.analytics.adapter.repository.source;

import de.burger.forensics.analytics.application.analysis.port.RepositorySourcePort;
import de.burger.forensics.analytics.domain.repository.RepositoryMetadata;
import de.burger.forensics.analytics.domain.repository.RepositorySource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class LocalRepositorySourceAdapter implements RepositorySourcePort {
    private static final Set<String> IGNORED_DIRECTORY_NAMES = Set.of(".git", ".gradle", ".idea", "build", "target");

    private final Path baseDirectory;

    public LocalRepositorySourceAdapter(Path baseDirectory) {
        this.baseDirectory = Objects.requireNonNull(baseDirectory, "baseDirectory must not be null")
            .toAbsolutePath()
            .normalize();
    }

    @Override
    public RepositorySource resolve(RepositoryMetadata repositoryMetadata) {
        Objects.requireNonNull(repositoryMetadata, "repositoryMetadata must not be null");
        var repositoryDirectory = LocalRepositoryLocation.resolve(repositoryMetadata.repositoryLocation(), baseDirectory);
        requireExistingDirectory(repositoryDirectory);
        return new RepositorySource(repositoryMetadata, sourceRoots(repositoryDirectory));
    }

    private static void requireExistingDirectory(Path repositoryDirectory) {
        if (!Files.isDirectory(repositoryDirectory)) {
            throw new IllegalArgumentException("repository location must point to an existing directory: " + repositoryDirectory);
        }
    }

    private static List<String> sourceRoots(Path repositoryDirectory) {
        var discoveredRoots = discoverMainJavaSourceRoots(repositoryDirectory).stream()
            .map(Path::toString)
            .toList();
        if (discoveredRoots.isEmpty()) {
            return List.of(repositoryDirectory.toString());
        }
        return discoveredRoots;
    }

    private static List<Path> discoverMainJavaSourceRoots(Path repositoryDirectory) {
        try (var paths = Files.walk(repositoryDirectory)) {
            return paths
                .filter(Files::isDirectory)
                .filter(path -> !isIgnored(repositoryDirectory, path))
                .filter(LocalRepositorySourceAdapter::isMainJavaSourceRoot)
                .map(path -> path.toAbsolutePath().normalize())
                .sorted(Comparator.comparing(Path::toString))
                .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to discover source roots below " + repositoryDirectory + ".", e);
        }
    }

    private static boolean isIgnored(Path repositoryDirectory, Path candidate) {
        var relative = repositoryDirectory.relativize(candidate);
        for (var segment : relative) {
            if (IGNORED_DIRECTORY_NAMES.contains(segment.toString())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMainJavaSourceRoot(Path path) {
        var javaDirectory = path.getFileName();
        var mainDirectory = path.getParent() == null ? null : path.getParent().getFileName();
        var sourceDirectory = path.getParent() == null || path.getParent().getParent() == null
            ? null
            : path.getParent().getParent().getFileName();
        return nameEquals(javaDirectory, "java")
            && nameEquals(mainDirectory, "main")
            && nameEquals(sourceDirectory, "src");
    }

    private static boolean nameEquals(Path path, String expectedName) {
        return path != null && expectedName.equals(path.toString());
    }
}
