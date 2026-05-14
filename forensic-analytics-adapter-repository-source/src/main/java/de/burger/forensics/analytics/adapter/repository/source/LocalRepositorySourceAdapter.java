package de.burger.forensics.analytics.adapter.repository.source;

import de.burger.forensics.analytics.application.analysis.port.RepositorySourcePort;
import de.burger.forensics.analytics.domain.repository.RepositoryMetadata;
import de.burger.forensics.analytics.domain.repository.RepositorySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class LocalRepositorySourceAdapter implements RepositorySourcePort {
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
        var sourceRoots = SourceRootDetector.sourceRoots(repositoryDirectory);
        return new RepositorySource(repositoryMetadata, sourceRoots);
    }

    private static void requireExistingDirectory(Path repositoryDirectory) {
        if (!Files.isDirectory(repositoryDirectory)) {
            throw new IllegalArgumentException("repository location must point to an existing directory: " + repositoryDirectory);
        }
    }
}
