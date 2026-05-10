package de.burger.forensics.analytics.domain.repository;

import java.util.List;
import java.util.Objects;

public record RepositorySource(RepositoryMetadata metadata, List<String> sourceRoots) {
    public RepositorySource {
        Objects.requireNonNull(metadata, "metadata must not be null");
        sourceRoots = List.copyOf(Objects.requireNonNull(sourceRoots, "sourceRoots must not be null"));
    }
}
