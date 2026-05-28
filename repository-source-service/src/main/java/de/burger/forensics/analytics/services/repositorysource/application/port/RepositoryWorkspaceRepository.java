package de.burger.forensics.analytics.services.repositorysource.application.port;

import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.Diagnostic;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryKey;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspace;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceBranch;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceStatus;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceBranchId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RepositoryWorkspaceRepository {
    RepositoryWorkspace save(RepositoryWorkspace workspace);

    List<RepositoryWorkspace> findAll(boolean includeCleaned);

    Optional<RepositoryWorkspace> findById(WorkspaceId workspaceId);

    Optional<RepositoryWorkspace> findByRepositoryKey(RepositoryKey repositoryKey);

    Optional<RepositoryWorkspaceBranch> findBranch(WorkspaceId workspaceId, WorkspaceBranchId workspaceBranchId);

    RepositoryWorkspace updateWorkspaceStatus(
        WorkspaceId workspaceId,
        RepositoryWorkspaceStatus status,
        Instant updatedAt,
        List<Diagnostic> diagnostics
    );
}
