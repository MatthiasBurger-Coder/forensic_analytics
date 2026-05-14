package de.burger.forensics.analytics.application.ingestion.command;

import de.burger.forensics.analytics.domain.repository.BranchReference;
import de.burger.forensics.analytics.domain.repository.CommitReference;
import de.burger.forensics.analytics.domain.repository.RepositoryReference;
import de.burger.forensics.analytics.domain.workspace.WorkspacePolicy;

import java.util.Objects;

public record AnalyzeRepositoryCommand(
    RepositoryReference repository,
    BranchReference branch,
    CommitReference commit,
    WorkspacePolicy workspacePolicy,
    BuildContextCommand buildContext,
    String requestId,
    String schemaVersion
) {
    public AnalyzeRepositoryCommand {
        repository = Objects.requireNonNull(repository, "repository must not be null");
        branch = Objects.requireNonNull(branch, "branch must not be null");
        commit = Objects.requireNonNull(commit, "commit must not be null");
        workspacePolicy = Objects.requireNonNull(workspacePolicy, "workspacePolicy must not be null");
        buildContext = Objects.requireNonNull(buildContext, "buildContext must not be null");
        requireText(requestId, "request id");
        requireText(schemaVersion, "schema version");
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
