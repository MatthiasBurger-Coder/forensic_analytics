package de.burger.forensics.analytics.domain.workspace;

public record ProjectId(String value) {
    public ProjectId {
        RequiredWorkspaceText.requireText(value, "project id");
    }
}
