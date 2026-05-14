package de.burger.forensics.analytics.application.asset.command;

import de.burger.forensics.analytics.domain.workspace.AssetId;
import de.burger.forensics.analytics.domain.workspace.ProjectId;
import de.burger.forensics.analytics.domain.workspace.UserId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;

import java.util.Objects;

public record RegisterProjectAssetCommand(
    WorkspaceId workspaceId,
    ProjectId projectId,
    AssetId assetId,
    String storedFileName,
    String sha256,
    long sizeBytes,
    UserId actorUserId
) {
    public RegisterProjectAssetCommand {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(projectId, "projectId must not be null");
        Objects.requireNonNull(assetId, "assetId must not be null");
        requireText(storedFileName, "storedFileName");
        requireText(sha256, "sha256");
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("sizeBytes must not be negative");
        }
        Objects.requireNonNull(actorUserId, "actorUserId must not be null");
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
