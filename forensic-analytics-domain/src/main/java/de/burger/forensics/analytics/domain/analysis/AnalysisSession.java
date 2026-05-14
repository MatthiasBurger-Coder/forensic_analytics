package de.burger.forensics.analytics.domain.analysis;

import de.burger.forensics.analytics.domain.repository.BranchReference;
import de.burger.forensics.analytics.domain.repository.CheckoutResult;
import de.burger.forensics.analytics.domain.repository.CommitReference;
import de.burger.forensics.analytics.domain.repository.RepositoryReference;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;
import de.burger.forensics.analytics.domain.workspace.WorkspacePolicy;

import java.util.Objects;

public record AnalysisSession(
    AnalysisRunId id,
    String requestId,
    String schemaVersion,
    BuildContext buildContext,
    RepositoryReference repository,
    BranchReference branch,
    CommitReference commit,
    WorkspacePolicy workspacePolicy,
    WorkspaceId workspaceId,
    CheckoutResult checkoutResult,
    AnalysisSessionState state
) {
    public AnalysisSession {
        id = Objects.requireNonNull(id, "id must not be null");
        RequiredAnalysisText.requireText(requestId, "request id");
        RequiredAnalysisText.requireText(schemaVersion, "schema version");
        buildContext = Objects.requireNonNull(buildContext, "buildContext must not be null");
        repository = Objects.requireNonNull(repository, "repository must not be null");
        branch = Objects.requireNonNull(branch, "branch must not be null");
        commit = Objects.requireNonNull(commit, "commit must not be null");
        workspacePolicy = Objects.requireNonNull(workspacePolicy, "workspacePolicy must not be null");
        workspaceId = Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        checkoutResult = Objects.requireNonNull(checkoutResult, "checkoutResult must not be null");
        state = Objects.requireNonNull(state, "state must not be null");
    }

    public static AnalysisSession registered(
        AnalysisRunId id,
        String requestId,
        String schemaVersion,
        BuildContext buildContext,
        RepositoryReference repository,
        BranchReference branch,
        CommitReference commit,
        WorkspacePolicy workspacePolicy,
        WorkspaceId workspaceId,
        CheckoutResult checkoutResult
    ) {
        return new AnalysisSession(
            id,
            requestId,
            schemaVersion,
            buildContext,
            repository,
            branch,
            commit,
            workspacePolicy,
            workspaceId,
            checkoutResult,
            AnalysisSessionState.REGISTERED
        );
    }
}
