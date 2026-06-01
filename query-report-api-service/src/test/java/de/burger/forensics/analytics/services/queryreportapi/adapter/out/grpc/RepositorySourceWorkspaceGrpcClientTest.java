package de.burger.forensics.analytics.services.queryreportapi.adapter.out.grpc;

import de.burger.forensics.analytics.repositoryanalysis.v1.CleanupRepositoryWorkspaceByIdRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.CleanupRepositoryWorkspaceByIdResponse;
import de.burger.forensics.analytics.repositoryanalysis.v1.CreateRepositoryWorkspaceRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.CreateRepositoryWorkspaceResponse;
import de.burger.forensics.analytics.repositoryanalysis.v1.Diagnostic;
import de.burger.forensics.analytics.repositoryanalysis.v1.DiagnosticSeverity;
import de.burger.forensics.analytics.repositoryanalysis.v1.GetRepositoryWorkspaceRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.ListRepositoryWorkspacesRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.ListRepositoryWorkspacesResponse;
import de.burger.forensics.analytics.repositoryanalysis.v1.MetadataPreviewPolicy;
import de.burger.forensics.analytics.repositoryanalysis.v1.PreviewRepositoryWorkspaceMetadataRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.PreviewRepositoryWorkspaceMetadataResponse;
import de.burger.forensics.analytics.repositoryanalysis.v1.RefreshRepositoryWorkspaceBranchRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.RefreshRepositoryWorkspaceBranchResponse;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryAnalysisServiceGrpc;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryIdentity;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryWorkspace;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryWorkspaceBranch;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryWorkspaceBranchStatus;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryWorkspaceStatus;
import de.burger.forensics.analytics.services.queryreportapi.application.QueryReportApiIdempotencyConflictException;
import de.burger.forensics.analytics.services.queryreportapi.application.QueryReportApiWorkspaceException;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.CleanupWorkspaceRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.GetWorkspaceRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.ListWorkspacesRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.RefreshWorkspaceBranchRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceMetadataRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspacePolicy;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RepositorySourceWorkspaceGrpcClientTest {
    @Test
    void mapsAllWorkspaceOperationsToRepositorySourceOwnerRequests() throws Exception {
        var service = new RecordingService();
        withClient(service, client -> {
            var metadata = client.previewMetadata(metadataRequest());
            var created = client.create(createRequest());
            var loaded = client.get(getRequest());
            var listed = client.list(listRequest());
            var refreshed = client.refresh(refreshRequest());
            var cleaned = client.cleanup(cleanupRequest());

            assertEquals("demo", metadata.workspaceTitle());
            assertEquals(List.of("main", "release/1.0"), metadata.repositoryBranches());
            assertEquals("workspace-0001", created.workspaceId());
            assertEquals("workspace-0001", loaded.workspaceId());
            assertEquals("workspace-0001", listed.items().get(0).workspaceId());
            assertEquals("example.com", listed.items().get(0).repository().repositoryHost());
            assertEquals("workspace-branch-0001", refreshed.workspaceBranchId());
            assertEquals("CLEANED", cleaned.status());
            assertEquals("request-metadata", service.metadataRequest.getRequestId());
            assertEquals("query-report-workspace.v1", service.metadataRequest.getSchemaVersion());
            assertEquals("correlation-1", service.metadataRequest.getCorrelationId());
            assertEquals("https://example.com/acme/demo.git", service.metadataRequest.getRepository().getRemoteUrl());
            assertEquals(60, service.metadataRequest.getMetadataPolicy().getTimeoutSeconds());
            assertEquals("idem-1", service.createRequest.getIdempotencyKey());
            assertEquals("main", service.createRequest.getBranchSelector().getBranch());
            assertEquals(1_073_741_824L, service.createRequest.getWorkspacePolicy().getMaxWorkspaceBytes());
            assertEquals("workspace-0001", service.getRequest.getWorkspaceId());
            assertEquals("request-list", service.listRequest.getRequestId());
            assertEquals("query-report-workspace.v1", service.listRequest.getSchemaVersion());
            assertEquals("correlation-1", service.listRequest.getCorrelationId());
            assertEquals(false, service.listRequest.getIncludeCleaned());
            assertEquals("idem-refresh", service.refreshRequest.getIdempotencyKey());
            assertEquals("workspace-branch-0001", service.refreshRequest.getWorkspaceBranchId());
            assertEquals("request-cleanup", service.cleanupRequest.getRequestId());
            assertEquals("idem-cleanup", service.cleanupRequest.getIdempotencyKey());
            assertEquals("query-report-workspace.v1", service.cleanupRequest.getSchemaVersion());
            assertEquals("correlation-1", service.cleanupRequest.getCorrelationId());
            assertEquals("workspace-0001", service.cleanupRequest.getWorkspaceId());
        });
    }

    @Test
    void mapsRepositorySourceGrpcErrorsToPublicWorkspaceErrors() throws Exception {
        var cases = List.of(
            new ErrorCase(Status.INVALID_ARGUMENT, QueryReportApiWorkspaceException.class, 400, "VALIDATION_ERROR"),
            new ErrorCase(Status.ALREADY_EXISTS, QueryReportApiIdempotencyConflictException.class, 0, ""),
            new ErrorCase(Status.NOT_FOUND, QueryReportApiWorkspaceException.class, 404, "NOT_FOUND"),
            new ErrorCase(Status.DEADLINE_EXCEEDED, QueryReportApiWorkspaceException.class, 504, "TIMEOUT"),
            new ErrorCase(Status.UNAVAILABLE, QueryReportApiWorkspaceException.class, 503, "BACKEND_UNAVAILABLE"),
            new ErrorCase(Status.FAILED_PRECONDITION, QueryReportApiWorkspaceException.class, 502, "BACKEND_UNAVAILABLE"),
            new ErrorCase(Status.INTERNAL, QueryReportApiWorkspaceException.class, 502, "BACKEND_UNAVAILABLE")
        );

        for (var current : cases) {
            withClient(new FailingService(current.status()), client -> {
                var error = assertThrows(current.type(), () -> client.create(createRequest()));
                if (error instanceof QueryReportApiWorkspaceException workspaceError) {
                    assertEquals(current.statusCode(), workspaceError.statusCode());
                    assertEquals(current.errorCode(), workspaceError.errorCode());
                }
            });
        }
    }

    @Test
    void mapsRepositorySourceListAndCleanupErrorsToPublicWorkspaceErrors() throws Exception {
        withClient(new FailingListAndCleanupService(Status.UNAVAILABLE), client -> {
            var listError = assertThrows(QueryReportApiWorkspaceException.class, () -> client.list(listRequest()));
            var cleanupError = assertThrows(QueryReportApiWorkspaceException.class, () -> client.cleanup(cleanupRequest()));

            assertEquals(503, listError.statusCode());
            assertEquals("BACKEND_UNAVAILABLE", listError.errorCode());
            assertEquals(503, cleanupError.statusCode());
            assertEquals("BACKEND_UNAVAILABLE", cleanupError.errorCode());
        });
    }

    @Test
    void mapsRepositorySourceInternalErrorsToControlledBackendUnavailable() throws Exception {
        withClient(new RepositoryAnalysisServiceGrpc.RepositoryAnalysisServiceImplBase() {
            @Override
            public void createRepositoryWorkspace(
                CreateRepositoryWorkspaceRequest request,
                StreamObserver<CreateRepositoryWorkspaceResponse> responseObserver
            ) {
                responseObserver.onError(Status.INTERNAL.asRuntimeException());
            }
        }, client -> {
            var error = assertThrows(QueryReportApiWorkspaceException.class, () -> client.create(createRequest()));

            assertEquals(502, error.statusCode());
            assertEquals("BACKEND_UNAVAILABLE", error.errorCode());
            assertEquals("Repository Source could not load workspace state", error.getMessage());
        });
    }

    @Test
    void rejectsUnsupportedWorkspaceStatusWithoutFabricatingFailure() throws Exception {
        withClient(new RepositoryAnalysisServiceGrpc.RepositoryAnalysisServiceImplBase() {
            @Override
            public void createRepositoryWorkspace(
                CreateRepositoryWorkspaceRequest request,
                StreamObserver<CreateRepositoryWorkspaceResponse> responseObserver
            ) {
                responseObserver.onNext(CreateRepositoryWorkspaceResponse.newBuilder()
                    .setWorkspace(workspace(RepositoryWorkspaceStatus.REPOSITORY_WORKSPACE_STATUS_UNSPECIFIED))
                    .build());
                responseObserver.onCompleted();
            }
        }, client -> {
            var error = assertThrows(QueryReportApiWorkspaceException.class, () -> client.create(createRequest()));

            assertEquals(502, error.statusCode());
            assertEquals("BACKEND_UNAVAILABLE", error.errorCode());
            assertEquals("Repository Source returned unsupported workspace state", error.getMessage());
        });
    }

    @Test
    void rejectsUnsupportedCleanupStatusWithoutFabricatingDeletion() throws Exception {
        withClient(new RepositoryAnalysisServiceGrpc.RepositoryAnalysisServiceImplBase() {
            @Override
            public void cleanupRepositoryWorkspaceById(
                CleanupRepositoryWorkspaceByIdRequest request,
                StreamObserver<CleanupRepositoryWorkspaceByIdResponse> responseObserver
            ) {
                responseObserver.onNext(CleanupRepositoryWorkspaceByIdResponse.newBuilder()
                    .setWorkspaceId("workspace-0001")
                    .setWorkspaceStatus(RepositoryWorkspaceStatus.REPOSITORY_WORKSPACE_STATUS_READY)
                    .build());
                responseObserver.onCompleted();
            }
        }, client -> {
            var error = assertThrows(QueryReportApiWorkspaceException.class, () -> client.cleanup(cleanupRequest()));

            assertEquals(502, error.statusCode());
            assertEquals("BACKEND_UNAVAILABLE", error.errorCode());
            assertEquals("Repository Source returned unsupported workspace state", error.getMessage());
        });
    }

    @Test
    void rejectsUnsupportedBranchStatusWithoutFabricatingFailure() throws Exception {
        withClient(new RepositoryAnalysisServiceGrpc.RepositoryAnalysisServiceImplBase() {
            @Override
            public void createRepositoryWorkspace(
                CreateRepositoryWorkspaceRequest request,
                StreamObserver<CreateRepositoryWorkspaceResponse> responseObserver
            ) {
                responseObserver.onNext(CreateRepositoryWorkspaceResponse.newBuilder()
                    .setWorkspace(workspace(RepositoryWorkspaceStatus.REPOSITORY_WORKSPACE_STATUS_CHECKED_OUT)
                        .toBuilder()
                        .addBranches(branch(RepositoryWorkspaceBranchStatus.REPOSITORY_WORKSPACE_BRANCH_STATUS_UNSPECIFIED)))
                    .build());
                responseObserver.onCompleted();
            }
        }, client -> {
            var error = assertThrows(QueryReportApiWorkspaceException.class, () -> client.create(createRequest()));

            assertEquals(502, error.statusCode());
            assertEquals("BACKEND_UNAVAILABLE", error.errorCode());
            assertEquals("Repository Source returned unsupported workspace state", error.getMessage());
        });
    }

    private static void withClient(
        RepositoryAnalysisServiceGrpc.RepositoryAnalysisServiceImplBase service,
        ClientAssertion assertion
    ) throws Exception {
        var serverName = InProcessServerBuilder.generateName();
        var server = InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(service)
            .build()
            .start();
        var channel = InProcessChannelBuilder.forName(serverName)
            .directExecutor()
            .build();
        try {
            assertion.verify(new RepositorySourceWorkspaceGrpcClient(
                RepositoryAnalysisServiceGrpc.newBlockingStub(channel),
                10
            ));
        } finally {
            channel.shutdownNow();
            server.shutdownNow();
        }
    }

    private static de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.CreateWorkspaceRequest createRequest() {
        return new de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.CreateWorkspaceRequest(
            "request-1",
            "idem-1",
            "query-report-workspace.v1",
            "correlation-1",
            "https://example.com/acme/demo.git",
            "main",
            new WorkspacePolicy(false, true, false, false, 60, 1_073_741_824L)
        );
    }

    private static WorkspaceMetadataRequest metadataRequest() {
        return new WorkspaceMetadataRequest(
            "request-metadata",
            "idem-metadata",
            "query-report-workspace.v1",
            "correlation-1",
            "https://example.com/acme/demo.git",
            60
        );
    }

    private static GetWorkspaceRequest getRequest() {
        return new GetWorkspaceRequest(
            "request-get",
            "correlation-1",
            "workspace-0001"
        );
    }

    private static ListWorkspacesRequest listRequest() {
        return new ListWorkspacesRequest(
            "request-list",
            "query-report-workspace.v1",
            "correlation-1",
            false
        );
    }

    private static CleanupWorkspaceRequest cleanupRequest() {
        return new CleanupWorkspaceRequest(
            "request-cleanup",
            "idem-cleanup",
            "query-report-workspace.v1",
            "correlation-1",
            "workspace-0001"
        );
    }

    private static RefreshWorkspaceBranchRequest refreshRequest() {
        return new RefreshWorkspaceBranchRequest(
            "request-refresh",
            "idem-refresh",
            "query-report-workspace.v1",
            "correlation-1",
            "workspace-0001",
            "workspace-branch-0001",
            new WorkspacePolicy(false, true, false, false, 60, 1_073_741_824L)
        );
    }

    private static RepositoryWorkspace workspace(RepositoryWorkspaceStatus status) {
        return RepositoryWorkspace.newBuilder()
            .setWorkspaceId("workspace-0001")
            .setWorkspaceTitle("demo")
            .setRepository(RepositoryIdentity.newBuilder()
                .setRepositoryKey("example.com/acme/demo")
                .setRepositoryUrl("https://example.com/acme/demo.git")
                .setRepositoryHost("example.com")
                .setRepositoryOwner("acme")
                .setRepositoryName("demo")
                .setDefaultBranch("main"))
            .setStatus(status)
            .build();
    }

    private static RepositoryWorkspaceBranch branch(RepositoryWorkspaceBranchStatus status) {
        return RepositoryWorkspaceBranch.newBuilder()
            .setWorkspaceBranchId("workspace-branch-0001")
            .setRepositoryBranch("main")
            .setResolvedCommit("abcdef1")
            .setSourceSnapshotId("source-snapshot-0001")
            .setStatus(status)
            .build();
    }

    private static final class RecordingService extends RepositoryAnalysisServiceGrpc.RepositoryAnalysisServiceImplBase {
        private PreviewRepositoryWorkspaceMetadataRequest metadataRequest;
        private CreateRepositoryWorkspaceRequest createRequest;
        private GetRepositoryWorkspaceRequest getRequest;
        private ListRepositoryWorkspacesRequest listRequest;
        private RefreshRepositoryWorkspaceBranchRequest refreshRequest;
        private CleanupRepositoryWorkspaceByIdRequest cleanupRequest;

        @Override
        public void previewRepositoryWorkspaceMetadata(
            PreviewRepositoryWorkspaceMetadataRequest request,
            StreamObserver<PreviewRepositoryWorkspaceMetadataResponse> responseObserver
        ) {
            metadataRequest = request;
            responseObserver.onNext(PreviewRepositoryWorkspaceMetadataResponse.newBuilder()
                .setRepository(repository())
                .setWorkspaceTitle("demo")
                .addAllRepositoryBranches(List.of("main", "release/1.0"))
                .build());
            responseObserver.onCompleted();
        }

        @Override
        public void createRepositoryWorkspace(
            CreateRepositoryWorkspaceRequest request,
            StreamObserver<CreateRepositoryWorkspaceResponse> responseObserver
        ) {
            createRequest = request;
            responseObserver.onNext(CreateRepositoryWorkspaceResponse.newBuilder()
                .setWorkspace(workspace(RepositoryWorkspaceStatus.REPOSITORY_WORKSPACE_STATUS_CHECKED_OUT)
                    .toBuilder()
                    .addBranches(branch(RepositoryWorkspaceBranchStatus.REPOSITORY_WORKSPACE_BRANCH_STATUS_CHECKED_OUT)))
                .build());
            responseObserver.onCompleted();
        }

        @Override
        public void getRepositoryWorkspace(
            GetRepositoryWorkspaceRequest request,
            StreamObserver<RepositoryWorkspace> responseObserver
        ) {
            getRequest = request;
            responseObserver.onNext(workspace(RepositoryWorkspaceStatus.REPOSITORY_WORKSPACE_STATUS_CHECKED_OUT)
                .toBuilder()
                .addBranches(branch(RepositoryWorkspaceBranchStatus.REPOSITORY_WORKSPACE_BRANCH_STATUS_CHECKED_OUT))
                .build());
            responseObserver.onCompleted();
        }

        @Override
        public void listRepositoryWorkspaces(
            ListRepositoryWorkspacesRequest request,
            StreamObserver<ListRepositoryWorkspacesResponse> responseObserver
        ) {
            listRequest = request;
            responseObserver.onNext(ListRepositoryWorkspacesResponse.newBuilder()
                .addWorkspaces(workspace(RepositoryWorkspaceStatus.REPOSITORY_WORKSPACE_STATUS_CHECKED_OUT)
                    .toBuilder()
                    .addBranches(branch(RepositoryWorkspaceBranchStatus.REPOSITORY_WORKSPACE_BRANCH_STATUS_CHECKED_OUT))
                    .build())
                .addDiagnostics(Diagnostic.newBuilder()
                    .setSeverity(DiagnosticSeverity.DIAGNOSTIC_SEVERITY_INFO)
                    .setCode("WORKSPACES_LISTED")
                    .setMessage("Repository workspaces listed"))
                .build());
            responseObserver.onCompleted();
        }

        @Override
        public void refreshRepositoryWorkspaceBranch(
            RefreshRepositoryWorkspaceBranchRequest request,
            StreamObserver<RefreshRepositoryWorkspaceBranchResponse> responseObserver
        ) {
            refreshRequest = request;
            responseObserver.onNext(RefreshRepositoryWorkspaceBranchResponse.newBuilder()
                .setBranch(branch(RepositoryWorkspaceBranchStatus.REPOSITORY_WORKSPACE_BRANCH_STATUS_UP_TO_DATE))
                .setChanged(false)
                .build());
            responseObserver.onCompleted();
        }

        @Override
        public void cleanupRepositoryWorkspaceById(
            CleanupRepositoryWorkspaceByIdRequest request,
            StreamObserver<CleanupRepositoryWorkspaceByIdResponse> responseObserver
        ) {
            cleanupRequest = request;
            responseObserver.onNext(CleanupRepositoryWorkspaceByIdResponse.newBuilder()
                .setWorkspaceId("workspace-0001")
                .setWorkspaceStatus(RepositoryWorkspaceStatus.REPOSITORY_WORKSPACE_STATUS_CLEANED)
                .addDiagnostics(Diagnostic.newBuilder()
                    .setSeverity(DiagnosticSeverity.DIAGNOSTIC_SEVERITY_INFO)
                    .setCode("WORKSPACE_CLEANED")
                    .setMessage("Repository workspace cleaned"))
                .build());
            responseObserver.onCompleted();
        }
    }

    private static final class FailingService extends RepositoryAnalysisServiceGrpc.RepositoryAnalysisServiceImplBase {
        private final Status status;

        private FailingService(Status status) {
            this.status = status;
        }

        @Override
        public void createRepositoryWorkspace(
            CreateRepositoryWorkspaceRequest request,
            StreamObserver<CreateRepositoryWorkspaceResponse> responseObserver
        ) {
            responseObserver.onError(status.asRuntimeException());
        }
    }

    private static final class FailingListAndCleanupService extends RepositoryAnalysisServiceGrpc.RepositoryAnalysisServiceImplBase {
        private final Status status;

        private FailingListAndCleanupService(Status status) {
            this.status = status;
        }

        @Override
        public void listRepositoryWorkspaces(
            ListRepositoryWorkspacesRequest request,
            StreamObserver<ListRepositoryWorkspacesResponse> responseObserver
        ) {
            responseObserver.onError(status.asRuntimeException());
        }

        @Override
        public void cleanupRepositoryWorkspaceById(
            CleanupRepositoryWorkspaceByIdRequest request,
            StreamObserver<CleanupRepositoryWorkspaceByIdResponse> responseObserver
        ) {
            responseObserver.onError(status.asRuntimeException());
        }
    }

    private static RepositoryIdentity repository() {
        return RepositoryIdentity.newBuilder()
            .setRepositoryKey("example.com/acme/demo")
            .setRepositoryUrl("https://example.com/acme/demo.git")
            .setRepositoryHost("example.com")
            .setRepositoryOwner("acme")
            .setRepositoryName("demo")
            .setDefaultBranch("main")
            .build();
    }

    private record ErrorCase(
        Status status,
        Class<? extends RuntimeException> type,
        int statusCode,
        String errorCode
    ) {
    }

    @FunctionalInterface
    private interface ClientAssertion {
        void verify(RepositorySourceWorkspaceGrpcClient client) throws IOException;
    }
}
