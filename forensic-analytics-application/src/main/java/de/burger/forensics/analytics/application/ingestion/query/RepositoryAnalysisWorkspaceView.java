package de.burger.forensics.analytics.application.ingestion.query;

import de.burger.forensics.analytics.domain.workspace.WorkspaceId;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record RepositoryAnalysisWorkspaceView(
    WorkspaceId workspaceId,
    Optional<String> name,
    Optional<String> status,
    Optional<Instant> createdAt,
    Optional<Instant> updatedAt,
    List<RepositoryAnalysisView> repositoryAnalyses
) {
    public RepositoryAnalysisWorkspaceView {
        workspaceId = Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        name = copyOptionalText(name, "name");
        status = copyOptionalText(status, "status");
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        repositoryAnalyses = List.copyOf(Objects.requireNonNull(
            repositoryAnalyses,
            "repositoryAnalyses must not be null"
        ));
    }

    private static Optional<String> copyOptionalText(Optional<String> value, String fieldName) {
        var copied = Objects.requireNonNull(value, fieldName + " must not be null");
        copied.ifPresent(text -> {
            if (text.isBlank()) {
                throw new IllegalArgumentException(fieldName + " must not be blank");
            }
        });
        return copied;
    }
}
