package de.burger.forensics.analytics.services.repositorysource.adapter.out.memory;

import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryWorkspaceRepository;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryKey;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspace;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceBranch;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceBranchId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceId;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryRepositoryWorkspaceRepository implements RepositoryWorkspaceRepository {
    private final Map<WorkspaceId, RepositoryWorkspace> byWorkspaceId = new HashMap<>();
    private final Map<RepositoryKey, WorkspaceId> workspaceIdByRepositoryKey = new HashMap<>();

    @Override
    public synchronized RepositoryWorkspace save(RepositoryWorkspace workspace) {
        byWorkspaceId.put(workspace.workspaceId(), workspace);
        workspaceIdByRepositoryKey.put(workspace.repository().repositoryKey(), workspace.workspaceId());
        return workspace;
    }

    @Override
    public synchronized Optional<RepositoryWorkspace> findById(WorkspaceId workspaceId) {
        return Optional.ofNullable(byWorkspaceId.get(workspaceId));
    }

    @Override
    public synchronized Optional<RepositoryWorkspace> findByRepositoryKey(RepositoryKey repositoryKey) {
        return Optional.ofNullable(workspaceIdByRepositoryKey.get(repositoryKey))
            .flatMap(this::findById);
    }

    @Override
    public synchronized Optional<RepositoryWorkspaceBranch> findBranch(
        WorkspaceId workspaceId,
        WorkspaceBranchId workspaceBranchId
    ) {
        return findById(workspaceId)
            .flatMap(workspace -> workspace.branches().stream()
                .filter(branch -> branch.workspaceBranchId().equals(workspaceBranchId))
                .findFirst());
    }
}
