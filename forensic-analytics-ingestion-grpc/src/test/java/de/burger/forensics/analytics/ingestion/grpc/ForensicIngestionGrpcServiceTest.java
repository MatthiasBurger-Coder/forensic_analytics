package de.burger.forensics.analytics.ingestion.grpc;

import com.google.protobuf.ByteString;
import de.burger.forensics.analytics.application.ingestion.DefaultForensicIngestionUseCase;
import de.burger.forensics.analytics.application.ingestion.ForensicIngestionUseCase;
import de.burger.forensics.analytics.application.ingestion.IngestionSessionException;
import de.burger.forensics.analytics.application.ingestion.RepositoryAnalysisIngestionException;
import de.burger.forensics.analytics.application.ingestion.RepositoryAnalysisIngestionUseCase;
import de.burger.forensics.analytics.application.ingestion.command.AbortAnalysisSessionCommand;
import de.burger.forensics.analytics.application.ingestion.command.AnalyzeRepositoryCommand;
import de.burger.forensics.analytics.application.ingestion.command.CompleteAnalysisSessionCommand;
import de.burger.forensics.analytics.application.ingestion.command.StartAnalysisSessionCommand;
import de.burger.forensics.analytics.application.ingestion.command.UploadAnalysisDataCommand;
import de.burger.forensics.analytics.application.ingestion.port.IngestionSessionRepository;
import de.burger.forensics.analytics.application.ingestion.result.AbortAnalysisSessionResult;
import de.burger.forensics.analytics.application.ingestion.result.AnalyzeRepositoryResult;
import de.burger.forensics.analytics.application.ingestion.result.CompleteAnalysisSessionResult;
import de.burger.forensics.analytics.application.ingestion.result.IngestionStatus;
import de.burger.forensics.analytics.application.ingestion.result.StartAnalysisSessionResult;
import de.burger.forensics.analytics.application.ingestion.result.UploadAnalysisDataResult;
import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.ingestion.IngestionPayload;
import de.burger.forensics.analytics.domain.ingestion.IngestionSession;
import de.burger.forensics.analytics.domain.ingestion.IngestionSessionState;
import de.burger.forensics.analytics.domain.repository.CheckoutResult;
import de.burger.forensics.analytics.domain.repository.SourceRoot;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;
import de.burger.forensics.analytics.ingestion.v1.AbortAnalysisSessionRequest;
import de.burger.forensics.analytics.ingestion.v1.AnalysisDataEnvelope;
import de.burger.forensics.analytics.ingestion.v1.AnalysisPayloadDescriptor;
import de.burger.forensics.analytics.ingestion.v1.AnalysisPayloadKind;
import de.burger.forensics.analytics.ingestion.v1.AnalyzeRepositoryRequest;
import de.burger.forensics.analytics.ingestion.v1.BranchReference;
import de.burger.forensics.analytics.ingestion.v1.BuildIdentity;
import de.burger.forensics.analytics.ingestion.v1.BuildContext;
import de.burger.forensics.analytics.ingestion.v1.CommitReference;
import de.burger.forensics.analytics.ingestion.v1.CompleteAnalysisSessionRequest;
import de.burger.forensics.analytics.ingestion.v1.ForensicIngestionServiceGrpc;
import de.burger.forensics.analytics.ingestion.v1.ModuleIdentity;
import de.burger.forensics.analytics.ingestion.v1.PluginIdentity;
import de.burger.forensics.analytics.ingestion.v1.RepositoryReference;
import de.burger.forensics.analytics.ingestion.v1.StartAnalysisSessionRequest;
import de.burger.forensics.analytics.ingestion.v1.UploadAnalysisDataResponse;
import de.burger.forensics.analytics.ingestion.v1.WorkspacePolicy;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForensicIngestionGrpcServiceTest {
    private final FakeIngestionUseCase useCase = new FakeIngestionUseCase();
    private final FakeRepositoryAnalysisUseCase repositoryAnalysisUseCase = new FakeRepositoryAnalysisUseCase();
    private Server server;
    private ManagedChannel channel;
    private ForensicIngestionServiceGrpc.ForensicIngestionServiceBlockingStub blockingStub;
    private ForensicIngestionServiceGrpc.ForensicIngestionServiceStub asyncStub;

    @BeforeEach
    void startServer() throws IOException {
        var serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(new ForensicIngestionGrpcService(useCase, repositoryAnalysisUseCase))
            .build()
            .start();
        channel = InProcessChannelBuilder.forName(serverName)
            .directExecutor()
            .build();
        blockingStub = ForensicIngestionServiceGrpc.newBlockingStub(channel);
        asyncStub = ForensicIngestionServiceGrpc.newStub(channel);
    }

    @AfterEach
    void stopServer() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void startAnalysisSessionAcceptsValidRequest() {
        var response = blockingStub.startAnalysisSession(startRequest());

        assertEquals("session-1", response.getSessionId());
        assertEquals(
            de.burger.forensics.analytics.ingestion.v1.IngestionStatus.INGESTION_STATUS_ACCEPTED,
            response.getStatus()
        );
    }

    @Test
    void analyzeRepositoryReturnsSessionWorkspaceAndCheckoutResult() {
        var response = blockingStub.analyzeRepository(analyzeRepositoryRequest());

        assertEquals("analysis-1", response.getAnalysisSessionId().getValue());
        assertEquals("workspace-1", response.getWorkspaceId().getValue());
        assertEquals("https://example.invalid/repo.git", response.getCheckoutResult().getResolvedRemoteUrl());
        assertEquals("main", response.getCheckoutResult().getRequestedBranch());
        assertEquals("abcdef123456", response.getCheckoutResult().getResolvedCommit());
        assertEquals(List.of("/workspace/project/src/main/java"), response.getCheckoutResult().getDetectedSourceRootsList());
        assertEquals("request-1", repositoryAnalysisUseCase.lastCommand.requestId());
        assertEquals("gradle", repositoryAnalysisUseCase.lastCommand.buildContext().buildTool());
    }

    @Test
    void analyzeRepositoryRejectsMissingCheckoutTarget() {
        var invalidRequest = analyzeRepositoryRequest().toBuilder()
            .clearBranch()
            .clearCommit()
            .build();

        var error = assertThrows(
            StatusRuntimeException.class,
            () -> blockingStub.analyzeRepository(invalidRequest)
        );

        assertEquals(Status.Code.INVALID_ARGUMENT, error.getStatus().getCode());
    }

    @Test
    void analyzeRepositoryMapsApplicationFailureToFailedPrecondition() throws Exception {
        restartServerWith(useCase, new FailingRepositoryAnalysisUseCase());

        var error = assertThrows(
            StatusRuntimeException.class,
            () -> blockingStub.analyzeRepository(analyzeRepositoryRequest())
        );

        assertEquals(Status.Code.FAILED_PRECONDITION, error.getStatus().getCode());
    }

    @Test
    void grpcIngestionFlowCreatesAndCompletesApplicationSession() throws Exception {
        var repository = new RecordingIngestionSessionRepository();
        restartServerWith(new DefaultForensicIngestionUseCase(repository));

        var startResponse = blockingStub.startAnalysisSession(startRequest());
        var sessionId = startResponse.getSessionId();
        assertFalse(sessionId.isBlank());
        assertEquals(
            de.burger.forensics.analytics.ingestion.v1.IngestionStatus.INGESTION_STATUS_ACCEPTED,
            startResponse.getStatus()
        );

        var responseObserver = new RecordingStreamObserver<UploadAnalysisDataResponse>();
        StreamObserver<AnalysisDataEnvelope> requestObserver = asyncStub.uploadAnalysisData(responseObserver);
        requestObserver.onNext(envelope(sessionId, "payload-a"));
        requestObserver.onCompleted();

        assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        assertNull(responseObserver.getError());
        assertEquals(1, responseObserver.getValues().getFirst().getReceivedItems());

        var completeResponse = blockingStub.completeAnalysisSession(CompleteAnalysisSessionRequest.newBuilder()
            .setSessionId(sessionId)
            .build());

        var storedSession = repository.findById(sessionId).orElseThrow();
        assertEquals(sessionId, completeResponse.getSessionId());
        assertEquals(
            de.burger.forensics.analytics.ingestion.v1.IngestionStatus.INGESTION_STATUS_COMPLETED,
            completeResponse.getStatus()
        );
        assertEquals("project-a", storedSession.projectId());
        assertEquals("schema-v1", storedSession.schemaVersion());
        assertEquals(IngestionSessionState.COMPLETED, storedSession.state());
        assertEquals(1L, storedSession.receivedItems());
        assertEquals("module-a", repository.lastPayload.moduleName());
        assertEquals("payload-a", repository.lastPayload.payloadDescriptor().payloadId());
    }

    @Test
    void startAnalysisSessionRejectsMissingBuildId() {
        var invalidRequest = startRequest().toBuilder()
            .setBuildIdentity(validBuildIdentity().toBuilder().clearBuildId())
            .build();

        var error = assertThrows(
            StatusRuntimeException.class,
            () -> blockingStub.startAnalysisSession(invalidRequest)
        );
        assertEquals(Status.Code.INVALID_ARGUMENT, error.getStatus().getCode());
    }

    @Test
    void uploadAnalysisDataAcceptsValidStreamAndCountsItems() throws Exception {
        var responseObserver = new RecordingStreamObserver<UploadAnalysisDataResponse>();
        StreamObserver<AnalysisDataEnvelope> requestObserver = asyncStub.uploadAnalysisData(responseObserver);

        requestObserver.onNext(envelope("session-1", "payload-a"));
        requestObserver.onNext(envelope("session-1", "payload-b"));
        requestObserver.onCompleted();

        assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        assertNull(responseObserver.getError());
        var response = responseObserver.getValues().get(0);
        assertEquals("session-1", response.getSessionId());
        assertEquals(2, response.getReceivedItems());
        assertEquals(2, useCase.uploadCount);
        assertEquals(
            de.burger.forensics.analytics.domain.ingestion.AnalysisPayloadKind.SOURCE_FACTS,
            useCase.lastUpload.payloadDescriptor().kind()
        );
    }

    @Test
    void uploadAnalysisDataRejectsMissingPayloadDescriptor() throws Exception {
        var responseObserver = new RecordingStreamObserver<UploadAnalysisDataResponse>();
        StreamObserver<AnalysisDataEnvelope> requestObserver = asyncStub.uploadAnalysisData(responseObserver);

        requestObserver.onNext(envelope("session-1", "payload-a").toBuilder().clearPayloadDescriptor().build());

        assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        assertEquals(Status.Code.INVALID_ARGUMENT, Status.fromThrowable(responseObserver.getError()).getCode());
    }

    @Test
    void uploadObserverIgnoresFurtherEventsAfterValidationFailure() throws Exception {
        var responseObserver = new RecordingStreamObserver<UploadAnalysisDataResponse>();
        StreamObserver<AnalysisDataEnvelope> requestObserver =
            new ForensicIngestionGrpcService(useCase, repositoryAnalysisUseCase).uploadAnalysisData(responseObserver);

        requestObserver.onNext(envelope("session-1", "payload-a").toBuilder().clearPayloadDescriptor().build());
        requestObserver.onNext(envelope("session-1", "payload-a"));
        requestObserver.onCompleted();
        requestObserver.onError(new IllegalStateException("client stream already closed"));

        assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        assertEquals(Status.Code.INVALID_ARGUMENT, Status.fromThrowable(responseObserver.getError()).getCode());
        assertEquals(0, useCase.uploadCount);
    }

    @Test
    void completeAnalysisSessionAcceptsValidSessionId() {
        var response = blockingStub.completeAnalysisSession(CompleteAnalysisSessionRequest.newBuilder()
            .setSessionId("session-1")
            .build());

        assertEquals("session-1", response.getSessionId());
        assertEquals(
            de.burger.forensics.analytics.ingestion.v1.IngestionStatus.INGESTION_STATUS_COMPLETED,
            response.getStatus()
        );
    }

    @Test
    void completeAnalysisSessionMapsMissingApplicationSessionToFailedPrecondition() throws Exception {
        restartServerWith(new FailingIngestionUseCase(IngestionSessionException.missing("session-1")));

        var error = assertThrows(
            StatusRuntimeException.class,
            () -> blockingStub.completeAnalysisSession(CompleteAnalysisSessionRequest.newBuilder()
                .setSessionId("session-1")
                .build())
        );

        assertEquals(Status.Code.FAILED_PRECONDITION, error.getStatus().getCode());
    }

    @Test
    void abortAnalysisSessionAcceptsValidSessionIdAndReason() {
        var response = blockingStub.abortAnalysisSession(AbortAnalysisSessionRequest.newBuilder()
            .setSessionId("session-1")
            .setReason("cancelled")
            .build());

        assertEquals("session-1", response.getSessionId());
        assertEquals(
            de.burger.forensics.analytics.ingestion.v1.IngestionStatus.INGESTION_STATUS_ABORTED,
            response.getStatus()
        );
    }

    @Test
    void abortAnalysisSessionMapsUnexpectedApplicationFailureToInternal() throws Exception {
        restartServerWith(new FailingIngestionUseCase(new IllegalStateException("unexpected")));

        var error = assertThrows(
            StatusRuntimeException.class,
            () -> blockingStub.abortAnalysisSession(AbortAnalysisSessionRequest.newBuilder()
                .setSessionId("session-1")
                .setReason("cancelled")
                .build())
        );

        assertEquals(Status.Code.INTERNAL, error.getStatus().getCode());
    }

    private void restartServerWith(ForensicIngestionUseCase nextUseCase) throws IOException {
        restartServerWith(nextUseCase, repositoryAnalysisUseCase);
    }

    private void restartServerWith(
        ForensicIngestionUseCase nextUseCase,
        RepositoryAnalysisIngestionUseCase nextRepositoryAnalysisUseCase
    ) throws IOException {
        stopServer();
        var serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(new ForensicIngestionGrpcService(nextUseCase, nextRepositoryAnalysisUseCase))
            .build()
            .start();
        channel = InProcessChannelBuilder.forName(serverName)
            .directExecutor()
            .build();
        blockingStub = ForensicIngestionServiceGrpc.newBlockingStub(channel);
        asyncStub = ForensicIngestionServiceGrpc.newStub(channel);
    }

    private StartAnalysisSessionRequest startRequest() {
        return StartAnalysisSessionRequest.newBuilder()
            .setBuildIdentity(validBuildIdentity())
            .setPluginIdentity(validPluginIdentity())
            .setSchemaVersion("schema-v1")
            .build();
    }

    private AnalyzeRepositoryRequest analyzeRepositoryRequest() {
        return AnalyzeRepositoryRequest.newBuilder()
            .setRepository(RepositoryReference.newBuilder()
                .setRemoteUrl("https://example.invalid/repo.git")
                .setProvider("github")
                .putAttributes("visibility", "public"))
            .setBranch(BranchReference.newBuilder()
                .setName("main")
                .setRequired(true))
            .setCommit(CommitReference.newBuilder()
                .setHash("abcdef")
                .setRequired(false))
            .setWorkspacePolicy(WorkspacePolicy.newBuilder()
                .setEphemeral(true)
                .setAllowShallowClone(false)
                .setAllowPartialClone(false)
                .setAllowSparseCheckout(false)
                .setTimeoutSeconds(60)
                .setMaxWorkspaceBytes(0))
            .setBuildContext(BuildContext.newBuilder()
                .setBuildTool("gradle")
                .setBuildId("build-1")
                .setRootProjectName("project")
                .addDeclaredModules(":app"))
            .setRequestId("request-1")
            .setSchemaVersion("schema-v1")
            .build();
    }

    private AnalysisDataEnvelope envelope(String sessionId, String payloadId) {
        return AnalysisDataEnvelope.newBuilder()
            .setSessionId(sessionId)
            .setBuildIdentity(validBuildIdentity())
            .setModuleIdentity(ModuleIdentity.newBuilder()
                .setModuleName("module-a")
                .setModulePath(":module-a"))
            .setPluginIdentity(validPluginIdentity())
            .setSchemaVersion("schema-v1")
            .setPayloadDescriptor(payloadDescriptor(payloadId))
            .setPayload(ByteString.copyFromUtf8("{}"))
            .build();
    }

    private AnalysisPayloadDescriptor payloadDescriptor(String payloadId) {
        return AnalysisPayloadDescriptor.newBuilder()
            .setPayloadId(payloadId)
            .setKind(AnalysisPayloadKind.ANALYSIS_PAYLOAD_KIND_SOURCE_FACTS)
            .setContentType("application/json")
            .putAttributes("schema", "source-facts-v1")
            .build();
    }

    private BuildIdentity validBuildIdentity() {
        return BuildIdentity.newBuilder()
            .setProjectId("project-a")
            .setRepositoryUrl("https://example.invalid/repo.git")
            .setBranchName("main")
            .setCommitHash("abcdef")
            .setBuildId("build-1")
            .setScanTimestamp("2026-05-09T12:00:00Z")
            .build();
    }

    private PluginIdentity validPluginIdentity() {
        return PluginIdentity.newBuilder()
            .setPluginName("forensic-plugin")
            .setPluginVersion("0.1.0")
            .build();
    }

    private static final class FakeIngestionUseCase implements ForensicIngestionUseCase {
        private int uploadCount;
        private UploadAnalysisDataCommand lastUpload;

        @Override
        public StartAnalysisSessionResult start(StartAnalysisSessionCommand command) {
            return new StartAnalysisSessionResult("session-1", IngestionStatus.ACCEPTED, "accepted");
        }

        @Override
        public UploadAnalysisDataResult upload(UploadAnalysisDataCommand command) {
            uploadCount++;
            lastUpload = command;
            return new UploadAnalysisDataResult(command.sessionId(), IngestionStatus.ACCEPTED, uploadCount, "accepted");
        }

        @Override
        public CompleteAnalysisSessionResult complete(CompleteAnalysisSessionCommand command) {
            return new CompleteAnalysisSessionResult(command.sessionId(), IngestionStatus.COMPLETED, "completed");
        }

        @Override
        public AbortAnalysisSessionResult abort(AbortAnalysisSessionCommand command) {
            return new AbortAnalysisSessionResult(command.sessionId(), IngestionStatus.ABORTED, "aborted");
        }
    }

    private static final class FailingIngestionUseCase implements ForensicIngestionUseCase {
        private final RuntimeException failure;

        private FailingIngestionUseCase(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public StartAnalysisSessionResult start(StartAnalysisSessionCommand command) {
            throw failure;
        }

        @Override
        public UploadAnalysisDataResult upload(UploadAnalysisDataCommand command) {
            throw failure;
        }

        @Override
        public CompleteAnalysisSessionResult complete(CompleteAnalysisSessionCommand command) {
            throw failure;
        }

        @Override
        public AbortAnalysisSessionResult abort(AbortAnalysisSessionCommand command) {
            throw failure;
        }
    }

    private static final class FakeRepositoryAnalysisUseCase implements RepositoryAnalysisIngestionUseCase {
        private AnalyzeRepositoryCommand lastCommand;

        @Override
        public AnalyzeRepositoryResult analyze(AnalyzeRepositoryCommand command) {
            lastCommand = command;
            return new AnalyzeRepositoryResult(
                new AnalysisRunId("analysis-1"),
                new WorkspaceId("workspace-1"),
                new CheckoutResult(
                    "https://example.invalid/repo.git",
                    Optional.of("main"),
                    Optional.of("abcdef"),
                    "abcdef123456",
                    List.of(new SourceRoot("/workspace/project/src/main/java")),
                    "CHECKED_OUT",
                    List.of("checkout mode: full clone")
                ),
                "Repository analysis session registered"
            );
        }
    }

    private static final class FailingRepositoryAnalysisUseCase implements RepositoryAnalysisIngestionUseCase {
        @Override
        public AnalyzeRepositoryResult analyze(AnalyzeRepositoryCommand command) {
            throw new RepositoryAnalysisIngestionException("workspace preparation failed");
        }
    }

    private static final class RecordingIngestionSessionRepository implements IngestionSessionRepository {
        private final Map<String, IngestionSession> sessions = new HashMap<>();
        private final Map<String, Long> payloadCounts = new HashMap<>();
        private IngestionPayload lastPayload;

        @Override
        public void save(IngestionSession session) {
            sessions.put(session.sessionId(), session);
            payloadCounts.put(session.sessionId(), 0L);
        }

        @Override
        public Optional<IngestionSession> findById(String sessionId) {
            return Optional.ofNullable(sessions.get(sessionId));
        }

        @Override
        public void update(IngestionSession session) {
            sessions.put(session.sessionId(), session);
        }

        @Override
        public long appendPayload(IngestionPayload payload) {
            lastPayload = payload;
            var nextCount = payloadCounts.getOrDefault(payload.sessionId(), 0L) + 1L;
            payloadCounts.put(payload.sessionId(), nextCount);
            return nextCount;
        }
    }

    private static final class RecordingStreamObserver<T> implements StreamObserver<T> {
        private final CountDownLatch completion = new CountDownLatch(1);
        private final List<T> values = new ArrayList<>();
        private Throwable error;

        @Override
        public void onNext(T value) {
            values.add(value);
        }

        @Override
        public void onError(Throwable throwable) {
            error = throwable;
            completion.countDown();
        }

        @Override
        public void onCompleted() {
            completion.countDown();
        }

        boolean awaitCompletion(long timeout, TimeUnit unit) throws InterruptedException {
            return completion.await(timeout, unit);
        }

        List<T> getValues() {
            return values;
        }

        Throwable getError() {
            return error;
        }
    }
}
