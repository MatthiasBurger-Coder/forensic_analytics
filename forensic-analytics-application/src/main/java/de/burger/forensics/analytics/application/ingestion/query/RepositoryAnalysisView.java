package de.burger.forensics.analytics.application.ingestion.query;

import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.analysis.AnalysisSessionState;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record RepositoryAnalysisView(
    AnalysisRunId analysisRunId,
    WorkspaceId workspaceId,
    String repositoryUrl,
    Optional<String> branch,
    Optional<String> commit,
    String resolvedRemoteUrl,
    String resolvedCommit,
    String checkoutStatus,
    AnalysisSessionState status,
    RepositoryAnalysisWorkflow workflow,
    Optional<Instant> createdAt,
    List<String> sourceRoots,
    List<String> diagnostics
) {
    public RepositoryAnalysisView {
        analysisRunId = Objects.requireNonNull(analysisRunId, "analysisRunId must not be null");
        workspaceId = Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        requireText(repositoryUrl, "repositoryUrl");
        branch = copyOptionalText(branch, "branch");
        commit = copyOptionalText(commit, "commit");
        requireText(resolvedRemoteUrl, "resolvedRemoteUrl");
        requireText(resolvedCommit, "resolvedCommit");
        requireText(checkoutStatus, "checkoutStatus");
        status = Objects.requireNonNull(status, "status must not be null");
        workflow = Objects.requireNonNull(workflow, "workflow must not be null");
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        sourceRoots = copyTextList(sourceRoots, "sourceRoot");
        diagnostics = copyTextList(diagnostics, "diagnostic");
    }

    private static Optional<String> copyOptionalText(Optional<String> value, String fieldName) {
        var copied = Objects.requireNonNull(value, fieldName + " must not be null");
        copied.ifPresent(text -> requireText(text, fieldName));
        return copied;
    }

    private static List<String> copyTextList(List<String> values, String fieldName) {
        return List.copyOf(Objects.requireNonNull(values, fieldName + "s must not be null")).stream()
            .peek(value -> requireText(value, fieldName))
            .toList();
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
