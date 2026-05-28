package de.burger.forensics.analytics.services.repositorysource.application.port;

import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.Diagnostic;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryIdentity;

import java.util.List;
import java.util.Objects;

public record RepositoryMetadataResolution(
    RepositoryIdentity repository,
    boolean defaultBranchResolved,
    List<Diagnostic> diagnostics
) {
    public RepositoryMetadataResolution {
        Objects.requireNonNull(repository, "repository identity must not be null");
        diagnostics = List.copyOf(Objects.requireNonNullElse(diagnostics, List.of()));
    }
}
