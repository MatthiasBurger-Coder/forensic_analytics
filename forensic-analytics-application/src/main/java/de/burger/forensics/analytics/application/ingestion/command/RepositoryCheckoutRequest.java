package de.burger.forensics.analytics.application.ingestion.command;

import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.repository.BranchReference;
import de.burger.forensics.analytics.domain.repository.CommitReference;
import de.burger.forensics.analytics.domain.repository.RepositoryReference;
import de.burger.forensics.analytics.domain.workspace.PreparedWorkspace;
import de.burger.forensics.analytics.domain.workspace.WorkspacePolicy;

import java.util.Objects;

public record RepositoryCheckoutRequest(
    AnalysisRunId analysisSessionId,
    PreparedWorkspace workspace,
    WorkspacePolicy workspacePolicy,
    RepositoryReference repository,
    BranchReference branch,
    CommitReference commit
) {
    public RepositoryCheckoutRequest {
        analysisSessionId = Objects.requireNonNull(analysisSessionId, "analysisSessionId must not be null");
        workspace = Objects.requireNonNull(workspace, "workspace must not be null");
        workspacePolicy = Objects.requireNonNull(workspacePolicy, "workspacePolicy must not be null");
        repository = Objects.requireNonNull(repository, "repository must not be null");
        branch = Objects.requireNonNull(branch, "branch must not be null");
        commit = Objects.requireNonNull(commit, "commit must not be null");
    }
}
