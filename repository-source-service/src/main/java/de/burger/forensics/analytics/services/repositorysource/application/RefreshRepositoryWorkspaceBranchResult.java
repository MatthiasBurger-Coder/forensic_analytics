package de.burger.forensics.analytics.services.repositorysource.application;

import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.Diagnostic;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceBranch;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.SourceSnapshotId;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record RefreshRepositoryWorkspaceBranchResult(
    RepositoryWorkspaceBranch branch,
    boolean changed,
    String previousCommit,
    SourceSnapshotId previousSourceSnapshotId,
    List<Diagnostic> diagnostics,
    Map<String, String> safeAttributes
) {
    public RefreshRepositoryWorkspaceBranchResult {
        Objects.requireNonNull(branch, "repository workspace branch must not be null");
        previousCommit = previousCommit == null ? "" : previousCommit;
        diagnostics = List.copyOf(Objects.requireNonNullElse(diagnostics, List.of()));
        safeAttributes = de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.safeAttributes(safeAttributes);
    }
}
