package de.burger.forensics.analytics.application.retention;

import de.burger.forensics.analytics.application.retention.command.ConfigureWorkspaceRetentionCommand;
import de.burger.forensics.analytics.domain.workspace.RetentionPolicy;

public interface RetentionPolicyUseCase {
    RetentionPolicy configure(ConfigureWorkspaceRetentionCommand command);
}
