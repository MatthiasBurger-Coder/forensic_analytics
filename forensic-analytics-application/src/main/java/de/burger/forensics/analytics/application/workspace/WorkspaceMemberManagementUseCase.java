package de.burger.forensics.analytics.application.workspace;

import de.burger.forensics.analytics.application.workspace.command.AddWorkspaceMemberCommand;
import de.burger.forensics.analytics.application.workspace.command.ChangeWorkspaceMemberRoleCommand;
import de.burger.forensics.analytics.application.workspace.command.ListWorkspaceMembersCommand;
import de.burger.forensics.analytics.application.workspace.command.RemoveWorkspaceMemberCommand;
import de.burger.forensics.analytics.domain.workspace.WorkspaceMembership;

import java.util.List;

public interface WorkspaceMemberManagementUseCase {
    WorkspaceMembership add(AddWorkspaceMemberCommand command);

    List<WorkspaceMembership> list(ListWorkspaceMembersCommand command);

    WorkspaceMembership changeRole(ChangeWorkspaceMemberRoleCommand command);

    void remove(RemoveWorkspaceMemberCommand command);
}
