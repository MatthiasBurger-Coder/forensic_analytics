package de.burger.forensics.analytics.services.repositoryanalysis.adapter.out.memory;

import de.burger.forensics.analytics.services.repositoryanalysis.application.port.RepositoryPreparationRepository;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.RepositoryPreparation;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceSnapshotId;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.WorkspaceId;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryRepositoryPreparationRepository implements RepositoryPreparationRepository {
    private final Map<String, RepositoryPreparation> bySnapshot = new HashMap<>();
    private final Map<String, RepositoryPreparation> byWorkspace = new HashMap<>();

    @Override
    public synchronized RepositoryPreparation save(RepositoryPreparation preparation) {
        bySnapshot.put(snapshotKey(preparation.analysisRunId(), preparation.sourceSnapshotId()), preparation);
        byWorkspace.put(workspaceKey(preparation.analysisRunId(), preparation.workspaceId()), preparation);
        return preparation;
    }

    @Override
    public synchronized Optional<RepositoryPreparation> findByRunAndSnapshot(
        AnalysisRunId analysisRunId,
        SourceSnapshotId sourceSnapshotId
    ) {
        return Optional.ofNullable(bySnapshot.get(snapshotKey(analysisRunId, sourceSnapshotId)));
    }

    @Override
    public synchronized Optional<RepositoryPreparation> findByRunAndWorkspace(
        AnalysisRunId analysisRunId,
        WorkspaceId workspaceId
    ) {
        return Optional.ofNullable(byWorkspace.get(workspaceKey(analysisRunId, workspaceId)));
    }

    private static String snapshotKey(AnalysisRunId analysisRunId, SourceSnapshotId sourceSnapshotId) {
        return analysisRunId.value() + "|" + sourceSnapshotId.value();
    }

    private static String workspaceKey(AnalysisRunId analysisRunId, WorkspaceId workspaceId) {
        return analysisRunId.value() + "|" + workspaceId.value();
    }
}
