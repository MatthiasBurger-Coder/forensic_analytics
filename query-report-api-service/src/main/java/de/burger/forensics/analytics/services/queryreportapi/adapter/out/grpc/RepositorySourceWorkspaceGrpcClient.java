package de.burger.forensics.analytics.services.queryreportapi.adapter.out.grpc;

import de.burger.forensics.analytics.repositoryanalysis.v1.CleanupRepositoryWorkspaceByIdRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.CreateRepositoryWorkspaceRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.GetRepositoryWorkspaceRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.ListRepositoryWorkspacesRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.MetadataPreviewPolicy;
import de.burger.forensics.analytics.repositoryanalysis.v1.PreviewRepositoryWorkspaceMetadataRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.RefreshRepositoryWorkspaceBranchRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryAnalysisServiceGrpc;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryReference;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryWorkspace;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryWorkspaceBranch;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryWorkspaceBranchSelector;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryWorkspaceStatus;
import de.burger.forensics.analytics.services.queryreportapi.application.QueryReportApiIdempotencyConflictException;
import de.burger.forensics.analytics.services.queryreportapi.application.QueryReportApiWorkspaceException;
import de.burger.forensics.analytics.services.queryreportapi.application.port.RepositoryWorkspaceOwnerPort;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.Diagnostic;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.BranchRefreshResponse;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.CleanupWorkspaceRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.GetWorkspaceRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.ListWorkspacesRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.PublicRepositoryIdentity;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.RefreshWorkspaceBranchRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.RepositoryIdentity;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceBranchResponse;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceCleanupResponse;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceListItemResponse;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceListResponse;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceMetadataRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceMetadataResponse;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.util.List;
import java.util.concurrent.TimeUnit;

public final class RepositorySourceWorkspaceGrpcClient implements RepositoryWorkspaceOwnerPort, AutoCloseable {
    private final ManagedChannel channel;
    private final RepositoryAnalysisServiceGrpc.RepositoryAnalysisServiceBlockingStub stub;
    private final long deadlineSeconds;

