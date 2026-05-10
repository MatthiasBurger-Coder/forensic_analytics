package de.burger.forensics.analytics.ingestion.grpc;

import com.google.protobuf.ByteString;
import de.burger.forensics.analytics.application.ingestion.ForensicIngestionUseCase;
import de.burger.forensics.analytics.application.ingestion.IngestionSessionException;
import de.burger.forensics.analytics.application.ingestion.command.AbortAnalysisSessionCommand;
import de.burger.forensics.analytics.application.ingestion.command.CompleteAnalysisSessionCommand;
import de.burger.forensics.analytics.application.ingestion.command.StartAnalysisSessionCommand;
import de.burger.forensics.analytics.application.ingestion.command.UploadAnalysisDataCommand;
import de.burger.forensics.analytics.application.ingestion.result.AbortAnalysisSessionResult;
import de.burger.forensics.analytics.application.ingestion.result.CompleteAnalysisSessionResult;
import de.burger.forensics.analytics.application.ingestion.result.IngestionStatus;
import de.burger.forensics.analytics.application.ingestion.result.StartAnalysisSessionResult;
import de.burger.forensics.analytics.application.ingestion.result.UploadAnalysisDataResult;
import de.burger.forensics.analytics.ingestion.v1.AbortAnalysisSessionRequest;
import de.burger.forensics.analytics.ingestion.v1.AnalysisDataEnvelope;
import de.burger.forensics.analytics.ingestion.v1.BuildIdentity;
import de.burger.forensics.analytics.ingestion.v1.CompleteAnalysisSessionRequest;
import de.burger.forensics.analytics.ingestion.v1.ForensicIngestionServiceGrpc;
import de.burger.forensics.analytics.ingestion.v1.ModuleIdentity;
import de.burger.forensics.analytics.ingestion.v1.PluginIdentity;
import de.burger.forensics.analytics.ingestion.v1.StartAnalysisSessionRequest;
import de.burger.forensics.analytics.ingestion.v1.UploadAnalysisDataResponse;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForensicIngestionGrpcServiceTest {
    private final FakeIngestionUseCase useCase = new FakeIngestionUseCase();
    private Server server;
    private ManagedChannel channel;
    private ForensicIngestionServiceGrpc.ForensicIngestionServiceBlockingStub blockingStub;
    private ForensicIngestionServiceGrpc.ForensicIngestionServiceStub asyncStub;

    @BeforeEach
    void startServer() throws IOException {
        var serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(new ForensicIngestionGrpcService(useCase))
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
    }

    @Test
    void uploadAnalysisDataRejectsEmptyPayloadType() throws Exception {
        var responseObserver = new RecordingStreamObserver<UploadAnalysisDataResponse>();
        StreamObserver<AnalysisDataEnvelope> requestObserver = asyncStub.uploadAnalysisData(responseObserver);

        requestObserver.onNext(envelope("session-1", ""));

        assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS));
        assertEquals(Status.Code.INVALID_ARGUMENT, Status.fromThrowable(responseObserver.getError()).getCode());
    }

    @Test
    void uploadObserverIgnoresFurtherEventsAfterValidationFailure() throws Exception {
        var responseObserver = new RecordingStreamObserver<UploadAnalysisDataResponse>();
        StreamObserver<AnalysisDataEnvelope> requestObserver =
            new ForensicIngestionGrpcService(useCase).uploadAnalysisData(responseObserver);

        requestObserver.onNext(envelope("session-1", ""));
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
        stopServer();
        var serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(new ForensicIngestionGrpcService(nextUseCase))
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

    private AnalysisDataEnvelope envelope(String sessionId, String payloadType) {
        return AnalysisDataEnvelope.newBuilder()
            .setSessionId(sessionId)
            .setBuildIdentity(validBuildIdentity())
            .setModuleIdentity(ModuleIdentity.newBuilder()
                .setModuleName("module-a")
                .setModulePath(":module-a"))
            .setPluginIdentity(validPluginIdentity())
            .setSchemaVersion("schema-v1")
            .setPayloadType(payloadType)
            .setPayload(ByteString.copyFromUtf8("{}"))
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

        @Override
        public StartAnalysisSessionResult start(StartAnalysisSessionCommand command) {
            return new StartAnalysisSessionResult("session-1", IngestionStatus.ACCEPTED, "accepted");
        }

        @Override
        public UploadAnalysisDataResult upload(UploadAnalysisDataCommand command) {
            uploadCount++;
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
