package de.burger.forensics.analytics.application.project;

import de.burger.forensics.analytics.application.project.command.AddProjectMemberCommand;
import de.burger.forensics.analytics.application.project.command.ChangeProjectMemberRoleCommand;
import de.burger.forensics.analytics.application.project.command.ListProjectMembersCommand;
import de.burger.forensics.analytics.application.project.command.RemoveProjectMemberCommand;
import de.burger.forensics.analytics.domain.workspace.ProjectMembership;

import java.util.List;

public interface ProjectMemberManagementUseCase {
    ProjectMembership add(AddProjectMemberCommand command);

    List<ProjectMembership> list(ListProjectMembersCommand command);

    ProjectMembership changeRole(ChangeProjectMemberRoleCommand command);

    void remove(RemoveProjectMemberCommand command);
}
