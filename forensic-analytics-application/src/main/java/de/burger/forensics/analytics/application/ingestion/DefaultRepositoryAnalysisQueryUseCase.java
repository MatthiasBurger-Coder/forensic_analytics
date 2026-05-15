package de.burger.forensics.analytics.application.ingestion;

import de.burger.forensics.analytics.application.ingestion.port.AnalysisSessionRepository;
import de.burger.forensics.analytics.application.ingestion.query.RepositoryAnalysisView;
import de.burger.forensics.analytics.application.ingestion.query.RepositoryAnalysisWorkflow;
import de.burger.forensics.analytics.application.ingestion.query.RepositoryAnalysisWorkspaceView;
import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.analysis.AnalysisSession;
import de.burger.forensics.analytics.domain.repository.SourceRoot;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class DefaultRepositoryAnalysisQueryUseCase implements RepositoryAnalysisQueryUseCase {
    private static final RepositoryAnalysisWorkflow WORKFLOW =
        RepositoryAnalysisWorkflow.REPOSITORY_SESSION_REGISTRATION;

    private final AnalysisSessionRepository analysisSessionRepository;

    public DefaultRepositoryAnalysisQueryUseCase(AnalysisSessionRepository analysisSessionRepository) {
        this.analysisSessionRepository = Objects.requireNonNull(
            analysisSessionRepository,
            "analysisSessionRepository must not be null"
        );
    }

    @Override
    public List<RepositoryAnalysisView> listRepositoryAnalyses() {
        return orderedSessions().stream()
            .map(DefaultRepositoryAnalysisQueryUseCase::toView)
            .toList();
    }

    @Override
    public Optional<RepositoryAnalysisView> findRepositoryAnalysis(AnalysisRunId analysisRunId) {
        Objects.requireNonNull(analysisRunId, "analysisRunId must not be null");
        return analysisSessionRepository.findById(analysisRunId)
            .map(DefaultRepositoryAnalysisQueryUseCase::toView);
    }

    @Override
    public List<RepositoryAnalysisWorkspaceView> listWorkspaces() {
        var byWorkspace = new LinkedHashMap<WorkspaceId, List<RepositoryAnalysisView>>();
        listRepositoryAnalyses().forEach(view -> byWorkspace.merge(
            view.workspaceId(),
            List.of(view),
            DefaultRepositoryAnalysisQueryUseCase::append
        ));
        return byWorkspace.entrySet().stream()
            .map(entry -> workspaceView(entry.getKey(), entry.getValue()))
            .toList();
    }

    @Override
    public Optional<RepositoryAnalysisWorkspaceView> findWorkspace(WorkspaceId workspaceId) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        var relatedAnalyses = listRepositoryAnalyses().stream()
            .filter(view -> view.workspaceId().equals(workspaceId))
            .toList();
        return relatedAnalyses.isEmpty()
            ? Optional.empty()
            : Optional.of(workspaceView(workspaceId, relatedAnalyses));
    }

    private List<AnalysisSession> orderedSessions() {
        return analysisSessionRepository.findAll().stream()
            .sorted(Comparator.comparing(session -> session.id().value()))
            .toList();
    }

    private static RepositoryAnalysisView toView(AnalysisSession session) {
        var checkoutResult = session.checkoutResult();
        return new RepositoryAnalysisView(
            session.id(),
            session.workspaceId(),
            session.repository().remoteUrl(),
            session.branch().name(),
            session.commit().hash(),
            checkoutResult.resolvedRemoteUrl(),
            checkoutResult.resolvedCommit(),
            checkoutResult.checkoutStatus(),
            session.state(),
            WORKFLOW,
            Optional.empty(),
            checkoutResult.detectedSourceRoots().stream().map(SourceRoot::path).toList(),
            checkoutResult.diagnostics()
        );
    }

    private static RepositoryAnalysisWorkspaceView workspaceView(
        WorkspaceId workspaceId,
        List<RepositoryAnalysisView> relatedAnalyses
    ) {
        return new RepositoryAnalysisWorkspaceView(
            workspaceId,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            relatedAnalyses
        );
    }

    private static List<RepositoryAnalysisView> append(
        List<RepositoryAnalysisView> existing,
        List<RepositoryAnalysisView> appended
    ) {
        return java.util.stream.Stream.concat(existing.stream(), appended.stream())
            .sorted(Comparator.comparing(view -> view.analysisRunId().value()))
            .toList();
    }
}
