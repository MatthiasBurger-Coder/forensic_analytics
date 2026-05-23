package de.burger.forensics.analytics.services.repositorysource.adapter.out.git;

import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.SourceRoot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class SourceRootDetector {
    private static final Set<String> IGNORED_DIRECTORY_NAMES = Set.of(
        ".git",
        ".gradle",
        ".idea",
        "build",
        "target"
    );

    public List<SourceRoot> detect(Path repositoryRoot) {
        try (var stream = Files.walk(repositoryRoot, 6)) {
            var roots = stream
                .filter(path -> Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))
                .filter(path -> !containsIgnoredSegment(repositoryRoot, path))
                .filter(path -> path.endsWith(Path.of("src", "main", "java")))
                .map(repositoryRoot::relativize)
                .map(Path::toString)
                .map(path -> path.replace('\\', '/'))
                .sorted()
                .map(path -> new SourceRoot(path, "java"))
                .toList();
            if (!roots.isEmpty()) {
                return roots;
            }
            return fallbackRoot(repositoryRoot);
        } catch (IOException error) {
            throw new IllegalStateException("Failed to detect source roots", error);
        }
    }

    private static boolean containsIgnoredSegment(Path repositoryRoot, Path path) {
        var relative = repositoryRoot.relativize(path);
        for (Path segment : relative) {
            if (IGNORED_DIRECTORY_NAMES.contains(segment.toString())) {
                return true;
            }
        }
        return false;
    }

    private static List<SourceRoot> fallbackRoot(Path repositoryRoot) {
        var roots = new ArrayList<SourceRoot>();
        roots.add(new SourceRoot(".", "unknown"));
        return roots.stream()
            .sorted(Comparator.comparing(SourceRoot::relativePath))
            .toList();
    }
}
