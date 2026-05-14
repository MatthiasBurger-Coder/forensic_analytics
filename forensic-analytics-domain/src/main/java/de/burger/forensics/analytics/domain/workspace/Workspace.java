package de.burger.forensics.analytics.domain.workspace;

import java.util.Objects;

public record Workspace(
    WorkspaceId id,
    String name,
    WorkspaceStatus status
) {
    public Workspace {
        Objects.requireNonNull(id, "id must not be null");
        RequiredWorkspaceText.requireText(name, "workspace name");
        Objects.requireNonNull(status, "status must not be null");
    }

    public static Workspace active(WorkspaceId id, String name) {
        return new Workspace(id, name, WorkspaceStatus.ACTIVE);
    }

    public Workspace rename(String newName) {
        return new Workspace(id, newName, status);
    }

    public Workspace archive() {
        return new Workspace(id, name, WorkspaceStatus.ARCHIVED);
    }

    public boolean acceptsChanges() {
        return status.acceptsChanges();
    }
}
