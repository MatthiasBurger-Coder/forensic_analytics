package de.burger.forensics.analytics.application.ingestion;

import de.burger.forensics.analytics.application.ingestion.command.AnalyzeRepositoryCommand;
import de.burger.forensics.analytics.application.ingestion.command.RepositoryCheckoutRequest;
import de.burger.forensics.analytics.application.ingestion.command.WorkspacePreparationRequest;
import de.burger.forensics.analytics.application.ingestion.port.AnalysisSessionRepository;
import de.burger.forensics.analytics.application.ingestion.port.RepositoryCheckoutPort;
import de.burger.forensics.analytics.application.ingestion.port.WorkspacePreparationPort;
import de.burger.forensics.analytics.application.ingestion.result.AnalyzeRepositoryResult;
import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;

import java.util.Objects;

public final class DefaultRepositoryAnalysisIngestionUseCase implements RepositoryAnalysisIngestionUseCase {
    private final WorkspacePreparationService workspacePreparationService;
    private final RepositoryCheckoutService repositoryCheckoutService;
    private final AnalysisSessionRegistrationService registrationService;

    public DefaultRepositoryAnalysisIngestionUseCase(
        WorkspacePreparationPort workspacePreparationPort,
        RepositoryCheckoutPort repositoryCheckoutPort,
        AnalysisSessionRepository analysisSessionRepository
    ) {
        this(
            new WorkspacePreparationService(workspacePreparationPort),
            new RepositoryCheckoutService(repositoryCheckoutPort),
            new AnalysisSessionRegistrationService(analysisSessionRepository)
        );
    }

    public DefaultRepositoryAnalysisIngestionUseCase(
        WorkspacePreparationService workspacePreparationService,
        RepositoryCheckoutService repositoryCheckoutService,
        AnalysisSessionRegistrationService registrationService
    ) {
        this.workspacePreparationService = Objects.requireNonNull(
            workspacePreparationService,
            "workspacePreparationService must not be null"
        );
        this.repositoryCheckoutService = Objects.requireNonNull(
            repositoryCheckoutService,
            "repositoryCheckoutService must not be null"
        );
        this.registrationService = Objects.requireNonNull(registrationService, "registrationService must not be null");
    }

    @Override
    public AnalyzeRepositoryResult analyze(AnalyzeRepositoryCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        repositoryCheckoutService.requireCheckoutTarget(command.branch(), command.commit());

        var analysisSessionId = AnalysisRunId.deterministic(command.requestId());
        var workspace = workspacePreparationService.prepare(
            new WorkspacePreparationRequest(analysisSessionId, command.workspacePolicy())
        );
        var checkoutResult = repositoryCheckoutService.checkout(new RepositoryCheckoutRequest(
            analysisSessionId,
            workspace,
            command.workspacePolicy(),
            command.repository(),
            command.branch(),
            command.commit()
        ));
        registrationService.register(analysisSessionId, command, workspace.workspaceId(), checkoutResult);
        return new AnalyzeRepositoryResult(
            analysisSessionId,
            workspace.workspaceId(),
            checkoutResult,
            "Repository analysis session registered"
        );
    }
}
