package de.burger.forensics.analytics.services.repositorysource.application.port;

import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryPreparation;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.SourceSnapshotId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceId;

import java.util.Optional;

public interface RepositoryPreparationRepository {
    RepositoryPreparation save(RepositoryPreparation preparation);

    Optional<RepositoryPreparation> findByRunAndSnapshot(AnalysisRunId analysisRunId, SourceSnapshotId sourceSnapshotId);

    Optional<RepositoryPreparation> findByRunAndWorkspace(AnalysisRunId analysisRunId, WorkspaceId workspaceId);
}
