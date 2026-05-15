package de.burger.forensics.analytics.application.ingestion;

import de.burger.forensics.analytics.application.ingestion.query.RepositoryAnalysisView;
import de.burger.forensics.analytics.application.ingestion.query.RepositoryAnalysisWorkspaceView;
import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;

import java.util.List;
import java.util.Optional;

public interface RepositoryAnalysisQueryUseCase {
    List<RepositoryAnalysisView> listRepositoryAnalyses();

    Optional<RepositoryAnalysisView> findRepositoryAnalysis(AnalysisRunId analysisRunId);

    List<RepositoryAnalysisWorkspaceView> listWorkspaces();

    Optional<RepositoryAnalysisWorkspaceView> findWorkspace(WorkspaceId workspaceId);
}
