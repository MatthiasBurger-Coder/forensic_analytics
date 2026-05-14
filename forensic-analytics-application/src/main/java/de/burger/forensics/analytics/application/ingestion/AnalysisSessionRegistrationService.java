package de.burger.forensics.analytics.application.ingestion;

import de.burger.forensics.analytics.application.ingestion.command.AnalyzeRepositoryCommand;
import de.burger.forensics.analytics.application.ingestion.port.AnalysisSessionRepository;
import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.analysis.AnalysisSession;
import de.burger.forensics.analytics.domain.repository.CheckoutResult;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;

import java.util.Objects;

public final class AnalysisSessionRegistrationService {
    private final AnalysisSessionRepository analysisSessionRepository;

    public AnalysisSessionRegistrationService(AnalysisSessionRepository analysisSessionRepository) {
        this.analysisSessionRepository = Objects.requireNonNull(
            analysisSessionRepository,
            "analysisSessionRepository must not be null"
        );
    }

    public AnalysisSession register(
        AnalysisRunId analysisSessionId,
        AnalyzeRepositoryCommand command,
        WorkspaceId workspaceId,
        CheckoutResult checkoutResult
    ) {
        Objects.requireNonNull(analysisSessionId, "analysisSessionId must not be null");
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(checkoutResult, "checkoutResult must not be null");
        var session = AnalysisSession.registered(
            analysisSessionId,
            command.requestId(),
            command.schemaVersion(),
            command.buildContext().toDomain(),
            command.repository(),
            command.branch(),
            command.commit(),
            command.workspacePolicy(),
            workspaceId,
            checkoutResult
        );
        analysisSessionRepository.save(session);
        return session;
    }
}
