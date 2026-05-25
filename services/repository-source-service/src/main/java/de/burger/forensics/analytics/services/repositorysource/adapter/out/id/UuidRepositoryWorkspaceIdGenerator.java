package de.burger.forensics.analytics.services.repositorysource.adapter.out.id;

import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryWorkspaceIdGenerator;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceBranchId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceId;

import java.util.UUID;

public final class UuidRepositoryWorkspaceIdGenerator implements RepositoryWorkspaceIdGenerator {
    @Override
    public WorkspaceId newWorkspaceId() {
        return new WorkspaceId("workspace-" + UUID.randomUUID());
    }

    @Override
    public WorkspaceBranchId newWorkspaceBranchId() {
        return new WorkspaceBranchId("workspace-branch-" + UUID.randomUUID());
    }
}
