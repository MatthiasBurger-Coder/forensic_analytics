package de.burger.forensics.analytics.application.canvas;

import de.burger.forensics.analytics.application.canvas.command.GetWorkspaceCanvasCommand;
import de.burger.forensics.analytics.application.canvas.result.WorkspaceCanvasView;

public interface WorkspaceCanvasUseCase {
    WorkspaceCanvasView get(GetWorkspaceCanvasCommand command);
}
