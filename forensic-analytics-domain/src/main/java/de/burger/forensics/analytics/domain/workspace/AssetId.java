package de.burger.forensics.analytics.domain.workspace;

public record AssetId(String value) {
    public AssetId {
        RequiredWorkspaceText.requireText(value, "asset id");
    }
}
