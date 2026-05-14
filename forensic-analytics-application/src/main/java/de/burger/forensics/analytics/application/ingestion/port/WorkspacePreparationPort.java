package de.burger.forensics.analytics.application.ingestion.port;

import de.burger.forensics.analytics.application.ingestion.command.WorkspacePreparationRequest;
import de.burger.forensics.analytics.domain.workspace.PreparedWorkspace;

public interface WorkspacePreparationPort {
    PreparedWorkspace prepare(WorkspacePreparationRequest request);

    PreparedWorkspace cleanup(PreparedWorkspace workspace);
}
