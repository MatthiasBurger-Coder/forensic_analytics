package de.burger.forensics.analytics.services.ingestion.adapter.in.grpc;

import de.burger.forensics.analytics.ingestion.v1.AnalyzeRepositoryRequest;
import de.burger.forensics.analytics.ingestion.v1.BranchReference;
import de.burger.forensics.analytics.ingestion.v1.BuildContext;
import de.burger.forensics.analytics.ingestion.v1.CommitReference;
import de.burger.forensics.analytics.ingestion.v1.CompleteAnalysisSessionRequest;
import de.burger.forensics.analytics.ingestion.v1.ForensicIngestionServiceGrpc;
import de.burger.forensics.analytics.ingestion.v1.IngestionStatus;
import de.burger.forensics.analytics.ingestion.v1.AbortAnalysisSessionRequest;
import de.burger.forensics.analytics.ingestion.v1.AnalysisPayloadKind;
import de.burger.forensics.analytics.ingestion.v1.RepositoryReference;
import de.burger.forensics.analytics.ingestion.v1.StartAnalysisSessionRequest;
import de.burger.forensics.analytics.ingestion.v1.UploadAnalysisDataResponse;
import de.burger.forensics.analytics.ingestion.v1.WorkspacePolicy;
import de.burger.forensics.analytics.services.ingestion.adapter.out.memory.InMemoryIngestionSessionRepository;
import de.burger.forensics.analytics.services.ingestion.adapter.out.memory.NoOpAcceptedIngestionHandoffPort;
import de.burger.forensics.analytics.services.ingestion.application.IngestionApplicationService;
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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static de.burger.forensics.analytics.services.ingestion.adapter.in.grpc.ForensicIngestionRequestValidatorTest.envelope;
import static de.burger.forensics.analytics.services.ingestion.adapter.in.grpc.ForensicIngestionRequestValidatorTest.buildIdentity;
import static de.burger.forensics.analytics.services.ingestion.adapter.in.grpc.ForensicIngestionRequestValidatorTest.pluginIdentity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForensicIngestionGrpcEndpointTest {
    private Server server;
    private ManagedChannel channel;
    private ForensicIngestionServiceGrpc.ForensicIngestionServiceBlockingStub blockingStub;
    private ForensicIngestionServiceGrpc.ForensicIngestionServiceStub asyncStub;

    @BeforeEach
    void startServer() throws IOException {
        var serverName = InProcessServerBuilder.generateName();
        var applicationService = new IngestionApplicationService(
            new InMemoryIngestionSessionRepository(),
            new NoOpAcceptedIngestionHandoffPort()
        );
        server = InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(new ForensicIngestionGrpcEndpoint(applicationService))
            .build()
            .start();
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        blockingStub = ForensicIngestionServiceGrpc.newBlockingStub(channel);
        asyncStub = ForensicIngestionServiceGrpc.newStub(channel);
    }

    @AfterEach
    void stopServer() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void startsUploadsDeduplicatesAndCompletesSession() throws Exception {
        var start = blockingStub.startAnalysisSession(startRequest());
        var responseObserver = new RecordingStreamObserver<UploadAnalysisDataResponse>();
        StreamObserver<de.burger.forensics.analytics.ingestion.v1.AnalysisDataEnvelope> requestObserver =
            asyncStub.uploadAnalysisData(responseObserver);

        requestObserver.onNext(envelope(start.getSessionId(), "payload-a"));
        requestObserver.onNext(envelope(start.getSessionId(), "payload-a"));
        requestObserver.onCompleted();

        assertTrue(responseObserver.awaitCompletion());
        assertNull(responseObserver.error);
        assertEquals(1, responseObserver.values.getFirst().getReceivedItems());

        var complete = blockingStub.completeAnalysisSession(CompleteAnalysisSessionRequest.newBuilder()
            .setSessionId(start.getSessionId())
            .build());
        assertEquals(IngestionStatus.INGESTION_STATUS_COMPLETED, complete.getStatus());
    }

    @Test
    void acceptsAllDeclaredPayloadKinds() throws Exception {
        var start = blockingStub.startAnalysisSession(startRequest());
        var responseObserver = new RecordingStreamObserver<UploadAnalysisDataResponse>();
        StreamObserver<de.burger.forensics.analytics.ingestion.v1.AnalysisDataEnvelope> requestObserver =
            asyncStub.uploadAnalysisData(responseObserver);

        requestObserver.onNext(envelopeWithKind(
            start.getSessionId(),
            "payload-source",
            AnalysisPayloadKind.ANALYSIS_PAYLOAD_KIND_SOURCE_FACTS
        ));
        requestObserver.onNext(envelopeWithKind(
            start.getSessionId(),
            "payload-semantic",
            AnalysisPayloadKind.ANALYSIS_PAYLOAD_KIND_SEMANTIC_ARTIFACTS
        ));
        requestObserver.onNext(envelopeWithKind(
            start.getSessionId(),
            "payload-rule",
            AnalysisPayloadKind.ANALYSIS_PAYLOAD_KIND_RULE_ARTIFACTS
        ));
        requestObserver.onNext(envelopeWithKind(
            start.getSessionId(),
            "payload-runtime",
            AnalysisPayloadKind.ANALYSIS_PAYLOAD_KIND_RUNTIME_TRACE
        ));
        requestObserver.onNext(envelopeWithKind(
            start.getSessionId(),
            "payload-report",
            AnalysisPayloadKind.ANALYSIS_PAYLOAD_KIND_DIAGNOSTIC_REPORT
        ));
        requestObserver.onCompleted();

        assertTrue(responseObserver.awaitCompletion());
        assertNull(responseObserver.error);
        assertEquals(5, responseObserver.values.getFirst().getReceivedItems());
    }

    @Test
    void analyzeRepositoryIsExplicitlyOutsideIngestionScope() {
        var error = assertThrows(
            StatusRuntimeException.class,
            () -> blockingStub.analyzeRepository(analyzeRepositoryRequest())
        );
        var malformedError = assertThrows(
            StatusRuntimeException.class,
            () -> blockingStub.analyzeRepository(AnalyzeRepositoryRequest.getDefaultInstance())
        );

        assertEquals(Status.Code.UNIMPLEMENTED, error.getStatus().getCode());
        assertEquals(Status.Code.UNIMPLEMENTED, malformedError.getStatus().getCode());
    }

    @Test
    void mapsSessionLifecycleErrorsToGrpcStatuses() {
        var start = blockingStub.startAnalysisSession(startRequest());
        var abort = blockingStub.abortAnalysisSession(AbortAnalysisSessionRequest.newBuilder()
            .setSessionId(start.getSessionId())
            .setReason("operator aborted")
            .build());
        var missing = assertThrows(
            StatusRuntimeException.class,
            () -> blockingStub.completeAnalysisSession(CompleteAnalysisSessionRequest.newBuilder()
                .setSessionId("missing-session")
                .build())
        );
        var invalidState = assertThrows(
            StatusRuntimeException.class,
            () -> blockingStub.completeAnalysisSession(CompleteAnalysisSessionRequest.newBuilder()
                .setSessionId(start.getSessionId())
                .build())
        );

        assertEquals(IngestionStatus.INGESTION_STATUS_ABORTED, abort.getStatus());
        assertEquals(Status.Code.NOT_FOUND, missing.getStatus().getCode());
        assertEquals(Status.Code.FAILED_PRECONDITION, invalidState.getStatus().getCode());
    }

    @Test
    void uploadRejectsMissingPayloadDescriptor() throws Exception {
        var start = blockingStub.startAnalysisSession(startRequest());
        var responseObserver = new RecordingStreamObserver<UploadAnalysisDataResponse>();
        StreamObserver<de.burger.forensics.analytics.ingestion.v1.AnalysisDataEnvelope> requestObserver =
            asyncStub.uploadAnalysisData(responseObserver);

        requestObserver.onNext(envelope(start.getSessionId(), "payload-a").toBuilder()
            .clearPayloadDescriptor()
            .setPayloadType("SOURCE_FACTS")
            .build());

        assertTrue(responseObserver.awaitCompletion());
        assertEquals(Status.Code.INVALID_ARGUMENT, Status.fromThrowable(responseObserver.error).getCode());
    }

    @Test
    void emptyUploadStreamIsRejectedWithoutInventingSessionEvidence() throws Exception {
        var responseObserver = new RecordingStreamObserver<UploadAnalysisDataResponse>();
        StreamObserver<de.burger.forensics.analytics.ingestion.v1.AnalysisDataEnvelope> requestObserver =
            asyncStub.uploadAnalysisData(responseObserver);

        requestObserver.onCompleted();

        assertTrue(responseObserver.awaitCompletion());
        assertEquals(Status.Code.INVALID_ARGUMENT, Status.fromThrowable(responseObserver.error).getCode());
        assertTrue(responseObserver.values.isEmpty());
    }

    private static StartAnalysisSessionRequest startRequest() {
        return StartAnalysisSessionRequest.newBuilder()
            .setBuildIdentity(buildIdentity())
            .setPluginIdentity(pluginIdentity())
            .setSchemaVersion("schema-v1")
            .build();
    }

    private static AnalyzeRepositoryRequest analyzeRepositoryRequest() {
        return AnalyzeRepositoryRequest.newBuilder()
            .setRepository(RepositoryReference.newBuilder()
                .setRemoteUrl("https://example.invalid/repo.git")
                .setProvider("git"))
            .setBranch(BranchReference.newBuilder()
                .setName("main")
                .setRequired(true))
            .setCommit(CommitReference.newBuilder()
                .setHash("abcdef")
                .setRequired(false))
            .setWorkspacePolicy(WorkspacePolicy.newBuilder()
                .setTimeoutSeconds(60)
                .setMaxWorkspaceBytes(0))
            .setBuildContext(BuildContext.newBuilder()
                .setBuildTool("gradle")
                .setBuildId("build-1")
                .setRootProjectName("project-a")
                .addDeclaredModules(":module-a"))
            .setRequestId("request-1")
            .setSchemaVersion("schema-v1")
            .build();
    }

    private static de.burger.forensics.analytics.ingestion.v1.AnalysisDataEnvelope envelopeWithKind(
        String sessionId,
        String payloadId,
        AnalysisPayloadKind kind
    ) {
        return envelope(sessionId, payloadId).toBuilder()
            .setPayloadDescriptor(ForensicIngestionRequestValidatorTest.payloadDescriptor(payloadId).toBuilder()
                .setKind(kind))
            .build();
    }

    private static final class RecordingStreamObserver<T> implements StreamObserver<T> {
        private final CountDownLatch latch = new CountDownLatch(1);
        private final List<T> values = new ArrayList<>();
        private Throwable error;

        @Override
        public void onNext(T value) {
            values.add(value);
        }

        @Override
        public void onError(Throwable throwable) {
            error = throwable;
            latch.countDown();
        }

        @Override
        public void onCompleted() {
            latch.countDown();
        }

        private boolean awaitCompletion() throws InterruptedException {
            return latch.await(5, TimeUnit.SECONDS);
        }
    }
}
