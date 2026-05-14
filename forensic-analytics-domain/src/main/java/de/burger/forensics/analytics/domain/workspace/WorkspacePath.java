package de.burger.forensics.analytics.domain.workspace;

public record WorkspacePath(String value) {
    public WorkspacePath {
        RequiredWorkspaceText.requireText(value, "workspace path");
    }
}
