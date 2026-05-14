package de.burger.forensics.analytics.domain.workspace;

public enum ProjectStatus {
    ACTIVE(true),
    ARCHIVED(false);

    private final boolean acceptsChanges;

    ProjectStatus(boolean acceptsChanges) {
        this.acceptsChanges = acceptsChanges;
    }

    public boolean acceptsChanges() {
        return acceptsChanges;
    }
}
