package de.burger.forensics.analytics.domain.workspace;

public record UserId(String value) {
    public UserId {
        RequiredWorkspaceText.requireText(value, "user id");
    }
}
