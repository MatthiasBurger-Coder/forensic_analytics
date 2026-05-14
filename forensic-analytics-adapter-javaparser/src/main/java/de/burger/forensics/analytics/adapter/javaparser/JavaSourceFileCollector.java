package de.burger.forensics.analytics.adapter.javaparser;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class JavaSourceFileCollector {
    private static final int MAX_SCAN_DEPTH = 64;
    private static final Set<String> SKIPPED_DIRECTORY_NAMES = Set.of(".git", ".gradle", ".idea", "build", "target", "out");
    private static final List<List<String>> DEFAULT_EXCLUDED_SOURCE_ROOT_SEGMENTS = List.of(
        List.of("src", "test", "java"),
        List.of("src", "integrationTest", "java")
    );

    List<Path> collect(Path sourceRoot) {
        var normalizedRoot = sourceRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalizedRoot)) {
            throw new IllegalArgumentException("source root must point to an existing directory: " + normalizedRoot);
        }
        var sourceFiles = new ArrayList<Path>();
        try {
            Files.walkFileTree(
                normalizedRoot,
                EnumSet.noneOf(FileVisitOption.class),
                MAX_SCAN_DEPTH,
                new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) {
                        return shouldSkipDirectory(normalizedRoot, directory.toAbsolutePath().normalize())
                            ? FileVisitResult.SKIP_SUBTREE
                            : FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                        if (attributes.isRegularFile() && file.getFileName().toString().endsWith(".java")) {
                            sourceFiles.add(file.toAbsolutePath().normalize());
                        }
                        return FileVisitResult.CONTINUE;
                    }
                }
            );
            return sourceFiles.stream().sorted().toList();
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to collect Java source files below " + normalizedRoot + ".", exception);
        }
    }

    private static boolean shouldSkipDirectory(Path sourceRoot, Path directory) {
        if (directory.equals(sourceRoot)) {
            return false;
        }
        var directoryName = fileName(directory).toLowerCase(Locale.ROOT);
        return SKIPPED_DIRECTORY_NAMES.contains(directoryName) || isDefaultExcludedSourceRoot(directory);
    }

    private static boolean isDefaultExcludedSourceRoot(Path directory) {
        return DEFAULT_EXCLUDED_SOURCE_ROOT_SEGMENTS.stream()
            .anyMatch(segments -> endsWithSegments(directory, segments));
    }

    private static boolean endsWithSegments(Path path, List<String> suffix) {
        if (path.getNameCount() < suffix.size()) {
            return false;
        }
        var start = path.getNameCount() - suffix.size();
        for (var index = 0; index < suffix.size(); index++) {
            if (!path.getName(start + index).toString().equalsIgnoreCase(suffix.get(index))) {
                return false;
            }
        }
        return true;
    }

    private static String fileName(Path path) {
        var fileName = path.getFileName();
        return fileName == null ? "" : fileName.toString();
    }
}
