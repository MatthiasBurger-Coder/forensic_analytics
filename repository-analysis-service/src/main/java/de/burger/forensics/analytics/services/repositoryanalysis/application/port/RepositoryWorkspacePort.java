package de.burger.forensics.analytics.services.repositoryanalysis.application.port;

import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.WorkspaceId;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.WorkspacePolicy;

public interface RepositoryWorkspacePort {
    PreparedWorkspace prepare(AnalysisRunId analysisRunId, WorkspacePolicy policy);

    void cleanup(WorkspaceId workspaceId);
}
