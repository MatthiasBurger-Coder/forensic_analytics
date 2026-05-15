package de.burger.forensics.analytics.bootstrap;

import de.burger.forensics.analytics.application.ingestion.command.AnalyzeRepositoryCommand;
import de.burger.forensics.analytics.application.ingestion.command.BuildContextCommand;
import de.burger.forensics.analytics.application.ingestion.command.RepositoryCheckoutRequest;
import de.burger.forensics.analytics.application.ingestion.command.WorkspacePreparationRequest;
import de.burger.forensics.analytics.application.ingestion.port.RepositoryCheckoutPort;
import de.burger.forensics.analytics.application.ingestion.port.WorkspacePreparationPort;
import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.repository.BranchReference;
import de.burger.forensics.analytics.domain.repository.CheckoutResult;
import de.burger.forensics.analytics.domain.repository.CommitReference;
import de.burger.forensics.analytics.domain.repository.RepositoryReference;
import de.burger.forensics.analytics.domain.workspace.PreparedWorkspace;
import de.burger.forensics.analytics.domain.workspace.WorkspaceCleanupPolicy;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceLease;
import de.burger.forensics.analytics.domain.workspace.WorkspacePath;
import de.burger.forensics.analytics.domain.workspace.WorkspacePolicy;
import de.burger.forensics.analytics.domain.workspace.WorkspacePreparationStatus;
import de.burger.forensics.analytics.persistence.InMemoryAnalysisSessionRepository;
import de.burger.forensics.analytics.persistence.InMemoryIngestionSessionRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForensicAnalyticsBackendComponentsTest {
    @Test
    void repositoryAnalysisIngestionAndQueryUseCasesShareAnalysisSessionRepository() {
        var components = ForensicAnalyticsBackendComponents.create(
            new InMemoryIngestionSessionRepository(),
            new InMemoryAnalysisSessionRepository(),
            new RecordingWorkspacePreparationPort(),
            new RecordingRepositoryCheckoutPort()
        );

        var result = components.repositoryAnalysisIngestionUseCase().analyze(command());

        var stored = components.repositoryAnalysisQueryUseCase()
            .findRepositoryAnalysis(result.analysisSessionId())
            .orElseThrow();
        assertEquals(result.analysisSessionId(), stored.analysisRunId());
        assertEquals("REGISTERED", stored.status().name());
        assertTrue(stored.createdAt().isEmpty());
    }

    private static AnalyzeRepositoryCommand command() {
        return new AnalyzeRepositoryCommand(
            new RepositoryReference("https://example.invalid/project.git", Optional.of("github"), Map.of()),
            new BranchReference(Optional.of("main"), true),
            new CommitReference(Optional.empty(), false),
            new WorkspacePolicy(false, false, false, false, Duration.ofSeconds(60), 0, WorkspaceCleanupPolicy.RETAIN_FOR_REVIEW),
            new BuildContextCommand("gradle", "build-1", "project", List.of(":app"), Map.of()),
            "request-1",
            "schema-v1"
        );
    }

    private static final class RecordingWorkspacePreparationPort implements WorkspacePreparationPort {
        @Override
        public PreparedWorkspace prepare(WorkspacePreparationRequest request) {
            return new PreparedWorkspace(
                new WorkspaceId("workspace-1"),
                WorkspacePreparationStatus.READY,
                new WorkspacePath("/tmp/workspace-1"),
                new WorkspaceLease(
                    request.analysisSessionId().value(),
                    Instant.parse("2026-05-14T12:00:00Z"),
                    Optional.empty(),
                    WorkspacePreparationStatus.READY
                ),
                List.of("Workspace created")
            );
        }

        @Override
        public PreparedWorkspace cleanup(PreparedWorkspace workspace) {
            return workspace;
        }
    }

    private static final class RecordingRepositoryCheckoutPort implements RepositoryCheckoutPort {
        @Override
        public CheckoutResult checkout(RepositoryCheckoutRequest request) {
            return new CheckoutResult(
                request.repository().remoteUrl(),
                request.branch().name(),
                request.commit().hash(),
                "abcdef",
                List.of(),
                "CHECKED_OUT",
                List.of()
            );
        }
    }
}
