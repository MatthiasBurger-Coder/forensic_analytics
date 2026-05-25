package de.burger.forensics.analytics.services.repositorysource.application.port;

import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryKey;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspace;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceBranch;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceBranchId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceId;

import java.util.Optional;

public interface RepositoryWorkspaceRepository {
    RepositoryWorkspace save(RepositoryWorkspace workspace);

    Optional<RepositoryWorkspace> findById(WorkspaceId workspaceId);

    Optional<RepositoryWorkspace> findByRepositoryKey(RepositoryKey repositoryKey);

    Optional<RepositoryWorkspaceBranch> findBranch(WorkspaceId workspaceId, WorkspaceBranchId workspaceBranchId);
}
