package de.burger.forensics.analytics.services.gateway.adapter.out.grpc;

import de.burger.forensics.analytics.repositoryanalysis.v1.PrepareRepositoryRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryAnalysisServiceGrpc;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryReference;
import de.burger.forensics.analytics.repositoryanalysis.v1.RevisionSelector;
import de.burger.forensics.analytics.repositoryanalysis.v1.WorkspacePolicy;
import de.burger.forensics.analytics.services.gateway.application.GatewayRepositoryAnalysisException;
import de.burger.forensics.analytics.services.gateway.application.port.RepositoryAnalysisPreparationPort;
import de.burger.forensics.analytics.services.gateway.domain.GatewayRepositoryAnalysis.Diagnostic;
import de.burger.forensics.analytics.services.gateway.domain.GatewayRepositoryAnalysis.RepositoryPreparationCommand;
import de.burger.forensics.analytics.services.gateway.domain.GatewayRepositoryAnalysis.RepositoryPreparationResult;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.util.concurrent.TimeUnit;

public final class RepositoryAnalysisGrpcClient implements RepositoryAnalysisPreparationPort, AutoCloseable {
    private final ManagedChannel channel;
    private final RepositoryAnalysisServiceGrpc.RepositoryAnalysisServiceBlockingStub stub;
    private final long deadlineSeconds;

    public RepositoryAnalysisGrpcClient(String host, int port, long deadlineSeconds) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext().build(), deadlineSeconds);
    }

    RepositoryAnalysisGrpcClient(
        RepositoryAnalysisServiceGrpc.RepositoryAnalysisServiceBlockingStub stub,
        long deadlineSeconds
    ) {
        this.channel = null;
        this.stub = stub;
        this.deadlineSeconds = deadlineSeconds;
    }

    private RepositoryAnalysisGrpcClient(ManagedChannel channel, long deadlineSeconds) {
        this.channel = channel;
        this.stub = RepositoryAnalysisServiceGrpc.newBlockingStub(channel);
        this.deadlineSeconds = deadlineSeconds;
    }

    @Override
    public RepositoryPreparationResult prepare(RepositoryPreparationCommand command) {
        try {
            var response = stub
                .withDeadlineAfter(deadlineSeconds, TimeUnit.SECONDS)
                .prepareRepository(request(command));
            var preparation = response.getPreparation();
            var diagnostics = response.getStatus().getDiagnosticsList().stream()
                .map(diagnostic -> Diagnostic.info(diagnostic.getCode(), diagnostic.getMessage()))
                .toList();
            return new RepositoryPreparationResult(
                preparation.getAnalysisRunId(),
                preparation.getSourceSnapshotId(),
                preparation.getCheckout().getStatus().name(),
                diagnostics
            );
        } catch (StatusRuntimeException error) {
            throw map(error);
        }
    }

    private static PrepareRepositoryRequest request(RepositoryPreparationCommand command) {
        var request = command.request();
        return PrepareRepositoryRequest.newBuilder()
            .setRequestId(request.requestId())
            .setIdempotencyKey(request.idempotencyKey())
            .setSchemaVersion(request.schemaVersion())
            .setCorrelationId(request.correlationId())
            .setAnalysisRunId(command.analysisRunId())
            .setRepository(RepositoryReference.newBuilder()
                .setRemoteUrl(request.repositoryUrl())
                .setProvider(request.provider()))
            .setRevision(RevisionSelector.newBuilder()
                .setBranch(request.branch())
                .setBranchRequired(!request.branch().isBlank())
                .setCommit(request.commit())
                .setCommitRequired(!request.commit().isBlank()))
            .setWorkspacePolicy(WorkspacePolicy.newBuilder()
                .setEphemeral(request.workspacePolicy().ephemeral())
                .setAllowShallowClone(request.workspacePolicy().allowShallowClone())
                .setAllowPartialClone(request.workspacePolicy().allowPartialClone())
                .setAllowSparseCheckout(request.workspacePolicy().allowSparseCheckout())
                .setTimeoutSeconds(request.workspacePolicy().timeoutSeconds())
                .setMaxWorkspaceBytes(request.workspacePolicy().maxWorkspaceBytes()))
            .build();
    }

    private static GatewayRepositoryAnalysisException map(StatusRuntimeException error) {
        var code = error.getStatus().getCode();
        if (code == Status.Code.INVALID_ARGUMENT) {
            return new GatewayRepositoryAnalysisException(400, "VALIDATION_ERROR", false, "Invalid repository analysis request");
        }
        if (code == Status.Code.ALREADY_EXISTS) {
            return new GatewayRepositoryAnalysisException(409, "CONFLICT", false, "Repository analysis request conflicts with a previous idempotency key");
        }
        if (code == Status.Code.NOT_FOUND) {
            return new GatewayRepositoryAnalysisException(404, "NOT_FOUND", false, "Repository analysis resource was not found");
        }
        if (code == Status.Code.DEADLINE_EXCEEDED) {
            return new GatewayRepositoryAnalysisException(504, "TIMEOUT", true, "Repository analysis request timed out");
        }
        if (code == Status.Code.UNAVAILABLE) {
            return new GatewayRepositoryAnalysisException(503, "BACKEND_UNAVAILABLE", true, "Repository analysis service is unavailable");
        }
        if (code == Status.Code.FAILED_PRECONDITION) {
            return new GatewayRepositoryAnalysisException(502, "BACKEND_UNAVAILABLE", false, "Repository analysis service could not prepare the repository");
        }
        return new GatewayRepositoryAnalysisException(500, "UNEXPECTED_ERROR", false, "Repository analysis service failed");
    }

    @Override
    public void close() {
        if (channel != null) {
            channel.shutdownNow();
        }
    }
}
