package de.burger.forensics.analytics.domain.workspace;

public record WorkspaceId(String value) {
    public WorkspaceId {
        RequiredWorkspaceText.requireText(value, "workspace id");
    }
}
