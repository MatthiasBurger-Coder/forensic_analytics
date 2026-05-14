package de.burger.forensics.analytics.domain.workspace;

import java.util.Objects;

public record WorkspaceAsset(
    AssetId id,
    AssetScope scope,
    String storedFileName,
    String sha256,
    long sizeBytes
) {
    public WorkspaceAsset {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(scope, "scope must not be null");
        RequiredWorkspaceText.requireText(storedFileName, "stored file name");
        RequiredWorkspaceText.requireText(sha256, "sha256");
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("sizeBytes must not be negative");
        }
    }

    public WorkspaceId workspaceId() {
        return scope.workspaceId();
    }

    public boolean isShared() {
        return scope.isShared();
    }

    public boolean isProjectScoped() {
        return scope.isProjectScoped();
    }
}
