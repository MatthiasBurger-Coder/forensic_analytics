package de.burger.forensics.analytics.application.workspace;

import de.burger.forensics.analytics.application.workspace.command.ArchiveWorkspaceCommand;
import de.burger.forensics.analytics.application.workspace.command.CreateWorkspaceCommand;
import de.burger.forensics.analytics.application.workspace.command.GetWorkspaceCommand;
import de.burger.forensics.analytics.application.workspace.command.ListWorkspacesCommand;
import de.burger.forensics.analytics.application.workspace.command.RenameWorkspaceCommand;
import de.burger.forensics.analytics.domain.workspace.Workspace;

import java.util.List;

public interface WorkspaceManagementUseCase {
    Workspace create(CreateWorkspaceCommand command);

    Workspace get(GetWorkspaceCommand command);

    List<Workspace> list(ListWorkspacesCommand command);

    Workspace rename(RenameWorkspaceCommand command);

    Workspace archive(ArchiveWorkspaceCommand command);
}
