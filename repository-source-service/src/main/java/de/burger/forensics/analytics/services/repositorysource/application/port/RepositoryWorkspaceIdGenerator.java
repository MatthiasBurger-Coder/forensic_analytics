package de.burger.forensics.analytics.services.repositorysource.application.port;

import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceBranchId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceId;

public interface RepositoryWorkspaceIdGenerator {
    WorkspaceId newWorkspaceId();

    WorkspaceBranchId newWorkspaceBranchId();
}
