package de.burger.forensics.analytics.services.repositorysource.adapter.out.memory;

import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryWorkspaceRepository;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.Diagnostic;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryKey;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspace;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceBranch;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceStatus;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceBranchId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceId;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
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
    public synchronized List<RepositoryWorkspace> findAll(boolean includeCleaned) {
        return byWorkspaceId.values().stream()
            .filter(workspace -> includeCleaned || workspace.status() != RepositoryWorkspaceStatus.CLEANED)
            .map(InMemoryRepositoryWorkspaceRepository::withSortedBranches)
            .sorted(Comparator.comparing(workspace -> workspace.workspaceId().value()))
            .toList();
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

    @Override
    public synchronized RepositoryWorkspace updateWorkspaceStatus(
        WorkspaceId workspaceId,
        RepositoryWorkspaceStatus status,
        Instant updatedAt,
        List<Diagnostic> diagnostics
    ) {
        var workspace = findById(workspaceId)
            .orElseThrow(() -> new IllegalStateException("repository workspace was not found"));
        return save(new RepositoryWorkspace(
            workspace.workspaceId(),
            workspace.workspaceTitle(),
            workspace.repository(),
            status,
            workspace.createdAt(),
            updatedAt,
            workspace.branches(),
            diagnostics,
            workspace.safeAttributes()
        ));
    }

    private static RepositoryWorkspace withSortedBranches(RepositoryWorkspace workspace) {
        var branches = workspace.branches().stream()
            .sorted(Comparator
                .comparing(RepositoryWorkspaceBranch::repositoryBranch)
                .thenComparing(branch -> branch.workspaceBranchId().value()))
            .toList();
        return new RepositoryWorkspace(
            workspace.workspaceId(),
            workspace.workspaceTitle(),
            workspace.repository(),
            workspace.status(),
            workspace.createdAt(),
            workspace.updatedAt(),
            branches,
            workspace.diagnostics(),
            workspace.safeAttributes()
        );
    }
}
