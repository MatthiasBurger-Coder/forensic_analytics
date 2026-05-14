package de.burger.forensics.analytics.application.project.port;

import de.burger.forensics.analytics.domain.workspace.ProjectId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceProject;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository {
    void save(WorkspaceProject project);

    void update(WorkspaceProject project);

    Optional<WorkspaceProject> findById(ProjectId projectId);

    List<WorkspaceProject> findByWorkspace(WorkspaceId workspaceId);
}
