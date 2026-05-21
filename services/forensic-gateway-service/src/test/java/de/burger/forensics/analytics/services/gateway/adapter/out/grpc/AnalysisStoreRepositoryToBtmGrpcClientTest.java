package de.burger.forensics.analytics.services.gateway.adapter.out.grpc;

import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobId;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobServiceGrpc;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisRunId;
import de.burger.forensics.analytics.analysisjob.v1.BtmDeliveryReadiness;
import de.burger.forensics.analytics.analysisjob.v1.GetRepositoryToBtmStatusRequest;
import de.burger.forensics.analytics.analysisjob.v1.OperationStatus;
import de.burger.forensics.analytics.analysisjob.v1.RepositoryToBtmDiagnostic;
import de.burger.forensics.analytics.analysisjob.v1.RepositoryToBtmDiagnosticSeverity;
import de.burger.forensics.analytics.analysisjob.v1.RepositoryToBtmOrchestrationState;
import de.burger.forensics.analytics.analysisjob.v1.RepositoryToBtmOrchestrationStatus;
import de.burger.forensics.analytics.analysisjob.v1.SourceSnapshotId;
import de.burger.forensics.analytics.analysisjob.v1.StartRepositoryToBtmRequest;
import de.burger.forensics.analytics.services.gateway.application.GatewayRepositoryAnalysisException;
import de.burger.forensics.analytics.services.gateway.domain.GatewayRepositoryAnalysis.BuildContext;
import de.burger.forensics.analytics.services.gateway.domain.GatewayRepositoryAnalysis.StatusRequest;
import de.burger.forensics.analytics.services.gateway.domain.GatewayRepositoryAnalysis.SubmissionRequest;
import de.burger.forensics.analytics.services.gateway.domain.GatewayRepositoryAnalysis.WorkspacePolicy;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnalysisStoreRepositoryToBtmGrpcClientTest {
    @Test
    void mapsGatewaySubmissionToAnalysisStoreOwnerRequest() {
        var service = new CapturingAnalysisStoreService();
        var fixture = GrpcFixture.start(service);
        try {
            var client = new AnalysisStoreRepositoryToBtmGrpcClient(
                AnalysisJobServiceGrpc.newBlockingStub(fixture.channel),
                30
            );

            var request = request("main", "");
            var result = client.start(request);

            assertEquals(request.analysisRunId(), result.analysisRunId());
            assertEquals("ACCEPTED", result.status());
            assertEquals("BTM_DELIVERY_NOT_READY", result.btmDeliveryStatus());
            assertEquals("request-1", service.request.getRequestId());
            assertEquals("idem-1", service.request.getIdempotencyKey());
            assertEquals("correlation-1", service.request.getCorrelationId());
            assertEquals("https://example.com/acme/demo.git", service.request.getRepository().getRemoteUrl());
            assertEquals("main", service.request.getRevision().getBranch());
            assertEquals("", service.request.getRevision().getCommit());
            assertEquals(60, service.request.getWorkspacePolicy().getTimeoutSeconds());
            assertEquals("ORCHESTRATION_ACCEPTED", result.diagnostics().getFirst().code());
        } finally {
            fixture.close();
        }
    }

    @Test
    void redactsAnalysisStoreDiagnosticsBeforeReturningPublicGatewaySubmission() {
        var service = new CapturingAnalysisStoreService();
        service.diagnosticMessage = "git clone https://example.com/private.git failed in /tmp/workspace-1 with token=abc";
        var fixture = GrpcFixture.start(service);
        try {
            var client = new AnalysisStoreRepositoryToBtmGrpcClient(
                AnalysisJobServiceGrpc.newBlockingStub(fixture.channel),
                30
            );

            var result = client.start(request("", "abcdef1"));

            assertEquals("Diagnostic details redacted", result.diagnostics().getFirst().message());
            assertFalse(result.toString().contains("/tmp"));
            assertFalse(result.toString().contains("workspace-"));
            assertFalse(result.toString().contains("token="));
            assertFalse(result.toString().contains("https://example.com/private.git"));
        } finally {
            fixture.close();
        }
    }

    @Test
    void readsRepositoryToBtmStatusThroughAnalysisStoreOwnerApi() {
        var service = new CapturingAnalysisStoreService();
        var fixture = GrpcFixture.start(service);
        try {
            var client = new AnalysisStoreRepositoryToBtmGrpcClient(
                AnalysisJobServiceGrpc.newBlockingStub(fixture.channel),
                30
            );

            var result = client.status(new StatusRequest("request-status", "correlation-status", "analysis-run-1"));

            assertEquals("request-status", service.statusRequest.getRequestId());
            assertEquals("correlation-status", service.statusRequest.getCorrelationId());
            assertEquals("analysis-run-1", service.statusRequest.getAnalysisRunId().getValue());
            assertEquals("analysis-run-1", result.analysisRunId());
            assertEquals("correlation-status", result.correlationId());
        } finally {
            fixture.close();
        }
    }

    @Test
    void mapsOwnerReadinessStatesToPublicGatewayStatuses() {
        assertPublicStatus(
            RepositoryToBtmOrchestrationState.REPOSITORY_TO_BTM_ORCHESTRATION_STATE_READY_FOR_BTM_DELIVERY,
            BtmDeliveryReadiness.BTM_DELIVERY_READINESS_READY,
            "COMPLETED",
            "BTM_DELIVERY_READY"
        );
        assertPublicStatus(
            RepositoryToBtmOrchestrationState.REPOSITORY_TO_BTM_ORCHESTRATION_STATE_FAILED,
            BtmDeliveryReadiness.BTM_DELIVERY_READINESS_UNAVAILABLE,
            "FAILED",
            "BTM_DELIVERY_UNAVAILABLE"
        );
        assertPublicStatus(
            RepositoryToBtmOrchestrationState.REPOSITORY_TO_BTM_ORCHESTRATION_STATE_UNSPECIFIED,
            BtmDeliveryReadiness.BTM_DELIVERY_READINESS_UNKNOWN,
            "UNKNOWN",
            "BTM_DELIVERY_UNKNOWN"
        );
        assertPublicStatus(
            RepositoryToBtmOrchestrationState.REPOSITORY_TO_BTM_ORCHESTRATION_STATE_INCOMPLETE,
            BtmDeliveryReadiness.BTM_DELIVERY_READINESS_UNSPECIFIED,
            "ACCEPTED",
            "BTM_DELIVERY_UNKNOWN"
        );
    }

    @Test
    void mapsAllGrpcFailuresAndClosesOwnedChannelsWithoutLeakingDescriptions() {
        assertGatewayError(Status.INVALID_ARGUMENT, 400, "VALIDATION_ERROR", false);
        assertGatewayError(Status.ALREADY_EXISTS, 409, "CONFLICT", false);
        assertGatewayError(Status.NOT_FOUND, 404, "NOT_FOUND", false);
        assertGatewayError(Status.DEADLINE_EXCEEDED, 504, "TIMEOUT", true);
        assertGatewayError(Status.UNAVAILABLE, 503, "BACKEND_UNAVAILABLE", true);
        assertGatewayError(Status.FAILED_PRECONDITION, 502, "BACKEND_UNAVAILABLE", false);
        assertGatewayError(Status.INTERNAL, 500, "UNEXPECTED_ERROR", false);

        var client = new AnalysisStoreRepositoryToBtmGrpcClient("127.0.0.1", 1, 1);
        client.close();
    }

    private static SubmissionRequest request(String branch, String commit) {
        return new SubmissionRequest(
            "request-1",
            "idem-1",
            "gateway.v1",
            "correlation-1",
            List.of("BTM_RULES"),
            "https://example.com/acme/demo.git",
            "github",
            branch,
            commit,
            "",
            new BuildContext("gradle", "build-1", "demo", List.of(":app"), Map.of("tenant", "demo")),
            new WorkspacePolicy(false, true, false, false, 60, 100_000)
        );
    }

    private static void assertGatewayError(
        Status status,
        int expectedStatusCode,
        String expectedErrorCode,
        boolean expectedRetryable
    ) {
        var fixture = GrpcFixture.start(new FailingAnalysisStoreService(status));
        try {
            var client = new AnalysisStoreRepositoryToBtmGrpcClient(
                AnalysisJobServiceGrpc.newBlockingStub(fixture.channel),
                30
            );

            var failure = assertThrows(GatewayRepositoryAnalysisException.class, () -> client.start(request("", "abcdef1")));

            assertEquals(expectedStatusCode, failure.statusCode());
            assertEquals(expectedErrorCode, failure.errorCode());
            assertEquals(expectedRetryable, failure.retryable());
            assertFalse(failure.getMessage().contains("/tmp"));
        } finally {
            fixture.close();
        }
    }

    private static void assertPublicStatus(
        RepositoryToBtmOrchestrationState state,
        BtmDeliveryReadiness readiness,
        String expectedStatus,
        String expectedReadiness
    ) {
        var service = new CapturingAnalysisStoreService();
        service.state = state;
        service.readiness = readiness;
        var fixture = GrpcFixture.start(service);
        try {
            var client = new AnalysisStoreRepositoryToBtmGrpcClient(
                AnalysisJobServiceGrpc.newBlockingStub(fixture.channel),
                30
            );

            var result = client.start(request("main", ""));

            assertEquals(expectedStatus, result.status());
            assertEquals(expectedReadiness, result.btmDeliveryStatus());
        } finally {
            fixture.close();
        }
    }

    private static final class CapturingAnalysisStoreService
        extends AnalysisJobServiceGrpc.AnalysisJobServiceImplBase {
        private StartRepositoryToBtmRequest request;
        private GetRepositoryToBtmStatusRequest statusRequest;
        private String diagnosticMessage = "Analysis Store accepted orchestration";
        private RepositoryToBtmOrchestrationState state =
            RepositoryToBtmOrchestrationState.REPOSITORY_TO_BTM_ORCHESTRATION_STATE_WAITING_FOR_REPOSITORY;
        private BtmDeliveryReadiness readiness = BtmDeliveryReadiness.BTM_DELIVERY_READINESS_NOT_READY;

        @Override
        public void startRepositoryToBtm(
            StartRepositoryToBtmRequest request,
            StreamObserver<RepositoryToBtmOrchestrationStatus> responseObserver
        ) {
            this.request = request;
            responseObserver.onNext(RepositoryToBtmOrchestrationStatus.newBuilder()
                .setStatus(OperationStatus.newBuilder()
                    .setCode("REPOSITORY_TO_BTM_ACCEPTED")
                    .setMessage("accepted")
                    .setCorrelationId(request.getCorrelationId()))
                .setAnalysisRunId(AnalysisRunId.newBuilder().setValue(request.getAnalysisRunId().getValue()))
                .setRepositoryAnalysisJobId(AnalysisJobId.newBuilder().setValue("repository-analysis-job-1"))
                .setSourceSnapshotId(SourceSnapshotId.newBuilder().setValue("source-snapshot-pending-1"))
                .setCompleteness(de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.ANALYSIS_COMPLETENESS_INCOMPLETE)
                .setState(state)
                .setBtmDeliveryReadiness(readiness)
                .setJoernSkipped(true)
                .addDiagnostics(RepositoryToBtmDiagnostic.newBuilder()
                    .setCode("ORCHESTRATION_ACCEPTED")
                    .setMessage(diagnosticMessage)
                    .setSeverity(RepositoryToBtmDiagnosticSeverity.REPOSITORY_TO_BTM_DIAGNOSTIC_SEVERITY_INFO))
                .build());
            responseObserver.onCompleted();
        }

        @Override
        public void getRepositoryToBtmStatus(
            GetRepositoryToBtmStatusRequest request,
            StreamObserver<RepositoryToBtmOrchestrationStatus> responseObserver
        ) {
            statusRequest = request;
            responseObserver.onNext(status(request.getAnalysisRunId().getValue(), request.getCorrelationId()));
            responseObserver.onCompleted();
        }

        private RepositoryToBtmOrchestrationStatus status(String analysisRunId, String correlationId) {
            return RepositoryToBtmOrchestrationStatus.newBuilder()
                .setStatus(OperationStatus.newBuilder()
                    .setCode("REPOSITORY_TO_BTM_ACCEPTED")
                    .setMessage("accepted")
                    .setCorrelationId(correlationId))
                .setAnalysisRunId(AnalysisRunId.newBuilder().setValue(analysisRunId))
                .setRepositoryAnalysisJobId(AnalysisJobId.newBuilder().setValue("repository-analysis-job-1"))
                .setSourceSnapshotId(SourceSnapshotId.newBuilder().setValue("source-snapshot-pending-1"))
                .setCompleteness(de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.ANALYSIS_COMPLETENESS_INCOMPLETE)
                .setState(state)
                .setBtmDeliveryReadiness(readiness)
                .setJoernSkipped(true)
                .addDiagnostics(RepositoryToBtmDiagnostic.newBuilder()
                    .setCode("ORCHESTRATION_STATUS")
                    .setMessage(diagnosticMessage)
                    .setSeverity(RepositoryToBtmDiagnosticSeverity.REPOSITORY_TO_BTM_DIAGNOSTIC_SEVERITY_INFO))
                .build();
        }
    }

    private static final class FailingAnalysisStoreService
        extends AnalysisJobServiceGrpc.AnalysisJobServiceImplBase {
        private final Status status;

        private FailingAnalysisStoreService(Status status) {
            this.status = status;
        }

        @Override
        public void startRepositoryToBtm(
            StartRepositoryToBtmRequest request,
            StreamObserver<RepositoryToBtmOrchestrationStatus> responseObserver
        ) {
            responseObserver.onError(new StatusRuntimeException(status.withDescription("private /tmp/workspace")));
        }
    }

    private record GrpcFixture(Server server, ManagedChannel channel) implements AutoCloseable {
        private static GrpcFixture start(AnalysisJobServiceGrpc.AnalysisJobServiceImplBase service) {
            try {
                var serverName = InProcessServerBuilder.generateName();
                var server = InProcessServerBuilder.forName(serverName)
                    .directExecutor()
                    .addService(service)
                    .build()
                    .start();
                var channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
                return new GrpcFixture(server, channel);
            } catch (IOException error) {
                throw new IllegalStateException("failed to start in-process gRPC fixture", error);
            }
        }

        @Override
        public void close() {
            channel.shutdownNow();
            server.shutdownNow();
        }
    }
}
