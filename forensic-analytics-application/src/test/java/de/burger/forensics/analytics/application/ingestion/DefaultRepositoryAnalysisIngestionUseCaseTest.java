package de.burger.forensics.analytics.application.ingestion;

import de.burger.forensics.analytics.application.ingestion.command.AnalyzeRepositoryCommand;
import de.burger.forensics.analytics.application.ingestion.command.BuildContextCommand;
import de.burger.forensics.analytics.application.ingestion.command.RepositoryCheckoutRequest;
import de.burger.forensics.analytics.application.ingestion.command.WorkspacePreparationRequest;
import de.burger.forensics.analytics.application.ingestion.port.AnalysisSessionRepository;
import de.burger.forensics.analytics.application.ingestion.port.RepositoryCheckoutPort;
import de.burger.forensics.analytics.application.ingestion.port.WorkspacePreparationPort;
import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.analysis.AnalysisSession;
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
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultRepositoryAnalysisIngestionUseCaseTest {
    private final RecordingWorkspacePreparationPort workspacePreparationPort = new RecordingWorkspacePreparationPort();
    private final RecordingRepositoryCheckoutPort repositoryCheckoutPort = new RecordingRepositoryCheckoutPort();
    private final RecordingAnalysisSessionRepository analysisSessionRepository = new RecordingAnalysisSessionRepository();
    private final DefaultRepositoryAnalysisIngestionUseCase useCase = new DefaultRepositoryAnalysisIngestionUseCase(
        workspacePreparationPort,
        repositoryCheckoutPort,
        analysisSessionRepository
    );

    @Test
    void analyzePreparesWorkspaceChecksOutRepositoryAndRegistersSession() {
        var command = command();

        var result = useCase.analyze(command);

        var expectedSessionId = AnalysisRunId.deterministic("request-1");
        assertEquals(expectedSessionId, workspacePreparationPort.request.analysisSessionId());
        assertEquals(command.workspacePolicy(), workspacePreparationPort.request.policy());
        assertEquals(workspacePreparationPort.workspace, repositoryCheckoutPort.request.workspace());
        assertEquals(command.workspacePolicy(), repositoryCheckoutPort.request.workspacePolicy());
        assertEquals(command.repository(), repositoryCheckoutPort.request.repository());
        assertEquals(command.branch(), repositoryCheckoutPort.request.branch());
        assertEquals(command.commit(), repositoryCheckoutPort.request.commit());
        assertEquals(expectedSessionId, result.analysisSessionId());
        assertEquals(new WorkspaceId("workspace-1"), result.workspaceId());
        assertEquals(repositoryCheckoutPort.checkoutResult, result.checkoutResult());
        assertEquals(analysisSessionRepository.session.id(), result.analysisSessionId());
        assertEquals(command.requestId(), analysisSessionRepository.session.requestId());
        assertEquals(command.schemaVersion(), analysisSessionRepository.session.schemaVersion());
        assertEquals(command.buildContext().toDomain(), analysisSessionRepository.session.buildContext());
        assertEquals(command.repository(), analysisSessionRepository.session.repository());
        assertEquals(command.branch(), analysisSessionRepository.session.branch());
        assertEquals(command.commit(), analysisSessionRepository.session.commit());
        assertEquals(command.workspacePolicy(), analysisSessionRepository.session.workspacePolicy());
        assertEquals(repositoryCheckoutPort.checkoutResult, analysisSessionRepository.session.checkoutResult());
    }

    @Test
    void rejectsMissingCheckoutTargetBeforePreparingWorkspace() {
        var invalidCommand = new AnalyzeRepositoryCommand(
            command().repository(),
            new BranchReference(Optional.empty(), false),
            new CommitReference(Optional.empty(), false),
            command().workspacePolicy(),
            command().buildContext(),
            "request-1",
            "schema-v1"
        );

        assertThrows(RepositoryAnalysisIngestionException.class, () -> useCase.analyze(invalidCommand));
        assertEquals(null, workspacePreparationPort.request);
    }

    @Test
    void rejectsRequiredBranchAndCommitBeforePreparingWorkspace() {
        assertThrows(
            RepositoryAnalysisIngestionException.class,
            () -> useCase.analyze(new AnalyzeRepositoryCommand(
                command().repository(),
                new BranchReference(Optional.empty(), true),
                command().commit(),
                command().workspacePolicy(),
                command().buildContext(),
                "request-1",
                "schema-v1"
            ))
        );
        assertThrows(
            RepositoryAnalysisIngestionException.class,
            () -> useCase.analyze(new AnalyzeRepositoryCommand(
                command().repository(),
                command().branch(),
                new CommitReference(Optional.empty(), true),
                command().workspacePolicy(),
                command().buildContext(),
                "request-1",
                "schema-v1"
            ))
        );
        assertEquals(null, workspacePreparationPort.request);
    }

    @Test
    void rejectsWorkspaceThatIsNotReady() {
        workspacePreparationPort.workspace = new PreparedWorkspace(
            new WorkspaceId("workspace-1"),
            WorkspacePreparationStatus.FAILED,
            new WorkspacePath("/tmp/workspace-1"),
            new WorkspaceLease("analysis-1", Instant.parse("2026-05-14T12:00:00Z"), Optional.empty(), WorkspacePreparationStatus.FAILED),
            List.of("disk quota exceeded")
        );

        assertThrows(RepositoryAnalysisIngestionException.class, () -> useCase.analyze(command()));
        assertEquals(null, repositoryCheckoutPort.request);
    }

    @Test
    void dependenciesAndCommandAreRequired() {
        assertThrows(
            NullPointerException.class,
            () -> new DefaultRepositoryAnalysisIngestionUseCase(null, repositoryCheckoutPort, analysisSessionRepository)
        );
        assertThrows(
            NullPointerException.class,
            () -> new DefaultRepositoryAnalysisIngestionUseCase(workspacePreparationPort, null, analysisSessionRepository)
        );
        assertThrows(
            NullPointerException.class,
            () -> new DefaultRepositoryAnalysisIngestionUseCase(workspacePreparationPort, repositoryCheckoutPort, null)
        );
        assertThrows(NullPointerException.class, () -> useCase.analyze(null));
    }

    @Test
    void servicesRejectMissingDependenciesAndNullResults() {
        assertThrows(NullPointerException.class, () -> new WorkspacePreparationService(null));
        assertThrows(NullPointerException.class, () -> new RepositoryCheckoutService(null));
        assertThrows(NullPointerException.class, () -> new AnalysisSessionRegistrationService(null));
        assertThrows(
            NullPointerException.class,
            () -> new WorkspacePreparationService(new NullWorkspacePreparationPort()).prepare(
                new WorkspacePreparationRequest(AnalysisRunId.deterministic("request-1"), command().workspacePolicy())
            )
        );
        assertThrows(
            NullPointerException.class,
            () -> new RepositoryCheckoutService(ignored -> null).checkout(new RepositoryCheckoutRequest(
                AnalysisRunId.deterministic("request-1"),
                workspacePreparationPort.workspace,
                command().workspacePolicy(),
                command().repository(),
                command().branch(),
                command().commit()
            ))
        );
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

    private static CheckoutResult checkoutResult() {
        return new CheckoutResult(
            "https://example.invalid/project.git",
            Optional.of("main"),
            Optional.empty(),
            "abcdef",
            List.of(),
            "CHECKED_OUT",
            List.of("checkout mode: full clone")
        );
    }

    private static final class RecordingWorkspacePreparationPort implements WorkspacePreparationPort {
        private WorkspacePreparationRequest request;
        private PreparedWorkspace workspace = new PreparedWorkspace(
            new WorkspaceId("workspace-1"),
            WorkspacePreparationStatus.READY,
            new WorkspacePath("/tmp/workspace-1"),
            new WorkspaceLease("analysis-1", Instant.parse("2026-05-14T12:00:00Z"), Optional.empty(), WorkspacePreparationStatus.READY),
            List.of("Workspace created")
        );

        @Override
        public PreparedWorkspace prepare(WorkspacePreparationRequest request) {
            this.request = request;
            return workspace;
        }

        @Override
        public PreparedWorkspace cleanup(PreparedWorkspace workspace) {
            return workspace;
        }
    }

    private static final class RecordingRepositoryCheckoutPort implements RepositoryCheckoutPort {
        private RepositoryCheckoutRequest request;
        private final CheckoutResult checkoutResult = checkoutResult();

        @Override
        public CheckoutResult checkout(RepositoryCheckoutRequest request) {
            this.request = request;
            return checkoutResult;
        }
    }

    private static final class RecordingAnalysisSessionRepository implements AnalysisSessionRepository {
        private AnalysisSession session;

        @Override
        public void save(AnalysisSession session) {
            this.session = session;
        }

        @Override
        public Optional<AnalysisSession> findById(AnalysisRunId sessionId) {
            return Optional.ofNullable(session).filter(stored -> stored.id().equals(sessionId));
        }

        @Override
        public List<AnalysisSession> findAll() {
            return Optional.ofNullable(session).stream().toList();
        }
    }

    private static final class NullWorkspacePreparationPort implements WorkspacePreparationPort {
        @Override
        public PreparedWorkspace prepare(WorkspacePreparationRequest request) {
            return null;
        }

        @Override
        public PreparedWorkspace cleanup(PreparedWorkspace workspace) {
            return null;
        }
    }
}
