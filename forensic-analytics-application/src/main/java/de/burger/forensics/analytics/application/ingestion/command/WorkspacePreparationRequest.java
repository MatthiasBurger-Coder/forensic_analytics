package de.burger.forensics.analytics.application.ingestion.command;

import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.workspace.WorkspacePolicy;

import java.util.Objects;

public record WorkspacePreparationRequest(AnalysisRunId analysisSessionId, WorkspacePolicy policy) {
    public WorkspacePreparationRequest {
        analysisSessionId = Objects.requireNonNull(analysisSessionId, "analysisSessionId must not be null");
        policy = Objects.requireNonNull(policy, "policy must not be null");
    }
}
