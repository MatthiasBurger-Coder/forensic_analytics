package de.burger.forensics.analytics.services.repositoryanalysis.adapter.in.grpc;

import de.burger.forensics.analytics.repositoryanalysis.v1.ArtifactReference;
import de.burger.forensics.analytics.repositoryanalysis.v1.CheckoutResult;
import de.burger.forensics.analytics.repositoryanalysis.v1.CheckoutStatus;
import de.burger.forensics.analytics.repositoryanalysis.v1.CleanupRepositoryWorkspaceRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.CleanupRepositoryWorkspaceResponse;
import de.burger.forensics.analytics.repositoryanalysis.v1.Diagnostic;
import de.burger.forensics.analytics.repositoryanalysis.v1.DiagnosticSeverity;
import de.burger.forensics.analytics.repositoryanalysis.v1.GetRepositoryPreparationRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.OperationStatus;
import de.burger.forensics.analytics.repositoryanalysis.v1.PrepareRepositoryRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.PrepareRepositoryResponse;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryAnalysisServiceGrpc;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryPreparation;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryReference;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryWorkspaceStatus;
import de.burger.forensics.analytics.repositoryanalysis.v1.RevisionSelector;
import de.burger.forensics.analytics.repositoryanalysis.v1.SourceRoot;
import de.burger.forensics.analytics.repositoryanalysis.v1.SourceSnapshot;
import de.burger.forensics.analytics.repositoryanalysis.v1.SourceSnapshotCompleteness;
import de.burger.forensics.analytics.repositoryanalysis.v1.WorkspacePolicy;
import de.burger.forensics.analytics.services.repositoryanalysis.application.IdempotencyConflictException;
import de.burger.forensics.analytics.services.repositoryanalysis.application.RepositoryAnalysisApplicationService;
import de.burger.forensics.analytics.services.repositoryanalysis.application.RepositoryPreparationNotFoundException;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceSnapshotId;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.WorkspaceId;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.Objects;

import static de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.requireText;

public final class RepositoryAnalysisGrpcEndpoint extends RepositoryAnalysisServiceGrpc.RepositoryAnalysisServiceImplBase {
    private final RepositoryAnalysisApplicationService applicationService;

    public RepositoryAnalysisGrpcEndpoint(RepositoryAnalysisApplicationService applicationService) {
        this.applicationService = Objects.requireNonNull(applicationService, "application service must not be null");
    }

    @Override
    public void prepareRepository(
        PrepareRepositoryRequest request,
        StreamObserver<PrepareRepositoryResponse> responseObserver
    ) {
        try {
            requireText(request.getRequestId(), "request id");
            var preparation = applicationService.prepare(
                request.getIdempotencyKey(),
                request.getSchemaVersion(),
                request.getCorrelationId(),
                new AnalysisRunId(request.getAnalysisRunId()),
                repository(request.getRepository()),
                revision(request.getRevision()),
                workspacePolicy(request.getWorkspacePolicy()),
                request.getSafeAttributesMap()
            );
            responseObserver.onNext(PrepareRepositoryResponse.newBuilder()
                .setPreparation(preparation(preparation))
                .setStatus(status("PREPARED", "Repository preparation completed", request.getCorrelationId()))
                .build());
            responseObserver.onCompleted();
        } catch (RuntimeException error) {
            responseObserver.onError(status(error).asRuntimeException());
        }
    }

    @Override
    public void getRepositoryPreparation(
        GetRepositoryPreparationRequest request,
        StreamObserver<RepositoryPreparation> responseObserver
    ) {
        try {
            requireText(request.getRequestId(), "request id");
            requireText(request.getCorrelationId(), "correlation id");
            var preparation = applicationService.get(
                new AnalysisRunId(request.getAnalysisRunId()),
                new SourceSnapshotId(request.getSourceSnapshotId())
            );
            responseObserver.onNext(preparation(preparation));
            responseObserver.onCompleted();
        } catch (RuntimeException error) {
            responseObserver.onError(status(error).asRuntimeException());
        }
    }

