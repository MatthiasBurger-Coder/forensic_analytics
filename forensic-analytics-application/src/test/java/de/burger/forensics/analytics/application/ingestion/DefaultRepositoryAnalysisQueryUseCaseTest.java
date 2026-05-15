package de.burger.forensics.analytics.application.ingestion;

import de.burger.forensics.analytics.application.ingestion.port.AnalysisSessionRepository;
import de.burger.forensics.analytics.application.ingestion.query.RepositoryAnalysisWorkflow;
import de.burger.forensics.analytics.application.ingestion.query.RepositoryAnalysisWorkspaceView;
import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.analysis.AnalysisSession;
import de.burger.forensics.analytics.domain.analysis.AnalysisSessionState;
import de.burger.forensics.analytics.domain.analysis.BuildContext;
import de.burger.forensics.analytics.domain.repository.BranchReference;
import de.burger.forensics.analytics.domain.repository.CheckoutResult;
import de.burger.forensics.analytics.domain.repository.CommitReference;
import de.burger.forensics.analytics.domain.repository.RepositoryReference;
import de.burger.forensics.analytics.domain.repository.SourceRoot;
import de.burger.forensics.analytics.domain.workspace.WorkspaceCleanupPolicy;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;
import de.burger.forensics.analytics.domain.workspace.WorkspacePolicy;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultRepositoryAnalysisQueryUseCaseTest {
    private final RecordingAnalysisSessionRepository repository = new RecordingAnalysisSessionRepository();
    private final DefaultRepositoryAnalysisQueryUseCase useCase = new DefaultRepositoryAnalysisQueryUseCase(repository);

    @Test
    void listsRepositoryAnalysesInDeterministicAnalysisRunIdOrderWithVerifiedStatus() {
        repository.save(session("analysis-c", "workspace-c"));
        repository.save(session("analysis-a", "workspace-a"));
        repository.save(session("analysis-b", "workspace-b"));

        var analyses = useCase.listRepositoryAnalyses();

        assertEquals(List.of("analysis-a", "analysis-b", "analysis-c"), analyses.stream()
            .map(view -> view.analysisRunId().value())
            .toList());
        assertEquals(AnalysisSessionState.REGISTERED, analyses.getFirst().status());
        assertEquals(RepositoryAnalysisWorkflow.REPOSITORY_SESSION_REGISTRATION, analyses.getFirst().workflow());
        assertTrue(analyses.getFirst().createdAt().isEmpty());
        assertEquals(List.of("src/main/java"), analyses.getFirst().sourceRoots());
    }

    @Test
    void findsRepositoryAnalysisById() {
        repository.save(session("analysis-a", "workspace-a"));

        assertTrue(useCase.findRepositoryAnalysis(new AnalysisRunId("missing")).isEmpty());
        assertEquals(
            "analysis-a",
            useCase.findRepositoryAnalysis(new AnalysisRunId("analysis-a")).orElseThrow().analysisRunId().value()
        );
    }

    @Test
    void derivesWorkspaceViewsOnlyFromAnalysisSessionsWithoutInventedTimestampsOrNames() {
        repository.save(session("analysis-b", "workspace-1"));
        repository.save(session("analysis-a", "workspace-1"));
        repository.save(session("analysis-c", "workspace-2"));

        var workspaces = useCase.listWorkspaces();

        assertEquals(List.of("workspace-1", "workspace-2"), workspaces.stream()
            .map(view -> view.workspaceId().value())
            .toList());
        var first = workspaces.getFirst();
        assertTrue(first.name().isEmpty());
        assertTrue(first.status().isEmpty());
        assertTrue(first.createdAt().isEmpty());
        assertTrue(first.updatedAt().isEmpty());
        assertEquals(List.of("analysis-a", "analysis-b"), first.repositoryAnalyses().stream()
            .map(view -> view.analysisRunId().value())
            .toList());
    }

    @Test
    void findsWorkspaceById() {
        repository.save(session("analysis-a", "workspace-a"));

        assertTrue(useCase.findWorkspace(new WorkspaceId("missing")).isEmpty());
        assertEquals(
            "workspace-a",
            useCase.findWorkspace(new WorkspaceId("workspace-a")).orElseThrow().workspaceId().value()
        );
    }

    @Test
    void requiresInputs() {
        assertThrows(NullPointerException.class, () -> new DefaultRepositoryAnalysisQueryUseCase(null));
        assertThrows(NullPointerException.class, () -> useCase.findRepositoryAnalysis(null));
        assertThrows(NullPointerException.class, () -> useCase.findWorkspace(null));
    }

    @Test
    void workspaceViewAllowsVerifiedOptionalTextAndRejectsBlankOptionalText() {
        var view = new RepositoryAnalysisWorkspaceView(
            new WorkspaceId("workspace-1"),
            Optional.of("Investigation"),
            Optional.of("REGISTERED"),
            Optional.empty(),
            Optional.empty(),
            List.of()
        );

        assertEquals(Optional.of("Investigation"), view.name());
        assertEquals(Optional.of("REGISTERED"), view.status());
        assertThrows(
            IllegalArgumentException.class,
            () -> new RepositoryAnalysisWorkspaceView(
                new WorkspaceId("workspace-1"),
                Optional.of(" "),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                List.of()
            )
        );
    }

    @Test
    void repositoryAnalysisViewRejectsBlankRequiredEvidenceFields() {
        var valid = useCaseView();

        assertEquals("analysis-valid", valid.analysisRunId().value());
        assertThrows(
            IllegalArgumentException.class,
            () -> new de.burger.forensics.analytics.application.ingestion.query.RepositoryAnalysisView(
                new AnalysisRunId("analysis-invalid"),
                new WorkspaceId("workspace-1"),
                " ",
                Optional.empty(),
                Optional.empty(),
                "https://example.invalid/project.git",
                "abcdef",
                "CHECKED_OUT",
                AnalysisSessionState.REGISTERED,
                RepositoryAnalysisWorkflow.REPOSITORY_SESSION_REGISTRATION,
                Optional.empty(),
                List.of(),
                List.of()
            )
        );
    }

    private static AnalysisSession session(String analysisRunId, String workspaceId) {
        return AnalysisSession.registered(
            new AnalysisRunId(analysisRunId),
            "request-" + analysisRunId,
            "schema-v1",
            new BuildContext("gradle", "build-1", Optional.empty(), List.of(":app"), Map.of()),
            new RepositoryReference("https://example.invalid/project.git", Optional.of("github"), Map.of()),
            new BranchReference(Optional.of("main"), true),
            new CommitReference(Optional.empty(), false),
            new WorkspacePolicy(
                false,
                false,
                false,
                false,
                Duration.ofSeconds(60),
                0,
                WorkspaceCleanupPolicy.RETAIN_FOR_REVIEW
            ),
            new WorkspaceId(workspaceId),
            new CheckoutResult(
                "https://example.invalid/project.git",
                Optional.of("main"),
                Optional.empty(),
                "abcdef",
                List.of(new SourceRoot("src/main/java")),
                "CHECKED_OUT",
                List.of("checkout completed")
            )
        );
    }

    private static de.burger.forensics.analytics.application.ingestion.query.RepositoryAnalysisView useCaseView() {
        return new de.burger.forensics.analytics.application.ingestion.query.RepositoryAnalysisView(
            new AnalysisRunId("analysis-valid"),
            new WorkspaceId("workspace-1"),
            "https://example.invalid/project.git",
            Optional.empty(),
            Optional.empty(),
            "https://example.invalid/project.git",
            "abcdef",
            "CHECKED_OUT",
            AnalysisSessionState.REGISTERED,
            RepositoryAnalysisWorkflow.REPOSITORY_SESSION_REGISTRATION,
            Optional.empty(),
            List.of(),
            List.of()
        );
    }

    private static final class RecordingAnalysisSessionRepository implements AnalysisSessionRepository {
        private final List<AnalysisSession> sessions = new ArrayList<>();

        @Override
        public void save(AnalysisSession session) {
            sessions.removeIf(stored -> stored.id().equals(session.id()));
            sessions.add(session);
        }

        @Override
        public Optional<AnalysisSession> findById(AnalysisRunId sessionId) {
            return sessions.stream()
                .filter(session -> session.id().equals(sessionId))
                .findFirst();
        }

        @Override
        public List<AnalysisSession> findAll() {
            return List.copyOf(sessions);
        }
    }
}
