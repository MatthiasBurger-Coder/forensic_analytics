package de.burger.forensics.analytics.application.project;

import de.burger.forensics.analytics.application.project.command.ArchiveProjectCommand;
import de.burger.forensics.analytics.application.project.command.CreateProjectCommand;
import de.burger.forensics.analytics.application.project.command.GetProjectCommand;
import de.burger.forensics.analytics.application.project.command.ListProjectsCommand;
import de.burger.forensics.analytics.application.project.command.RenameProjectCommand;
import de.burger.forensics.analytics.domain.workspace.WorkspaceProject;

import java.util.List;

public interface ProjectManagementUseCase {
    WorkspaceProject create(CreateProjectCommand command);

    WorkspaceProject get(GetProjectCommand command);

    List<WorkspaceProject> list(ListProjectsCommand command);

    WorkspaceProject rename(RenameProjectCommand command);

    WorkspaceProject archive(ArchiveProjectCommand command);
}
