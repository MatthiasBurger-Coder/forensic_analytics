package de.burger.forensics.analytics.services.repositoryanalysis.application.port;

import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.RepositoryPreparation;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceSnapshotId;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.WorkspaceId;

import java.util.Optional;

public interface RepositoryPreparationRepository {
    RepositoryPreparation save(RepositoryPreparation preparation);

    Optional<RepositoryPreparation> findByRunAndSnapshot(AnalysisRunId analysisRunId, SourceSnapshotId sourceSnapshotId);

    Optional<RepositoryPreparation> findByRunAndWorkspace(AnalysisRunId analysisRunId, WorkspaceId workspaceId);
}
