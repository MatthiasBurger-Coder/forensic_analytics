package de.burger.forensics.analytics.engine;

import java.util.List;
import java.util.Objects;

public record RepositorySource(String repositoryLocation, List<String> sourceRoots) {
    public RepositorySource {
        Objects.requireNonNull(repositoryLocation, "repositoryLocation must not be null");
        sourceRoots = List.copyOf(Objects.requireNonNull(sourceRoots, "sourceRoots must not be null"));
    }
}
