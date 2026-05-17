package de.burger.forensics.analytics.services.repositoryanalysis.adapter.out.git;

import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceRoot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SourceRootDetector {
    public List<SourceRoot> detect(Path repositoryRoot) {
        try (var stream = Files.walk(repositoryRoot, 6)) {
            var roots = stream
                .filter(Files::isDirectory)
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

    private static List<SourceRoot> fallbackRoot(Path repositoryRoot) {
        var roots = new ArrayList<SourceRoot>();
        roots.add(new SourceRoot(".", "unknown"));
        return roots.stream()
            .sorted(Comparator.comparing(SourceRoot::relativePath))
            .toList();
    }
}
