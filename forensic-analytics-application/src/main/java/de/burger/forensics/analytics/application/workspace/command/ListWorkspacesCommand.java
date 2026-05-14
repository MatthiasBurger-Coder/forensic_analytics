package de.burger.forensics.analytics.application.workspace.command;

import de.burger.forensics.analytics.domain.workspace.UserId;

import java.util.Objects;

public record ListWorkspacesCommand(UserId actorUserId) {
    public ListWorkspacesCommand {
        Objects.requireNonNull(actorUserId, "actorUserId must not be null");
    }
}
