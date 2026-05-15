package de.burger.forensics.analytics.adapter.repository.source;

import de.burger.forensics.analytics.application.analysis.port.RepositorySourcePort;
import de.burger.forensics.analytics.domain.repository.RepositoryMetadata;
import de.burger.forensics.analytics.domain.repository.RepositorySource;
import de.burger.forensics.analytics.observability.OperationLogger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class LocalRepositorySourceAdapter implements RepositorySourcePort {
    private final Path baseDirectory;
    private final OperationLogger operationLogger;

    public LocalRepositorySourceAdapter(Path baseDirectory) {
        this(baseDirectory, OperationLogger.system(LocalRepositorySourceAdapter.class));
    }

    LocalRepositorySourceAdapter(Path baseDirectory, OperationLogger operationLogger) {
        this.baseDirectory = Objects.requireNonNull(baseDirectory, "baseDirectory must not be null")
            .toAbsolutePath()
            .normalize();
        this.operationLogger = Objects.requireNonNull(operationLogger, "operationLogger must not be null");
    }

    @Override
    public RepositorySource resolve(RepositoryMetadata repositoryMetadata) {
        var verifiedMetadata = Objects.requireNonNull(repositoryMetadata, "repositoryMetadata must not be null");
        return operationLogger.logged("adapter.repository-source.local-resolve", () -> resolveVerified(verifiedMetadata));
    }

    private RepositorySource resolveVerified(RepositoryMetadata repositoryMetadata) {
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
