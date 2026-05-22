package de.burger.forensics.analytics.services.queryreportapi.adapter.out.grpc;

import de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness;
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
import de.burger.forensics.analytics.services.queryreportapi.application.QueryReportApiRepositoryAnalysisException;
import de.burger.forensics.analytics.services.queryreportapi.application.port.RepositoryAnalysisOwnerPort;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.Diagnostic;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.RepositoryToBtmSubmission;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.RepositoryToBtmStatus;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.StatusRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.SubmissionRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class AnalysisOrchestratorRepositoryToBtmGrpcClient implements RepositoryAnalysisOwnerPort, AutoCloseable {
    private final ManagedChannel channel;
    private final AnalysisJobServiceGrpc.AnalysisJobServiceBlockingStub stub;
    private final long deadlineSeconds;

    public AnalysisOrchestratorRepositoryToBtmGrpcClient(String host, int port, long deadlineSeconds) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext().build(), deadlineSeconds);
    }

    AnalysisOrchestratorRepositoryToBtmGrpcClient(
        AnalysisJobServiceGrpc.AnalysisJobServiceBlockingStub stub,
        long deadlineSeconds
    ) {
        this.channel = null;
        this.stub = stub;
        this.deadlineSeconds = deadlineSeconds;
    }

    private AnalysisOrchestratorRepositoryToBtmGrpcClient(ManagedChannel channel, long deadlineSeconds) {
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
    public RepositoryToBtmStatus status(StatusRequest request) {
        try {
            return status(stub
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
            diagnostics(status)
        );
    }

    private static RepositoryToBtmStatus status(RepositoryToBtmOrchestrationStatus status) {
        return new RepositoryToBtmStatus(
            status.getAnalysisRunId().getValue(),
            null,
            null,
            null,
            sourceSnapshotStatus(status),
            publicStatus(status),
            "repository-to-btm",
            null,
            diagnostics(status)
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

    private static String sourceSnapshotStatus(RepositoryToBtmOrchestrationStatus status) {
        return status.getSourceSnapshotId().getValue().isBlank() ? null : "AVAILABLE";
    }

    private static String publicStatus(RepositoryToBtmOrchestrationStatus status) {
        return switch (status.getState()) {
            case REPOSITORY_TO_BTM_ORCHESTRATION_STATE_READY_FOR_BTM_DELIVERY ->
                status.getCompleteness() == AnalysisCompleteness.ANALYSIS_COMPLETENESS_COMPLETE ? "COMPLETED" : "UNKNOWN";
            case REPOSITORY_TO_BTM_ORCHESTRATION_STATE_FAILED -> "FAILED";
            case REPOSITORY_TO_BTM_ORCHESTRATION_STATE_INCOMPLETE,
                 REPOSITORY_TO_BTM_ORCHESTRATION_STATE_UNSPECIFIED,
                 UNRECOGNIZED -> "UNKNOWN";
            case REPOSITORY_TO_BTM_ORCHESTRATION_STATE_ACCEPTED,
                 REPOSITORY_TO_BTM_ORCHESTRATION_STATE_WAITING_FOR_REPOSITORY -> "ACCEPTED";
        };
    }

    private static List<Diagnostic> diagnostics(RepositoryToBtmOrchestrationStatus status) {
        var diagnostics = new ArrayList<>(status.getDiagnosticsList().stream()
            .map(diagnostic -> new Diagnostic(
                diagnostic.getSeverity().name().replace("REPOSITORY_TO_BTM_DIAGNOSTIC_SEVERITY_", ""),
                diagnostic.getCode(),
                diagnostic.getMessage()
            ))
            .toList());
        completenessDiagnostic(status.getCompleteness()).ifPresent(diagnostics::add);
        return List.copyOf(diagnostics);
    }

    private static java.util.Optional<Diagnostic> completenessDiagnostic(AnalysisCompleteness completeness) {
        return switch (completeness) {
            case ANALYSIS_COMPLETENESS_INCOMPLETE -> java.util.Optional.of(new Diagnostic(
                "WARNING",
                "ANALYSIS_COMPLETENESS_INCOMPLETE",
                "Analysis Orchestrator reported incomplete repository analysis state"
            ));
            case ANALYSIS_COMPLETENESS_UNKNOWN, ANALYSIS_COMPLETENESS_UNSPECIFIED, UNRECOGNIZED -> java.util.Optional.of(new Diagnostic(
                "WARNING",
                "ANALYSIS_COMPLETENESS_UNKNOWN",
                "Analysis Orchestrator did not provide complete repository analysis state"
            ));
            case ANALYSIS_COMPLETENESS_COMPLETE -> java.util.Optional.empty();
        };
    }

    private static QueryReportApiRepositoryAnalysisException map(StatusRuntimeException error) {
        var code = error.getStatus().getCode();
        if (code == Status.Code.INVALID_ARGUMENT) {
            return new QueryReportApiRepositoryAnalysisException(400, "VALIDATION_ERROR", false, "Invalid repository-to-BTM orchestration request");
        }
        if (code == Status.Code.ALREADY_EXISTS) {
            return new QueryReportApiRepositoryAnalysisException(409, "CONFLICT", false, "Repository-to-BTM orchestration request conflicts with a previous idempotency key");
        }
        if (code == Status.Code.NOT_FOUND) {
            return new QueryReportApiRepositoryAnalysisException(404, "NOT_FOUND", false, "Repository-to-BTM orchestration was not found");
        }
        if (code == Status.Code.DEADLINE_EXCEEDED) {
            return new QueryReportApiRepositoryAnalysisException(504, "TIMEOUT", true, "Repository-to-BTM orchestration request timed out");
        }
        if (code == Status.Code.UNAVAILABLE) {
            return new QueryReportApiRepositoryAnalysisException(503, "BACKEND_UNAVAILABLE", true, "Analysis Orchestrator service is unavailable");
        }
        if (code == Status.Code.FAILED_PRECONDITION) {
            return new QueryReportApiRepositoryAnalysisException(502, "BACKEND_UNAVAILABLE", false, "Analysis Orchestrator could not prepare orchestration state");
        }
        return new QueryReportApiRepositoryAnalysisException(500, "UNEXPECTED_ERROR", false, "Analysis Orchestrator orchestration failed");
    }

    @Override
    public void close() {
        if (channel != null) {
            channel.shutdownNow();
        }
    }
}
