package de.burger.forensics.analytics.domain.workspace;

public enum WorkspaceStatus {
    ACTIVE(true),
    ARCHIVED(false);

    private final boolean acceptsChanges;

    WorkspaceStatus(boolean acceptsChanges) {
        this.acceptsChanges = acceptsChanges;
    }

    public boolean acceptsChanges() {
        return acceptsChanges;
    }
}
