package de.burger.forensics.analytics.services.repositorysource.application.port;

import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspacePolicy;

public interface RepositoryWorkspacePort {
    PreparedWorkspace prepare(AnalysisRunId analysisRunId, WorkspacePolicy policy);

    void cleanup(WorkspaceId workspaceId);
}
