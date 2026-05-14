package de.burger.forensics.analytics.application.ingestion.result;

import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.repository.CheckoutResult;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;

import java.util.Objects;

public record AnalyzeRepositoryResult(
    AnalysisRunId analysisSessionId,
    WorkspaceId workspaceId,
    CheckoutResult checkoutResult,
    String message
) {
    public AnalyzeRepositoryResult {
        analysisSessionId = Objects.requireNonNull(analysisSessionId, "analysisSessionId must not be null");
        workspaceId = Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        checkoutResult = Objects.requireNonNull(checkoutResult, "checkoutResult must not be null");
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
    }
}
