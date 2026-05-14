package de.burger.forensics.analytics.persistence;

import de.burger.forensics.analytics.application.project.port.ProjectRepository;
import de.burger.forensics.analytics.domain.workspace.ProjectId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceProject;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryProjectRepository implements ProjectRepository {
    private final Map<ProjectId, WorkspaceProject> projects = new ConcurrentHashMap<>();

    @Override
    public void save(WorkspaceProject project) {
        Objects.requireNonNull(project, "project must not be null");
        projects.put(project.id(), project);
    }

    @Override
    public void update(WorkspaceProject project) {
        save(project);
    }

    @Override
    public Optional<WorkspaceProject> findById(ProjectId projectId) {
        Objects.requireNonNull(projectId, "projectId must not be null");
        return Optional.ofNullable(projects.get(projectId));
    }

    @Override
    public List<WorkspaceProject> findByWorkspace(WorkspaceId workspaceId) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        return projects.values().stream()
            .filter(project -> project.workspaceId().equals(workspaceId))
            .sorted(Comparator.comparing(project -> project.id().value()))
            .toList();
    }
}
