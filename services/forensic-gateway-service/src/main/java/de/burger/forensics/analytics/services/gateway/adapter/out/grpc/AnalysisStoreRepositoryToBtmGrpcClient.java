package de.burger.forensics.analytics.services.gateway.adapter.out.grpc;

import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobServiceGrpc;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisRunId;
import de.burger.forensics.analytics.analysisjob.v1.BtmDeliveryReadiness;
import de.burger.forensics.analytics.analysisjob.v1.GetRepositoryToBtmStatusRequest;
import de.burger.forensics.analytics.analysisjob.v1.RepositoryToBtmBuildContext;
import de.burger.forensics.analytics.analysisjob.v1.RepositoryToBtmOrchestrationStatus;
import de.burger.forensics.analytics.analysisjob.v1.RepositoryToBtmRepositoryReference;
import de.burger.forensics.analytics.analysisjob.v1.RepositoryToBtmRevision;
import de.burger.forensics.analytics.analysisjob.v1.RepositoryToBtmWorkspacePolicy;
import de.burger.forensics.analytics.analysisjob.v1.RequestedRepositoryToBtmOutput;
import de.burger.forensics.analytics.analysisjob.v1.StartRepositoryToBtmRequest;
import de.burger.forensics.analytics.services.gateway.application.GatewayRepositoryAnalysisException;
import de.burger.forensics.analytics.services.gateway.application.port.RepositoryToBtmOrchestrationPort;
import de.burger.forensics.analytics.services.gateway.domain.GatewayRepositoryAnalysis.Diagnostic;
import de.burger.forensics.analytics.services.gateway.domain.GatewayRepositoryAnalysis.RepositoryToBtmSubmission;
import de.burger.forensics.analytics.services.gateway.domain.GatewayRepositoryAnalysis.StatusRequest;
import de.burger.forensics.analytics.services.gateway.domain.GatewayRepositoryAnalysis.SubmissionRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.util.concurrent.TimeUnit;

public final class AnalysisStoreRepositoryToBtmGrpcClient implements RepositoryToBtmOrchestrationPort, AutoCloseable {
    private final ManagedChannel channel;
    private final AnalysisJobServiceGrpc.AnalysisJobServiceBlockingStub stub;
    private final long deadlineSeconds;

