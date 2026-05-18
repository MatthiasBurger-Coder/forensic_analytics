package de.burger.forensics.analytics.services.gateway.adapter.out.grpc;

import de.burger.forensics.analytics.repositoryanalysis.v1.CheckoutResult;
import de.burger.forensics.analytics.repositoryanalysis.v1.CheckoutStatus;
import de.burger.forensics.analytics.repositoryanalysis.v1.OperationStatus;
import de.burger.forensics.analytics.repositoryanalysis.v1.PrepareRepositoryRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.PrepareRepositoryResponse;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryAnalysisServiceGrpc;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryPreparation;
import de.burger.forensics.analytics.services.gateway.application.GatewayRepositoryAnalysisException;
import de.burger.forensics.analytics.services.gateway.domain.GatewayRepositoryAnalysis.BuildContext;
import de.burger.forensics.analytics.services.gateway.domain.GatewayRepositoryAnalysis.RepositoryPreparationCommand;
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

class RepositoryAnalysisGrpcClientTest {
    @Test
    void mapsGatewaySubmissionToRepositoryAnalysisGrpcRequest() {
        var service = new CapturingRepositoryAnalysisService();
        var fixture = GrpcFixture.start(service);
        try {
            var client = new RepositoryAnalysisGrpcClient(
                RepositoryAnalysisServiceGrpc.newBlockingStub(fixture.channel),
                1
            );

            var result = client.prepare(new RepositoryPreparationCommand("analysis-run-1", request("main", "")));

            assertEquals("analysis-run-1", result.analysisRunId());
            assertEquals("source-snapshot-1", result.sourceSnapshotId());
            assertEquals("request-1", service.request.getRequestId());
            assertEquals("idem-1", service.request.getIdempotencyKey());
            assertEquals("correlation-1", service.request.getCorrelationId());
            assertEquals("https://example.com/acme/demo.git", service.request.getRepository().getRemoteUrl());
            assertEquals("main", service.request.getRevision().getBranch());
            assertEquals("", service.request.getRevision().getCommit());
            assertEquals(60, service.request.getWorkspacePolicy().getTimeoutSeconds());
            assertEquals("DOWNSTREAM_OK", result.diagnostics().getFirst().code());
        } finally {
            fixture.close();
        }
    }

    @Test
    void mapsGrpcFailuresToGatewayErrorsWithoutInternalDescriptions() {
        var fixture = GrpcFixture.start(new FailingRepositoryAnalysisService(Status.UNAVAILABLE));
        try {
            var client = new RepositoryAnalysisGrpcClient(
                RepositoryAnalysisServiceGrpc.newBlockingStub(fixture.channel),
                1
            );

            var failure = assertThrows(GatewayRepositoryAnalysisException.class, () -> client.prepare(
                new RepositoryPreparationCommand("analysis-run-1", request("", "abcdef1"))
            ));

            assertEquals(503, failure.statusCode());
            assertEquals("BACKEND_UNAVAILABLE", failure.errorCode());
            assertEquals("Repository analysis service is unavailable", failure.getMessage());
        } finally {
            fixture.close();
        }
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

        var client = new RepositoryAnalysisGrpcClient("127.0.0.1", 1, 1);
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
        var fixture = GrpcFixture.start(new FailingRepositoryAnalysisService(status));
        try {
            var client = new RepositoryAnalysisGrpcClient(
                RepositoryAnalysisServiceGrpc.newBlockingStub(fixture.channel),
                1
            );

            var failure = assertThrows(GatewayRepositoryAnalysisException.class, () -> client.prepare(
                new RepositoryPreparationCommand("analysis-run-1", request("", "abcdef1"))
            ));

            assertEquals(expectedStatusCode, failure.statusCode());
            assertEquals(expectedErrorCode, failure.errorCode());
            assertEquals(expectedRetryable, failure.retryable());
            assertFalse(failure.getMessage().contains("/tmp"));
        } finally {
            fixture.close();
        }
    }

    private static final class CapturingRepositoryAnalysisService
        extends RepositoryAnalysisServiceGrpc.RepositoryAnalysisServiceImplBase {
        private PrepareRepositoryRequest request;

        @Override
        public void prepareRepository(
            PrepareRepositoryRequest request,
            StreamObserver<PrepareRepositoryResponse> responseObserver
        ) {
            this.request = request;
            responseObserver.onNext(PrepareRepositoryResponse.newBuilder()
                .setPreparation(RepositoryPreparation.newBuilder()
                    .setAnalysisRunId(request.getAnalysisRunId())
                    .setSourceSnapshotId("source-snapshot-1")
                    .setCheckout(CheckoutResult.newBuilder()
                        .setStatus(CheckoutStatus.CHECKOUT_STATUS_CHECKED_OUT)))
                .setStatus(OperationStatus.newBuilder()
                    .setCode("PREPARED")
                    .setMessage("prepared")
                    .setCorrelationId(request.getCorrelationId())
                    .addDiagnostics(de.burger.forensics.analytics.repositoryanalysis.v1.Diagnostic.newBuilder()
                        .setCode("DOWNSTREAM_OK")
                        .setMessage("Repository Analysis accepted the snapshot")))
                .build());
            responseObserver.onCompleted();
        }
    }

    private static final class FailingRepositoryAnalysisService
        extends RepositoryAnalysisServiceGrpc.RepositoryAnalysisServiceImplBase {
        private final Status status;

        private FailingRepositoryAnalysisService(Status status) {
            this.status = status;
        }

        @Override
        public void prepareRepository(
            PrepareRepositoryRequest request,
            StreamObserver<PrepareRepositoryResponse> responseObserver
        ) {
            responseObserver.onError(new StatusRuntimeException(status.withDescription("private /tmp/workspace")));
        }
    }

    private record GrpcFixture(Server server, ManagedChannel channel) implements AutoCloseable {
        private static GrpcFixture start(RepositoryAnalysisServiceGrpc.RepositoryAnalysisServiceImplBase service) {
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
