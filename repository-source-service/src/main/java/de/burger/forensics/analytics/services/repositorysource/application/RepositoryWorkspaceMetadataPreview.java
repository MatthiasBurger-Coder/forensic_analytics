package de.burger.forensics.analytics.services.repositorysource.application;

import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.Diagnostic;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryIdentity;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceTitle;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record RepositoryWorkspaceMetadataPreview(
    RepositoryIdentity repository,
    WorkspaceTitle workspaceTitle,
    List<String> repositoryBranches,
    List<Diagnostic> diagnostics,
    Map<String, String> safeAttributes
) {
    public RepositoryWorkspaceMetadataPreview {
        Objects.requireNonNull(repository, "repository identity must not be null");
        Objects.requireNonNull(workspaceTitle, "workspace title must not be null");
        repositoryBranches = List.copyOf(Objects.requireNonNullElse(repositoryBranches, List.<String>of()).stream()
            .filter(branch -> branch != null && !branch.isBlank())
            .distinct()
            .toList());
        diagnostics = List.copyOf(Objects.requireNonNullElse(diagnostics, List.of()));
        safeAttributes = de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.safeAttributes(safeAttributes);
    }
}
