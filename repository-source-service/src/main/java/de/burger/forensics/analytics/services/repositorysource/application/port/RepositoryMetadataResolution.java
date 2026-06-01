package de.burger.forensics.analytics.services.repositorysource.application.port;

import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.Diagnostic;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryIdentity;

import java.util.List;
import java.util.Objects;

public record RepositoryMetadataResolution(
    RepositoryIdentity repository,
    boolean defaultBranchResolved,
    List<String> repositoryBranches,
    List<Diagnostic> diagnostics
) {
    public RepositoryMetadataResolution(
        RepositoryIdentity repository,
        boolean defaultBranchResolved,
        List<Diagnostic> diagnostics
    ) {
        this(repository, defaultBranchResolved, defaultBranchResolved ? List.of(repository.defaultBranch()) : List.of(), diagnostics);
    }

    public RepositoryMetadataResolution {
        Objects.requireNonNull(repository, "repository identity must not be null");
        repositoryBranches = List.copyOf(Objects.requireNonNullElse(repositoryBranches, List.<String>of()).stream()
            .filter(branch -> branch != null && !branch.isBlank())
            .distinct()
            .toList());
        diagnostics = List.copyOf(Objects.requireNonNullElse(diagnostics, List.of()));
    }
}
