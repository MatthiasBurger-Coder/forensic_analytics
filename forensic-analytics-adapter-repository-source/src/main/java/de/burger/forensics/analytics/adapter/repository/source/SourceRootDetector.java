package de.burger.forensics.analytics.adapter.repository.source;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

final class SourceRootDetector {
    private static final Set<String> IGNORED_DIRECTORY_NAMES = Set.of(".git", ".gradle", ".idea", "build", "target");

    private SourceRootDetector() {
    }

    static List<String> sourceRoots(Path repositoryDirectory) {
        var normalizedRepositoryDirectory = repositoryDirectory.toAbsolutePath().normalize();
        var discoveredRoots = discoverMainJavaSourceRoots(normalizedRepositoryDirectory).stream()
            .map(Path::toString)
            .toList();
        if (discoveredRoots.isEmpty()) {
            return List.of(normalizedRepositoryDirectory.toString());
        }
        return discoveredRoots;
    }

    private static List<Path> discoverMainJavaSourceRoots(Path repositoryDirectory) {
        var sourceRoots = new ArrayList<Path>();
        try {
            Files.walkFileTree(repositoryDirectory, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) {
                    var normalizedDirectory = directory.toAbsolutePath().normalize();
                    if (!repositoryDirectory.equals(normalizedDirectory) && isIgnored(repositoryDirectory, normalizedDirectory)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    if (isMainJavaSourceRoot(normalizedDirectory)) {
                        sourceRoots.add(normalizedDirectory);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            return sourceRoots.stream()
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