    public RepositorySourceWorkspaceGrpcClient(String host, int port, long deadlineSeconds) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext().build(), deadlineSeconds);
    }

    RepositorySourceWorkspaceGrpcClient(
        RepositoryAnalysisServiceGrpc.RepositoryAnalysisServiceBlockingStub stub,
        long deadlineSeconds
    ) {
        this.channel = null;
        this.stub = stub;
        this.deadlineSeconds = deadlineSeconds;
    }

    private RepositorySourceWorkspaceGrpcClient(ManagedChannel channel, long deadlineSeconds) {
        this.channel = channel;
        this.stub = RepositoryAnalysisServiceGrpc.newBlockingStub(channel);
        this.deadlineSeconds = deadlineSeconds;
    }

    @Override
    public WorkspaceMetadataResponse previewMetadata(WorkspaceMetadataRequest request) {
        try {
            var response = stub.withDeadlineAfter(deadlineSeconds, TimeUnit.SECONDS)
                .previewRepositoryWorkspaceMetadata(PreviewRepositoryWorkspaceMetadataRequest.newBuilder()
                    .setRequestId(request.requestId())
                    .setSchemaVersion(request.schemaVersion())
                    .setCorrelationId(request.correlationId())
                    .setRepository(repository(request.repositoryUrl()))
                    .setMetadataPolicy(MetadataPreviewPolicy.newBuilder()
                        .setTimeoutSeconds(request.metadataTimeoutSeconds()))
                    .build());
            var repository = response.getRepository();
            return new WorkspaceMetadataResponse(
                repository.getRepositoryKey(),
                repository.getRepositoryHost(),
                nullable(repository.getRepositoryOwner()),
                repository.getRepositoryName(),
                response.getWorkspaceTitle(),
                nullable(repository.getDefaultBranch()),
                response.getRepositoryBranchesList(),
                diagnostics(response.getDiagnosticsList())
            );
        } catch (StatusRuntimeException error) {
            throw map(error);
        }
    }

    @Override
    public WorkspaceResponse create(de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.CreateWorkspaceRequest request) {
        try {
            return workspace(stub.withDeadlineAfter(deadlineSeconds, TimeUnit.SECONDS)
                .createRepositoryWorkspace(CreateRepositoryWorkspaceRequest.newBuilder()
                    .setRequestId(request.requestId())
                    .setIdempotencyKey(request.idempotencyKey())
                    .setSchemaVersion(request.schemaVersion())
                    .setCorrelationId(request.correlationId())
                    .setRepository(repository(request.repositoryUrl()))
                    .setBranchSelector(RepositoryWorkspaceBranchSelector.newBuilder()
                        .setBranch(request.selectedBranch()))
                    .setWorkspacePolicy(workspacePolicy(request.workspacePolicy()))
                    .build())
                .getWorkspace());
        } catch (StatusRuntimeException error) {
            throw map(error);
        }
    }

    @Override
    public WorkspaceResponse get(GetWorkspaceRequest request) {
        try {
            return workspace(stub.withDeadlineAfter(deadlineSeconds, TimeUnit.SECONDS)
                .getRepositoryWorkspace(GetRepositoryWorkspaceRequest.newBuilder()
                    .setRequestId(request.requestId())
                    .setCorrelationId(request.correlationId())
                    .setWorkspaceId(request.workspaceId())
                    .build()));
        } catch (StatusRuntimeException error) {
            throw map(error);
        }
    }

    @Override
    public WorkspaceListResponse list(ListWorkspacesRequest request) {
        try {
            var response = stub.withDeadlineAfter(deadlineSeconds, TimeUnit.SECONDS)
                .listRepositoryWorkspaces(ListRepositoryWorkspacesRequest.newBuilder()
                    .setRequestId(request.requestId())
                    .setSchemaVersion(request.schemaVersion())
                    .setCorrelationId(request.correlationId())
                    .setIncludeCleaned(request.includeCleaned())
                    .build());
            return new WorkspaceListResponse(
                response.getWorkspacesList().stream()
                    .map(RepositorySourceWorkspaceGrpcClient::workspaceListItem)
                    .toList(),
                diagnostics(response.getDiagnosticsList())
            );
        } catch (StatusRuntimeException error) {
            throw map(error);
        } catch (IllegalArgumentException | NullPointerException error) {
            throw unsupportedStatus();
        }
    }

    @Override
    public WorkspaceCleanupResponse cleanup(CleanupWorkspaceRequest request) {
        try {
            var response = stub.withDeadlineAfter(deadlineSeconds, TimeUnit.SECONDS)
                .cleanupRepositoryWorkspaceById(CleanupRepositoryWorkspaceByIdRequest.newBuilder()
                    .setRequestId(request.requestId())
                    .setIdempotencyKey(request.idempotencyKey())
                    .setSchemaVersion(request.schemaVersion())
                    .setCorrelationId(request.correlationId())
                    .setWorkspaceId(request.workspaceId())
                    .build());
            return new WorkspaceCleanupResponse(
                response.getWorkspaceId(),
                workspaceStatus(response.getWorkspaceStatus()),
                diagnostics(response.getDiagnosticsList())
            );
        } catch (StatusRuntimeException error) {
            throw map(error);
        } catch (IllegalArgumentException | NullPointerException error) {
            throw unsupportedStatus();
        }
    }

    @Override
    public BranchRefreshResponse refresh(RefreshWorkspaceBranchRequest request) {
        try {
            var response = stub.withDeadlineAfter(deadlineSeconds, TimeUnit.SECONDS)
                .refreshRepositoryWorkspaceBranch(RefreshRepositoryWorkspaceBranchRequest.newBuilder()
                    .setRequestId(request.requestId())
                    .setIdempotencyKey(request.idempotencyKey())
                    .setSchemaVersion(request.schemaVersion())
                    .setCorrelationId(request.correlationId())
                    .setWorkspaceId(request.workspaceId())
                    .setWorkspaceBranchId(request.workspaceBranchId())
                    .setWorkspacePolicy(workspacePolicy(request.workspacePolicy()))
                    .build());
            var branch = response.getBranch();
            return new BranchRefreshResponse(
                branch.getWorkspaceBranchId(),
                branch.getRepositoryBranch(),
                branchStatus(branch),
                response.getChanged(),
                nullable(response.getPreviousCommit()),
                nullable(branch.getResolvedCommit()),
                nullable(branch.getSourceSnapshotId()),
                diagnostics(response.getDiagnosticsList())
            );
        } catch (StatusRuntimeException error) {
            throw map(error);
        }
    }

    private static RepositoryReference repository(String repositoryUrl) {
        return RepositoryReference.newBuilder()
            .setRemoteUrl(repositoryUrl)
            .build();
    }

    private static de.burger.forensics.analytics.repositoryanalysis.v1.WorkspacePolicy workspacePolicy(
        de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspacePolicy policy
    ) {
        return de.burger.forensics.analytics.repositoryanalysis.v1.WorkspacePolicy.newBuilder()
            .setEphemeral(policy.ephemeral())
            .setAllowShallowClone(policy.allowShallowClone())
            .setAllowPartialClone(policy.allowPartialClone())
            .setAllowSparseCheckout(policy.allowSparseCheckout())
            .setTimeoutSeconds(policy.timeoutSeconds())
            .setMaxWorkspaceBytes(policy.maxWorkspaceBytes())
            .build();
    }

    private static WorkspaceResponse workspace(RepositoryWorkspace workspace) {
        return new WorkspaceResponse(
            workspace.getWorkspaceId(),
            workspace.getWorkspaceTitle(),
            repositoryIdentity(workspace.getRepository()),
            workspace.getBranchesList().stream()
                .map(RepositorySourceWorkspaceGrpcClient::branch)
                .toList(),
            workspaceStatus(workspace),
            diagnostics(workspace.getDiagnosticsList())
        );
    }

    private static WorkspaceListItemResponse workspaceListItem(RepositoryWorkspace workspace) {
        return new WorkspaceListItemResponse(
            workspace.getWorkspaceId(),
            workspace.getWorkspaceTitle(),
            publicRepositoryIdentity(workspace.getRepository()),
            workspace.getBranchesList().stream()
                .map(RepositorySourceWorkspaceGrpcClient::branch)
                .toList(),
            workspaceStatus(workspace),
            diagnostics(workspace.getDiagnosticsList())
        );
    }

    private static RepositoryIdentity repositoryIdentity(
        de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryIdentity repository
    ) {
        return new RepositoryIdentity(
            repository.getRepositoryKey(),
            repository.getRepositoryUrl(),
            repository.getRepositoryHost(),
            nullable(repository.getRepositoryOwner()),
            repository.getRepositoryName(),
            nullable(repository.getDefaultBranch())
        );
    }

    private static PublicRepositoryIdentity publicRepositoryIdentity(
        de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryIdentity repository
    ) {
        return new PublicRepositoryIdentity(
            repository.getRepositoryKey(),
            repository.getRepositoryHost(),
            nullable(repository.getRepositoryOwner()),
            repository.getRepositoryName()
        );
    }

    private static WorkspaceBranchResponse branch(RepositoryWorkspaceBranch branch) {
        return new WorkspaceBranchResponse(
            branch.getWorkspaceBranchId(),
            branch.getRepositoryBranch(),
            branchStatus(branch),
            nullable(branch.getResolvedCommit()),
            nullable(branch.getSourceSnapshotId()),
            branch.getSourceRootsList().stream()
                .map(sourceRoot -> sourceRoot.getRelativePath())
                .toList(),
            diagnostics(branch.getDiagnosticsList())
        );
    }

    private static String workspaceStatus(RepositoryWorkspace workspace) {
        return workspaceStatus(workspace.getStatus());
    }

    private static String workspaceStatus(RepositoryWorkspaceStatus status) {
        return switch (status) {
            case REPOSITORY_WORKSPACE_STATUS_READY -> "READY";
            case REPOSITORY_WORKSPACE_STATUS_CHECKED_OUT -> "CHECKED_OUT";
            case REPOSITORY_WORKSPACE_STATUS_CLEANED -> "CLEANED";
            case REPOSITORY_WORKSPACE_STATUS_FAILED -> "FAILED";
            case REPOSITORY_WORKSPACE_STATUS_UNSPECIFIED, UNRECOGNIZED -> throw unsupportedStatus();
        };
    }

    private static String branchStatus(RepositoryWorkspaceBranch branch) {
        return switch (branch.getStatus()) {
            case REPOSITORY_WORKSPACE_BRANCH_STATUS_CHECKING_OUT -> "CHECKING_OUT";
            case REPOSITORY_WORKSPACE_BRANCH_STATUS_CHECKED_OUT -> "CHECKED_OUT";
            case REPOSITORY_WORKSPACE_BRANCH_STATUS_UP_TO_DATE -> "UP_TO_DATE";
            case REPOSITORY_WORKSPACE_BRANCH_STATUS_UPDATING -> "UPDATING";
            case REPOSITORY_WORKSPACE_BRANCH_STATUS_UPDATED -> "UPDATED";
            case REPOSITORY_WORKSPACE_BRANCH_STATUS_FAILED -> "FAILED";
            case REPOSITORY_WORKSPACE_BRANCH_STATUS_UNSPECIFIED, UNRECOGNIZED -> throw unsupportedStatus();
        };
    }

    private static QueryReportApiWorkspaceException unsupportedStatus() {
        return new QueryReportApiWorkspaceException(
            502,
            "BACKEND_UNAVAILABLE",
            false,
            "Repository Source returned unsupported workspace state"
        );
    }

    private static List<Diagnostic> diagnostics(List<de.burger.forensics.analytics.repositoryanalysis.v1.Diagnostic> diagnostics) {
        return diagnostics.stream()
            .map(diagnostic -> new Diagnostic(
                diagnostic.getSeverity().name().replace("DIAGNOSTIC_SEVERITY_", ""),
                diagnostic.getCode(),
                diagnostic.getMessage()
            ))
            .toList();
    }

    private static RuntimeException map(StatusRuntimeException error) {
        var code = error.getStatus().getCode();
        if (code == Status.Code.INVALID_ARGUMENT) {
            return new QueryReportApiWorkspaceException(400, "VALIDATION_ERROR", false, "Invalid repository workspace request");
        }
        if (code == Status.Code.ALREADY_EXISTS) {
            return new QueryReportApiIdempotencyConflictException("idempotency key was reused with different input");
        }
        if (code == Status.Code.NOT_FOUND) {
            return new QueryReportApiWorkspaceException(404, "NOT_FOUND", false, "Repository workspace was not found");
        }
        if (code == Status.Code.DEADLINE_EXCEEDED) {
            return new QueryReportApiWorkspaceException(504, "TIMEOUT", true, "Repository workspace request timed out");
        }
        if (code == Status.Code.UNAVAILABLE) {
            return new QueryReportApiWorkspaceException(503, "BACKEND_UNAVAILABLE", true, "Repository Source service is unavailable");
        }
        if (code == Status.Code.FAILED_PRECONDITION) {
            return new QueryReportApiWorkspaceException(502, "BACKEND_UNAVAILABLE", false, "Repository Source could not prepare workspace state");
        }
        if (code == Status.Code.INTERNAL) {
            return new QueryReportApiWorkspaceException(502, "BACKEND_UNAVAILABLE", false, "Repository Source could not load workspace state");
        }
        return new QueryReportApiWorkspaceException(500, "UNEXPECTED_ERROR", false, "Repository workspace request failed");
    }

    private static String nullable(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    @Override
    public void close() {
        if (channel != null) {
            channel.shutdownNow();
        }
    }
}
