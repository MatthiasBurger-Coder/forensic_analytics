package de.burger.forensics.analytics.application.canvas.result;

import de.burger.forensics.analytics.domain.workspace.Workspace;
import de.burger.forensics.analytics.domain.workspace.WorkspaceAsset;
import de.burger.forensics.analytics.domain.workspace.WorkspaceProject;
import de.burger.forensics.analytics.domain.workspace.WorkspaceRole;

import java.util.List;
import java.util.Objects;

public record WorkspaceCanvasView(
    Workspace workspace,
    WorkspaceRole actorRole,
    List<WorkspaceProject> visibleProjects,
    List<WorkspaceAsset> sharedAssets,
    boolean canManageWorkspace,
    boolean canManageSharedAssets,
    boolean canReadAuditLog
) {
    public WorkspaceCanvasView {
        Objects.requireNonNull(workspace, "workspace must not be null");
        Objects.requireNonNull(actorRole, "actorRole must not be null");
        visibleProjects = List.copyOf(Objects.requireNonNull(visibleProjects, "visibleProjects must not be null"));
        sharedAssets = List.copyOf(Objects.requireNonNull(sharedAssets, "sharedAssets must not be null"));
    }
}
