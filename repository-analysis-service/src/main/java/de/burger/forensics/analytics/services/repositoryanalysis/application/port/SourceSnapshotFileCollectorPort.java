package de.burger.forensics.analytics.services.repositoryanalysis.application.port;

import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceRoot;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceSnapshotHandoffPolicy;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceSnapshotSourceFile;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.WorkspaceId;

import java.util.List;

public interface SourceSnapshotFileCollectorPort {
    List<SourceSnapshotSourceFile> collect(
        WorkspaceId workspaceId,
        List<SourceRoot> sourceRoots,
        SourceSnapshotHandoffPolicy policy
    );
}