    @Override
    public void cleanupRepositoryWorkspace(
        CleanupRepositoryWorkspaceRequest request,
        StreamObserver<CleanupRepositoryWorkspaceResponse> responseObserver
    ) {
        try {
            requireText(request.getRequestId(), "request id");
            var result = applicationService.cleanup(
                request.getIdempotencyKey(),
                request.getCorrelationId(),
                new AnalysisRunId(request.getAnalysisRunId()),
                new WorkspaceId(request.getWorkspaceId())
            );
            var builder = CleanupRepositoryWorkspaceResponse.newBuilder()
                .setWorkspaceId(result.workspaceId().value())
                .setWorkspaceStatus(workspaceStatus(result.workspaceStatus()))
                .setStatus(status("CLEANED", "Repository workspace cleaned", request.getCorrelationId()));
            result.diagnostics().forEach(diagnostic -> builder.addDiagnostics(diagnostic(diagnostic)));
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (RuntimeException error) {
            responseObserver.onError(status(error).asRuntimeException());
        }
    }

    private static de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.RepositoryReference repository(
        RepositoryReference repository
    ) {
        return new de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.RepositoryReference(
            repository.getRemoteUrl(),
            repository.getProvider(),
            repository.getSafeAttributesMap()
        );
    }

    private static de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.RevisionSelector revision(
        RevisionSelector revision
    ) {
        return new de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.RevisionSelector(
            revision.getBranch(),
            revision.getBranchRequired(),
            revision.getCommit(),
            revision.getCommitRequired()
        );
    }

    private static de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.WorkspacePolicy workspacePolicy(
        WorkspacePolicy policy
    ) {
        return new de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.WorkspacePolicy(
            policy.getEphemeral(),
            policy.getAllowShallowClone(),
            policy.getAllowPartialClone(),
            policy.getAllowSparseCheckout(),
            policy.getTimeoutSeconds(),
            policy.getMaxWorkspaceBytes()
        );
    }

    private static RepositoryPreparation preparation(
        de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.RepositoryPreparation preparation
    ) {
        var builder = RepositoryPreparation.newBuilder()
            .setAnalysisRunId(preparation.analysisRunId().value())
            .setSourceSnapshotId(preparation.sourceSnapshotId().value())
            .setWorkspaceId(preparation.workspaceId().value())
            .setRepository(repository(preparation.repository()))
            .setRequestedRevision(revision(preparation.requestedRevision()))
            .setCheckout(checkout(preparation.checkout()))
            .setSourceSnapshot(sourceSnapshot(preparation.sourceSnapshot()))
            .setWorkspaceStatus(workspaceStatus(preparation.workspaceStatus()))
            .setCreatedAt(preparation.createdAt().toString())
            .setUpdatedAt(preparation.updatedAt().toString())
            .putAllSafeAttributes(preparation.safeAttributes());
        preparation.diagnostics().forEach(diagnostic -> builder.addDiagnostics(diagnostic(diagnostic)));
        return builder.build();
    }

    private static RepositoryReference repository(
        de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.RepositoryReference repository
    ) {
        return RepositoryReference.newBuilder()
            .setRemoteUrl(repository.remoteUrl())
            .setProvider(repository.provider())
            .putAllSafeAttributes(repository.safeAttributes())
            .build();
    }

    private static RevisionSelector revision(
        de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.RevisionSelector revision
    ) {
        return RevisionSelector.newBuilder()
            .setBranch(revision.branch())
            .setBranchRequired(revision.branchRequired())
            .setCommit(revision.commit())
            .setCommitRequired(revision.commitRequired())
            .build();
    }

    private static CheckoutResult checkout(
        de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.CheckoutResult checkout
    ) {
        var builder = CheckoutResult.newBuilder()
            .setStatus(checkoutStatus(checkout.status()))
            .setResolvedRemoteUrl(checkout.resolvedRemoteUrl())
            .setResolvedCommit(checkout.resolvedCommit())
            .setRequestedBranch(checkout.requestedBranch())
            .setRequestedCommit(checkout.requestedCommit())
            .setShallowClone(checkout.shallowClone())
            .setElapsedMillis(checkout.elapsedMillis())
            .setPartialClone(checkout.partialClone())
            .setSparseCheckout(checkout.sparseCheckout());
        checkout.diagnostics().forEach(diagnostic -> builder.addDiagnostics(diagnostic(diagnostic)));
        return builder.build();
    }

    private static SourceSnapshot sourceSnapshot(
        de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceSnapshot sourceSnapshot
    ) {
        var builder = SourceSnapshot.newBuilder()
            .setSourceSnapshotId(sourceSnapshot.sourceSnapshotId().value())
            .setCompleteness(completeness(sourceSnapshot.completeness()))
            .setManifestArtifact(artifact(sourceSnapshot.manifestArtifact()))
            .addAllLimitations(sourceSnapshot.limitations());
        sourceSnapshot.sourceRoots().forEach(sourceRoot -> builder.addSourceRoots(sourceRoot(sourceRoot)));
        return builder.build();
    }

    private static ArtifactReference artifact(
        de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.ArtifactReference artifact
    ) {
        return ArtifactReference.newBuilder()
            .setReference(artifact.reference())
            .setType(artifact.type())
            .setSha256(artifact.sha256())
            .setSizeBytes(artifact.sizeBytes())
            .build();
    }

    private static SourceRoot sourceRoot(
        de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceRoot sourceRoot
    ) {
        return SourceRoot.newBuilder()
            .setRelativePath(sourceRoot.relativePath())
            .setLanguage(sourceRoot.language())
            .build();
    }

    private static Diagnostic diagnostic(
        de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.Diagnostic diagnostic
    ) {
        return Diagnostic.newBuilder()
            .setCode(diagnostic.code())
            .setMessage(diagnostic.message())
            .setSeverity(severity(diagnostic.severity()))
            .build();
    }

    private static OperationStatus status(String code, String message, String correlationId) {
        return OperationStatus.newBuilder()
            .setCode(code)
            .setMessage(message)
            .setRetryable(false)
            .setCorrelationId(correlationId)
            .build();
    }

    static Status status(RuntimeException error) {
        if (error instanceof RepositoryPreparationNotFoundException) {
            return Status.NOT_FOUND.withDescription(error.getMessage());
        }
        if (error instanceof IdempotencyConflictException) {
            return Status.ALREADY_EXISTS.withDescription(error.getMessage());
        }
        if (error instanceof IllegalArgumentException) {
            return Status.INVALID_ARGUMENT.withDescription("Invalid repository analysis request");
        }
        if (error instanceof IllegalStateException) {
            return Status.FAILED_PRECONDITION.withDescription("Repository preparation failed");
        }
        return Status.INTERNAL.withDescription("Repository analysis service failed");
    }

    static CheckoutStatus checkoutStatus(
        de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.CheckoutStatus status
    ) {
        return switch (status) {
            case ACCEPTED -> CheckoutStatus.CHECKOUT_STATUS_ACCEPTED;
            case WORKSPACE_PREPARED -> CheckoutStatus.CHECKOUT_STATUS_WORKSPACE_PREPARED;
            case CHECKED_OUT -> CheckoutStatus.CHECKOUT_STATUS_CHECKED_OUT;
            case FAILED -> CheckoutStatus.CHECKOUT_STATUS_FAILED;
        };
    }

    static RepositoryWorkspaceStatus workspaceStatus(
        de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.RepositoryWorkspaceStatus status
    ) {
        return switch (status) {
            case READY -> RepositoryWorkspaceStatus.REPOSITORY_WORKSPACE_STATUS_READY;
            case CHECKED_OUT -> RepositoryWorkspaceStatus.REPOSITORY_WORKSPACE_STATUS_CHECKED_OUT;
            case CLEANED -> RepositoryWorkspaceStatus.REPOSITORY_WORKSPACE_STATUS_CLEANED;
            case FAILED -> RepositoryWorkspaceStatus.REPOSITORY_WORKSPACE_STATUS_FAILED;
        };
    }

    static SourceSnapshotCompleteness completeness(
        de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceSnapshotCompleteness completeness
    ) {
        return switch (completeness) {
            case COMPLETE -> SourceSnapshotCompleteness.SOURCE_SNAPSHOT_COMPLETENESS_COMPLETE;
            case INCOMPLETE -> SourceSnapshotCompleteness.SOURCE_SNAPSHOT_COMPLETENESS_INCOMPLETE;
            case UNKNOWN -> SourceSnapshotCompleteness.SOURCE_SNAPSHOT_COMPLETENESS_UNKNOWN;
        };
    }

    static DiagnosticSeverity severity(
        de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.DiagnosticSeverity severity
    ) {
        return switch (severity) {
            case INFO -> DiagnosticSeverity.DIAGNOSTIC_SEVERITY_INFO;
            case WARNING -> DiagnosticSeverity.DIAGNOSTIC_SEVERITY_WARNING;
            case ERROR -> DiagnosticSeverity.DIAGNOSTIC_SEVERITY_ERROR;
        };
    }
}