    public AnalysisStoreRepositoryToBtmGrpcClient(String host, int port, long deadlineSeconds) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext().build(), deadlineSeconds);
    }

    AnalysisStoreRepositoryToBtmGrpcClient(
        AnalysisJobServiceGrpc.AnalysisJobServiceBlockingStub stub,
        long deadlineSeconds
    ) {
        this.channel = null;
        this.stub = stub;
        this.deadlineSeconds = deadlineSeconds;
    }

    private AnalysisStoreRepositoryToBtmGrpcClient(ManagedChannel channel, long deadlineSeconds) {
        this.channel = channel;
        this.stub = AnalysisJobServiceGrpc.newBlockingStub(channel);
        this.deadlineSeconds = deadlineSeconds;
    }

    @Override
    public RepositoryToBtmSubmission start(SubmissionRequest request) {
        try {
            return submission(stub
                .withDeadlineAfter(deadlineSeconds, TimeUnit.SECONDS)
                .startRepositoryToBtm(request(request)));
        } catch (StatusRuntimeException error) {
            throw map(error);
        }
    }

    @Override
    public RepositoryToBtmSubmission status(StatusRequest request) {
        try {
            return submission(stub
                .withDeadlineAfter(deadlineSeconds, TimeUnit.SECONDS)
                .getRepositoryToBtmStatus(GetRepositoryToBtmStatusRequest.newBuilder()
                    .setRequestId(request.requestId())
                    .setCorrelationId(request.correlationId())
                    .setAnalysisRunId(AnalysisRunId.newBuilder().setValue(request.analysisRunId()))
                    .build()));
        } catch (StatusRuntimeException error) {
            throw map(error);
        }
    }

    private static StartRepositoryToBtmRequest request(SubmissionRequest request) {
        return StartRepositoryToBtmRequest.newBuilder()
            .setRequestId(request.requestId())
            .setIdempotencyKey(request.idempotencyKey())
            .setSchemaVersion(request.schemaVersion())
            .setCorrelationId(request.correlationId())
            .setAnalysisRunId(AnalysisRunId.newBuilder().setValue(request.analysisRunId()))
            .setRepository(RepositoryToBtmRepositoryReference.newBuilder()
                .setRemoteUrl(request.repositoryUrl())
                .setProvider(request.provider()))
            .setRevision(RepositoryToBtmRevision.newBuilder()
                .setBranch(request.branch())
                .setCommit(request.commit()))
            .setWorkspacePolicy(RepositoryToBtmWorkspacePolicy.newBuilder()
                .setEphemeral(request.workspacePolicy().ephemeral())
                .setAllowShallowClone(request.workspacePolicy().allowShallowClone())
                .setAllowPartialClone(request.workspacePolicy().allowPartialClone())
                .setAllowSparseCheckout(request.workspacePolicy().allowSparseCheckout())
                .setTimeoutSeconds(request.workspacePolicy().timeoutSeconds())
                .setMaxWorkspaceBytes(request.workspacePolicy().maxWorkspaceBytes()))
            .setBuildContext(RepositoryToBtmBuildContext.newBuilder()
                .setBuildTool(request.buildContext().buildTool())
                .setBuildId(request.buildContext().buildId())
                .setRootProjectName(request.buildContext().rootProjectName())
                .addAllDeclaredModules(request.buildContext().declaredModules())
                .putAllAttributes(request.buildContext().attributes()))
            .addRequestedOutputs(RequestedRepositoryToBtmOutput.REQUESTED_REPOSITORY_TO_BTM_OUTPUT_BTM_RULES)
            .build();
    }

    private static RepositoryToBtmSubmission submission(RepositoryToBtmOrchestrationStatus status) {
        var analysisRunId = status.getAnalysisRunId().getValue();
        return new RepositoryToBtmSubmission(
            analysisRunId,
            publicStatus(status),
            "/repository-analyses/" + analysisRunId,
            "/repository-analyses/" + analysisRunId + "/jobs",
            btmDeliveryStatus(status.getBtmDeliveryReadiness()),
            "BtmArtifactDeliveryService",
            status.getStatus().getCorrelationId(),
            status.getDiagnosticsList().stream()
                .map(diagnostic -> new Diagnostic(
                    diagnostic.getSeverity().name().replace("REPOSITORY_TO_BTM_DIAGNOSTIC_SEVERITY_", ""),
                    diagnostic.getCode(),
                    diagnostic.getMessage()
                ))
                .toList()
        );
    }

    private static String btmDeliveryStatus(BtmDeliveryReadiness readiness) {
        return switch (readiness) {
            case BTM_DELIVERY_READINESS_READY -> "BTM_DELIVERY_READY";
            case BTM_DELIVERY_READINESS_UNAVAILABLE -> "BTM_DELIVERY_UNAVAILABLE";
            case BTM_DELIVERY_READINESS_UNKNOWN, BTM_DELIVERY_READINESS_UNSPECIFIED, UNRECOGNIZED -> "BTM_DELIVERY_UNKNOWN";
            case BTM_DELIVERY_READINESS_NOT_READY -> "BTM_DELIVERY_NOT_READY";
        };
    }

    private static String publicStatus(RepositoryToBtmOrchestrationStatus status) {
        return switch (status.getState()) {
            case REPOSITORY_TO_BTM_ORCHESTRATION_STATE_READY_FOR_BTM_DELIVERY -> "COMPLETED";
            case REPOSITORY_TO_BTM_ORCHESTRATION_STATE_FAILED -> "FAILED";
            case REPOSITORY_TO_BTM_ORCHESTRATION_STATE_UNSPECIFIED, UNRECOGNIZED -> "UNKNOWN";
            case REPOSITORY_TO_BTM_ORCHESTRATION_STATE_ACCEPTED,
                 REPOSITORY_TO_BTM_ORCHESTRATION_STATE_WAITING_FOR_REPOSITORY,
                 REPOSITORY_TO_BTM_ORCHESTRATION_STATE_INCOMPLETE -> "ACCEPTED";
        };
    }

    private static GatewayRepositoryAnalysisException map(StatusRuntimeException error) {
        var code = error.getStatus().getCode();
        if (code == Status.Code.INVALID_ARGUMENT) {
            return new GatewayRepositoryAnalysisException(400, "VALIDATION_ERROR", false, "Invalid repository-to-BTM orchestration request");
        }
        if (code == Status.Code.ALREADY_EXISTS) {
            return new GatewayRepositoryAnalysisException(409, "CONFLICT", false, "Repository-to-BTM orchestration request conflicts with a previous idempotency key");
        }
        if (code == Status.Code.NOT_FOUND) {
            return new GatewayRepositoryAnalysisException(404, "NOT_FOUND", false, "Repository-to-BTM orchestration was not found");
        }
        if (code == Status.Code.DEADLINE_EXCEEDED) {
            return new GatewayRepositoryAnalysisException(504, "TIMEOUT", true, "Repository-to-BTM orchestration request timed out");
        }
        if (code == Status.Code.UNAVAILABLE) {
            return new GatewayRepositoryAnalysisException(503, "BACKEND_UNAVAILABLE", true, "Analysis Store service is unavailable");
        }
        if (code == Status.Code.FAILED_PRECONDITION) {
            return new GatewayRepositoryAnalysisException(502, "BACKEND_UNAVAILABLE", false, "Analysis Store could not prepare orchestration state");
        }
        return new GatewayRepositoryAnalysisException(500, "UNEXPECTED_ERROR", false, "Analysis Store orchestration failed");
    }

    @Override
    public void close() {
        if (channel != null) {
            channel.shutdownNow();
        }
    }
}
