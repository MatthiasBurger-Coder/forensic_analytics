package de.burger.forensics.analytics.services.repositorysource.adapter.in.grpc;

import de.burger.forensics.analytics.repositoryanalysis.v1.AnalyzeSourceSnapshotWithJavaAstRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.CleanupRepositoryWorkspaceByIdRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.CleanupRepositoryWorkspaceRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.CreateRepositoryWorkspaceRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.GetRepositoryPreparationRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.GetRepositoryWorkspaceRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputProducer;
import de.burger.forensics.analytics.repositoryanalysis.v1.ListRepositoryWorkspacesRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.MetadataPreviewPolicy;
import de.burger.forensics.analytics.repositoryanalysis.v1.PackageAvailability;
import de.burger.forensics.analytics.repositoryanalysis.v1.PrepareRepositoryRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.PreviewRepositoryWorkspaceMetadataRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.RefreshRepositoryWorkspaceBranchRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryAnalysisServiceGrpc;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryReference;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryWorkspaceBranchSelector;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryWorkspaceBranchStatus;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryWorkspaceStatus;
import de.burger.forensics.analytics.repositoryanalysis.v1.RevisionSelector;
import de.burger.forensics.analytics.repositoryanalysis.v1.WorkspacePolicy;
import de.burger.forensics.analytics.services.repositorysource.adapter.out.memory.InMemoryRepositoryPreparationRepository;
import de.burger.forensics.analytics.services.repositorysource.adapter.out.memory.InMemoryRepositorySourceIdempotencyRepository;
import de.burger.forensics.analytics.services.repositorysource.adapter.out.memory.InMemoryRepositoryWorkspaceRepository;
import de.burger.forensics.analytics.services.repositorysource.application.RepositorySourceApplicationService;
import de.burger.forensics.analytics.services.repositorysource.application.RepositoryWorkspaceApplicationService;
import de.burger.forensics.analytics.services.repositorysource.application.port.PreparedWorkspace;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryCheckoutPort;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryMetadataPort;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryMetadataResolution;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryWorkspaceIdGenerator;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryWorkspacePort;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.ArtifactByteCustody;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.CheckoutResult;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.CheckoutStatus;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.Diagnostic;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryIdentity;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspace;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceBranch;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.SourceRoot;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceBranchId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceTitle;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RepositorySourceGrpcEndpointTest {
    private Server server;
    private ManagedChannel channel;
    private RepositoryAnalysisServiceGrpc.RepositoryAnalysisServiceBlockingStub stub;
    private FakeWorkspacePort workspacePort;
    private SequencedCheckoutPort checkoutPort;
    private FakeMetadataPort metadataPort;
    private FixedRepositoryWorkspaceIdGenerator idGenerator;
    private InMemoryRepositoryWorkspaceRepository workspaceRepository;

    @BeforeEach
    void startServer() throws IOException {
        startServerWithPorts(new FakeWorkspacePort(), new SequencedCheckoutPort("b".repeat(40), "b".repeat(40), "c".repeat(40)));
    }

    private void startServerWithPorts(
        FakeWorkspacePort workspacePort,
        SequencedCheckoutPort checkoutPort
    ) throws IOException {
        var serverName = InProcessServerBuilder.generateName();
        var repository = new InMemoryRepositoryPreparationRepository();
        var workspaceRepository = new InMemoryRepositoryWorkspaceRepository();
        this.workspaceRepository = workspaceRepository;
        this.workspacePort = workspacePort;
        this.checkoutPort = checkoutPort;
        this.metadataPort = new FakeMetadataPort("main", true);
        this.idGenerator = new FixedRepositoryWorkspaceIdGenerator();
        var idempotencyRepository = new InMemoryRepositorySourceIdempotencyRepository();
        var applicationService = new RepositorySourceApplicationService(
            repository,
            idempotencyRepository,
            workspacePort,
            checkoutPort,
            Clock.fixed(Instant.parse("2026-05-16T10:15:30Z"), ZoneOffset.UTC)
        );
        var workspaceApplicationService = new RepositoryWorkspaceApplicationService(
            workspaceRepository,
            idGenerator,
            idempotencyRepository,
            workspacePort,
            checkoutPort,
            metadataPort,
            Clock.fixed(Instant.parse("2026-05-16T10:15:30Z"), ZoneOffset.UTC)
        );
        server = InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(new RepositorySourceGrpcEndpoint(applicationService, workspaceApplicationService))
            .build()
            .start();
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        stub = RepositoryAnalysisServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void stopServer() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void preparesGetsAndCleansRepositoryPreparation() {
        var prepared = stub.prepareRepository(prepareRequest("prepare-1", "schema-v1"));
        var loaded = stub.getRepositoryPreparation(GetRepositoryPreparationRequest.newBuilder()
            .setRequestId("request-get")
            .setCorrelationId("correlation-1")
            .setAnalysisRunId("run-1")
            .setSourceSnapshotId(prepared.getPreparation().getSourceSnapshotId())
            .build());
        var cleaned = stub.cleanupRepositoryWorkspace(CleanupRepositoryWorkspaceRequest.newBuilder()
            .setRequestId("request-cleanup")
            .setIdempotencyKey("cleanup-1")
            .setCorrelationId("correlation-1")
            .setAnalysisRunId("run-1")
            .setWorkspaceId(prepared.getPreparation().getWorkspaceId())
            .build());

        assertEquals("PREPARED", prepared.getStatus().getCode());
        assertEquals("https://example.com/acme/demo.git", loaded.getRepository().getRemoteUrl());
        assertFalse(loaded.getWorkspaceId().contains("memory"));
        assertFalse(loaded.getWorkspaceId().contains("/"));
        assertFalse(loaded.getWorkspaceId().contains("\\"));
        assertEquals("src/main/java", loaded.getSourceSnapshot().getSourceRoots(0).getRelativePath());
        assertEquals(PackageAvailability.PACKAGE_AVAILABILITY_PENDING, loaded.getSourceSnapshot().getSourcePackage().getAvailability());
        assertEquals("repository-source-service", loaded.getSourceSnapshot().getSourcePackage().getByteAccess().getOwnerService());
        assertEquals("repository-source.v1.SourcePackage", loaded.getSourceSnapshot().getSourcePackage().getByteAccess().getRetrievalContract());
        assertEquals(
            "source-snapshot/" + loaded.getSourceSnapshotId(),
            loaded.getSourceSnapshot().getSourcePackage().getByteAccess().getRetrievalReference()
        );
        assertEquals("build-artifact-worker-service", loaded.getSourceSnapshot().getBuildOutputPackage().getByteAccess().getOwnerService());
        assertEquals("auto-detect", loaded.getSourceSnapshot().getBuildOutputPackage().getBuildSystem());
        assertEquals(
            List.of(
                BuildOutputProducer.BUILD_OUTPUT_PRODUCER_ARTIFACT_STORE,
                BuildOutputProducer.BUILD_OUTPUT_PRODUCER_ARTIFACTORY,
                BuildOutputProducer.BUILD_OUTPUT_PRODUCER_JENKINS,
                BuildOutputProducer.BUILD_OUTPUT_PRODUCER_BUILD_ARTIFACT_WORKER
            ),
            loaded.getSourceSnapshot().getBuildOutputPackage().getResolution().getCandidatesList().stream()
                .map(candidate -> candidate.getProducer())
                .toList()
        );
        assertEquals(
            BuildOutputProducer.BUILD_OUTPUT_PRODUCER_UNSPECIFIED,
            loaded.getSourceSnapshot().getBuildOutputPackage().getResolution().getSelectedProducer()
        );
        assertEquals(RepositoryWorkspaceStatus.REPOSITORY_WORKSPACE_STATUS_CLEANED, cleaned.getWorkspaceStatus());
        assertEquals("CLEANED", cleaned.getStatus().getCode());
    }

    @Test
    void previewsCreatesGetsAndRefreshesRepositoryWorkspaceOwnerApi() {
        var preview = stub.previewRepositoryWorkspaceMetadata(metadataRequest());

        assertEquals("example.com/acme/demo", preview.getRepository().getRepositoryKey());
        assertEquals("demo", preview.getWorkspaceTitle());
        assertEquals("METADATA_RESOLVED", preview.getStatus().getCode());
        assertEquals("demo", preview.getSafeAttributesOrThrow("tenant"));
        assertEquals(1, metadataPort.calls);
        assertEquals(0, workspacePort.branchCheckouts);

        var created = stub.createRepositoryWorkspace(createWorkspaceRequest("workspace-key")
            .toBuilder()
            .setBranchSelector(RepositoryWorkspaceBranchSelector.newBuilder())
            .build());
        var replayed = stub.createRepositoryWorkspace(createWorkspaceRequest("workspace-key")
            .toBuilder()
            .setBranchSelector(RepositoryWorkspaceBranchSelector.newBuilder())
            .build());
        var workspace = created.getWorkspace();
        var branch = workspace.getBranches(0);
        var loaded = stub.getRepositoryWorkspace(GetRepositoryWorkspaceRequest.newBuilder()
            .setRequestId("request-workspace-get")
            .setCorrelationId("correlation-1")
            .setWorkspaceId(workspace.getWorkspaceId())
            .build());

        assertEquals("WORKSPACE_ACCEPTED", created.getStatus().getCode());
        assertEquals(workspace.getWorkspaceId(), replayed.getWorkspace().getWorkspaceId());
        assertEquals("workspace-0001", workspace.getWorkspaceId());
        assertEquals("demo", workspace.getWorkspaceTitle());
        assertEquals("main", branch.getRepositoryBranch());
        assertEquals(RepositoryWorkspaceBranchStatus.REPOSITORY_WORKSPACE_BRANCH_STATUS_CHECKED_OUT, branch.getStatus());
        assertEquals("b".repeat(40), branch.getResolvedCommit());
        assertEquals("src/main/java", branch.getSourceRoots(0).getRelativePath());
        assertFalse(branch.getWorkspaceBranchId().contains("/"));
        assertEquals(workspace.getWorkspaceId(), loaded.getWorkspaceId());
        assertEquals(1, idGenerator.workspaceIds);
        assertEquals(1, idGenerator.branchIds);
        assertEquals(1, checkoutPort.calls);

        var upToDate = stub.refreshRepositoryWorkspaceBranch(refreshRequest(
            "refresh-same",
            workspace.getWorkspaceId(),
            branch.getWorkspaceBranchId()
        ));
        var updated = stub.refreshRepositoryWorkspaceBranch(refreshRequest(
            "refresh-new",
            workspace.getWorkspaceId(),
            branch.getWorkspaceBranchId()
        ));

        assertFalse(upToDate.getChanged());
        assertEquals("BRANCH_UP_TO_DATE", upToDate.getStatus().getCode());
        assertEquals(RepositoryWorkspaceBranchStatus.REPOSITORY_WORKSPACE_BRANCH_STATUS_UP_TO_DATE, upToDate.getBranch().getStatus());
        assertEquals("b".repeat(40), upToDate.getPreviousCommit());
        assertEquals("c".repeat(40), updated.getBranch().getResolvedCommit());
        assertEquals("BRANCH_UPDATED", updated.getStatus().getCode());
        assertEquals(RepositoryWorkspaceBranchStatus.REPOSITORY_WORKSPACE_BRANCH_STATUS_UPDATED, updated.getBranch().getStatus());
        assertEquals(3, checkoutPort.calls);
    }

    @Test
    void listsRepositoryWorkspacesAndCleanupByIdThroughGrpcOwnerApi() {
        var cleanedWorkspace = stub.createRepositoryWorkspace(createWorkspaceRequest("workspace-key-alpha")).getWorkspace();
        var visibleWorkspace = stub.createRepositoryWorkspace(createWorkspaceRequest("workspace-key-beta")
            .toBuilder()
            .setRepository(RepositoryReference.newBuilder()
                .setRemoteUrl("https://example.com/acme/other.git")
                .setProvider("github"))
            .build()).getWorkspace();

        var cleaned = stub.cleanupRepositoryWorkspaceById(cleanupByIdRequest(
            "cleanup-alpha",
            cleanedWorkspace.getWorkspaceId(),
            Map.of("tenant", "demo")
        ));
        var visible = stub.listRepositoryWorkspaces(listRequest(false));
        var all = stub.listRepositoryWorkspaces(listRequest(true));

        assertEquals(RepositoryWorkspaceStatus.REPOSITORY_WORKSPACE_STATUS_CLEANED, cleaned.getWorkspaceStatus());
        assertEquals("CLEANED", cleaned.getStatus().getCode());
        assertEquals("correlation-1", cleaned.getStatus().getCorrelationId());
        assertEquals(List.of("WORKSPACE_CLEANED"), cleaned.getDiagnosticsList().stream()
            .map(diagnostic -> diagnostic.getCode())
            .toList());
        assertEquals(0, cleaned.getSafeAttributesCount());
        assertEquals("WORKSPACES_LISTED", visible.getStatus().getCode());
        assertEquals(List.of(visibleWorkspace.getWorkspaceId()), visible.getWorkspacesList().stream()
            .map(workspace -> workspace.getWorkspaceId())
            .toList());
        assertEquals(List.of(cleanedWorkspace.getWorkspaceId(), visibleWorkspace.getWorkspaceId()), all.getWorkspacesList().stream()
            .map(workspace -> workspace.getWorkspaceId())
            .toList());
        assertEquals("demo", all.getWorkspaces(0).getWorkspaceTitle());
        assertEquals("other", all.getWorkspaces(1).getWorkspaceTitle());
        assertEquals("main", all.getWorkspaces(1).getBranches(0).getRepositoryBranch());
        assertFalse(cleaned.getWorkspaceId().contains("/"));
        assertFalse(cleaned.getWorkspaceId().contains("\\"));
    }

    @Test
    void mapsListAndCleanupByIdErrorsToSanitizedGrpcStatuses() {
        var workspace = stub.createRepositoryWorkspace(createWorkspaceRequest("workspace-key")).getWorkspace();

        var invalidList = assertThrows(
            StatusRuntimeException.class,
            () -> stub.listRepositoryWorkspaces(ListRepositoryWorkspacesRequest.getDefaultInstance())
        );
        var missing = assertThrows(
            StatusRuntimeException.class,
            () -> stub.cleanupRepositoryWorkspaceById(cleanupByIdRequest(
                "cleanup-missing",
                "workspace-missing",
                Map.of("tenant", "demo")
            ))
        );
        stub.cleanupRepositoryWorkspaceById(cleanupByIdRequest(
            "cleanup-conflict",
            workspace.getWorkspaceId(),
            Map.of("tenant", "demo")
        ));
        var conflict = assertThrows(
            StatusRuntimeException.class,
            () -> stub.cleanupRepositoryWorkspaceById(cleanupByIdRequest(
                "cleanup-conflict",
                workspace.getWorkspaceId(),
                Map.of("tenant", "other")
            ))
        );
        var unsafeAttribute = assertThrows(
            StatusRuntimeException.class,
            () -> stub.cleanupRepositoryWorkspaceById(cleanupByIdRequest(
                "cleanup-unsafe",
                workspace.getWorkspaceId(),
                Map.of("note", "raw stderr at /tmp/private/workspace")
            ))
        );
        saveInProgressWorkspace("workspace-in-progress");
        var inProgress = assertThrows(
            StatusRuntimeException.class,
            () -> stub.cleanupRepositoryWorkspaceById(cleanupByIdRequest(
                "cleanup-in-progress",
                "workspace-in-progress",
                Map.of("tenant", "demo")
            ))
        );

        assertEquals(Status.Code.INVALID_ARGUMENT, invalidList.getStatus().getCode());
        assertEquals("Invalid repository source request", invalidList.getStatus().getDescription());
        assertEquals(Status.Code.NOT_FOUND, missing.getStatus().getCode());
        assertEquals(Status.Code.ALREADY_EXISTS, conflict.getStatus().getCode());
        assertEquals(Status.Code.INVALID_ARGUMENT, unsafeAttribute.getStatus().getCode());
        assertEquals("Invalid repository source request", unsafeAttribute.getStatus().getDescription());
        assertEquals(Status.Code.FAILED_PRECONDITION, inProgress.getStatus().getCode());
        assertEquals("Repository workspace operation failed", inProgress.getStatus().getDescription());
    }

    @Test
    void mapsWorkspaceOwnerApiValidationConflictAndMissingToGrpcStatuses() {
        var workspace = stub.createRepositoryWorkspace(createWorkspaceRequest("workspace-key")).getWorkspace();
        var branch = workspace.getBranches(0);
        stub.refreshRepositoryWorkspaceBranch(refreshRequest(
            "refresh-key",
            workspace.getWorkspaceId(),
            branch.getWorkspaceBranchId()
        ));

        var createConflict = assertThrows(
            StatusRuntimeException.class,
            () -> stub.createRepositoryWorkspace(createWorkspaceRequest("workspace-key")
                .toBuilder()
                .setBranchSelector(RepositoryWorkspaceBranchSelector.newBuilder().setBranch("release/1.0"))
                .build())
        );
        var refreshConflict = assertThrows(
            StatusRuntimeException.class,
            () -> stub.refreshRepositoryWorkspaceBranch(refreshRequest(
                "refresh-key",
                workspace.getWorkspaceId(),
                branch.getWorkspaceBranchId()
            ).toBuilder()
                .setWorkspacePolicy(workspacePolicy(30))
                .build())
        );
        var missingWorkspace = assertThrows(
            StatusRuntimeException.class,
            () -> stub.getRepositoryWorkspace(GetRepositoryWorkspaceRequest.newBuilder()
                .setRequestId("request-missing-workspace")
                .setCorrelationId("correlation-1")
                .setWorkspaceId("workspace-missing")
                .build())
        );
        var missingBranch = assertThrows(
            StatusRuntimeException.class,
            () -> stub.refreshRepositoryWorkspaceBranch(refreshRequest(
                "refresh-missing",
                workspace.getWorkspaceId(),
                "workspace-branch-missing"
            ))
        );
        var invalid = assertThrows(
            StatusRuntimeException.class,
            () -> stub.previewRepositoryWorkspaceMetadata(metadataRequest().toBuilder()
                .setRepository(RepositoryReference.newBuilder()
                    .setRemoteUrl("file:///tmp/private/repo")
                    .setProvider("local"))
                .build())
        );
        var unsafeAttribute = assertThrows(
            StatusRuntimeException.class,
            () -> stub.createRepositoryWorkspace(createWorkspaceRequest("unsafe-attribute")
                .toBuilder()
                .putSafeAttributes("note", "raw stderr at /tmp/private/workspace")
                .build())
        );

        assertEquals(Status.Code.ALREADY_EXISTS, createConflict.getStatus().getCode());
        assertEquals(Status.Code.ALREADY_EXISTS, refreshConflict.getStatus().getCode());
        assertEquals(Status.Code.NOT_FOUND, missingWorkspace.getStatus().getCode());
        assertEquals(Status.Code.NOT_FOUND, missingBranch.getStatus().getCode());
        assertEquals(Status.Code.INVALID_ARGUMENT, invalid.getStatus().getCode());
        assertEquals("Invalid repository source request", invalid.getStatus().getDescription());
        assertEquals(Status.Code.INVALID_ARGUMENT, unsafeAttribute.getStatus().getCode());
        assertEquals("Invalid repository source request", unsafeAttribute.getStatus().getDescription());
        assertEquals(2, checkoutPort.calls);
    }

    @Test
    void keepsJavaAstAnalysisOutsideRepositorySourceOwnership() {
        var failure = assertThrows(
            StatusRuntimeException.class,
            () -> stub.analyzeSourceSnapshotWithJavaAst(AnalyzeSourceSnapshotWithJavaAstRequest.newBuilder()
                .setRequestId("request-ast")
                .setIdempotencyKey("ast-key")
                .setSchemaVersion("schema-v1")
                .setCorrelationId("correlation-1")
                .setAnalysisRunId("run-1")
                .setAnalysisJobId("job-1")
                .setSourceSnapshotId("source-snapshot-1")
                .build())
        );

        assertEquals(Status.Code.UNIMPLEMENTED, failure.getStatus().getCode());
    }

    @Test
    void rejectsLegacyLocalRepositoryInputsAtGrpcBoundaryWithoutLeakingPaths() {
        for (String remote : List.of(
            "file:///tmp/repo",
            "/tmp/repo",
            "C:/tmp/repo",
            "ssh://example.com/repo.git",
            "git@example.com:org/repo.git"
        )) {
            var failure = assertThrows(
                StatusRuntimeException.class,
                () -> stub.prepareRepository(prepareRequest("prepare-" + Math.abs(remote.hashCode()), "schema-v1")
                    .toBuilder()
                    .setRepository(RepositoryReference.newBuilder()
                        .setRemoteUrl(remote)
                        .setProvider("legacy-input"))
                    .build())
            );

            assertEquals(Status.Code.INVALID_ARGUMENT, failure.getStatus().getCode());
            assertEquals("Invalid repository source request", failure.getStatus().getDescription());
        }
    }

    @Test
    void rejectsPrivateSafeAttributesAtGrpcBoundaryWithoutLeakingPaths() {
        var failure = assertThrows(
            StatusRuntimeException.class,
            () -> stub.prepareRepository(prepareRequest("prepare-private-attribute", "schema-v1")
                .toBuilder()
                .putSafeAttributes("note", "checkout failed at /tmp/private/workspace")
                .build())
        );

        assertEquals(Status.Code.INVALID_ARGUMENT, failure.getStatus().getCode());
        assertEquals("Invalid repository source request", failure.getStatus().getDescription());
    }

    @Test
    void mapsValidationMissingAndConflictToGrpcStatuses() {
        stub.prepareRepository(prepareRequest("prepare-1", "schema-v1"));

        var invalid = assertThrows(
            StatusRuntimeException.class,
            () -> stub.prepareRepository(PrepareRepositoryRequest.getDefaultInstance())
        );
        var conflict = assertThrows(
            StatusRuntimeException.class,
            () -> stub.prepareRepository(prepareRequest("prepare-1", "schema-v2"))
        );
        var missing = assertThrows(
            StatusRuntimeException.class,
            () -> stub.getRepositoryPreparation(GetRepositoryPreparationRequest.newBuilder()
                .setRequestId("request-get")
                .setCorrelationId("correlation-1")
                .setAnalysisRunId("run-1")
                .setSourceSnapshotId("missing")
                .build())
        );

        assertEquals(Status.Code.INVALID_ARGUMENT, invalid.getStatus().getCode());
        assertEquals("Invalid repository source request", invalid.getStatus().getDescription());
        assertEquals(Status.Code.ALREADY_EXISTS, conflict.getStatus().getCode());
        assertEquals(Status.Code.NOT_FOUND, missing.getStatus().getCode());
    }

    @Test
    void mapsPackageDescriptorEnumsAcrossGrpcBoundary() {
        assertEquals(
            PackageAvailability.PACKAGE_AVAILABILITY_AVAILABLE,
            RepositorySourceGrpcEndpoint.packageAvailability(
                de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.PackageAvailability.AVAILABLE
            )
        );
        assertEquals(
            PackageAvailability.PACKAGE_AVAILABILITY_UNAVAILABLE,
            RepositorySourceGrpcEndpoint.packageAvailability(
                de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.PackageAvailability.UNAVAILABLE
            )
        );
        assertEquals(
            PackageAvailability.PACKAGE_AVAILABILITY_FAILED_INTEGRITY,
            RepositorySourceGrpcEndpoint.packageAvailability(
                de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.PackageAvailability.FAILED_INTEGRITY
            )
        );
        assertEquals(
            de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputProducerStatus.BUILD_OUTPUT_PRODUCER_STATUS_AVAILABLE,
            RepositorySourceGrpcEndpoint.buildOutputProducerStatus(
                de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.BuildOutputProducerStatus.AVAILABLE
            )
        );
        assertEquals(
            de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputProducerStatus.BUILD_OUTPUT_PRODUCER_STATUS_MISSING,
            RepositorySourceGrpcEndpoint.buildOutputProducerStatus(
                de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.BuildOutputProducerStatus.MISSING
            )
        );
        assertEquals(
            de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputProducerStatus.BUILD_OUTPUT_PRODUCER_STATUS_TERMINAL_INTEGRITY_FAILURE,
            RepositorySourceGrpcEndpoint.buildOutputProducerStatus(
                de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.BuildOutputProducerStatus.TERMINAL_INTEGRITY_FAILURE
            )
        );
        assertEquals(
            de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_SCOPED_OBJECT_ACCESS,
            RepositorySourceGrpcEndpoint.byteCustody(
                de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.ArtifactByteCustody.SCOPED_OBJECT_ACCESS
            )
        );
        assertEquals(
            de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_EXPLICIT_HANDOFF,
            RepositorySourceGrpcEndpoint.byteCustody(
                de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.ArtifactByteCustody.EXPLICIT_HANDOFF
            )
        );
    }

    @Test
    void redactsInternalFailureDescriptionsFromGrpcErrors() throws Exception {
        stopServer();
        startServerWithCheckout((workspace, repository, revision, policy) -> {
            throw new IllegalStateException("checkout failed at /tmp/private/workspace");
        });

        var failure = assertThrows(
            StatusRuntimeException.class,
            () -> stub.prepareRepository(prepareRequest("prepare-fails", "schema-v1"))
        );

        assertEquals(Status.Code.FAILED_PRECONDITION, failure.getStatus().getCode());
        assertEquals("Repository preparation failed", failure.getStatus().getDescription());
    }

    private void startServerWithCheckout(RepositoryCheckoutPort checkoutPort) throws IOException {
        var serverName = InProcessServerBuilder.generateName();
        var repository = new InMemoryRepositoryPreparationRepository();
        var workspaceRepository = new InMemoryRepositoryWorkspaceRepository();
        var idempotencyRepository = new InMemoryRepositorySourceIdempotencyRepository();
        var workspacePort = new FakeWorkspacePort();
        var applicationService = new RepositorySourceApplicationService(
            repository,
            idempotencyRepository,
            workspacePort,
            checkoutPort,
            Clock.fixed(Instant.parse("2026-05-16T10:15:30Z"), ZoneOffset.UTC)
        );
        var workspaceApplicationService = new RepositoryWorkspaceApplicationService(
            workspaceRepository,
            new FixedRepositoryWorkspaceIdGenerator(),
            idempotencyRepository,
            workspacePort,
            checkoutPort,
            new FakeMetadataPort("main", true),
            Clock.fixed(Instant.parse("2026-05-16T10:15:30Z"), ZoneOffset.UTC)
        );
        server = InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(new RepositorySourceGrpcEndpoint(applicationService, workspaceApplicationService))
            .build()
            .start();
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        stub = RepositoryAnalysisServiceGrpc.newBlockingStub(channel);
    }

    private static PrepareRepositoryRequest prepareRequest(String idempotencyKey, String schemaVersion) {
        return PrepareRepositoryRequest.newBuilder()
            .setRequestId("request-prepare")
            .setIdempotencyKey(idempotencyKey)
            .setSchemaVersion(schemaVersion)
            .setCorrelationId("correlation-1")
            .setAnalysisRunId("run-1")
            .setRepository(RepositoryReference.newBuilder()
                .setRemoteUrl("https://example.com/acme/demo.git")
                .setProvider("github"))
            .setRevision(RevisionSelector.newBuilder()
                .setBranch("main")
                .setBranchRequired(true))
            .setWorkspacePolicy(WorkspacePolicy.newBuilder()
                .setEphemeral(true)
                .setAllowShallowClone(true)
                .setTimeoutSeconds(60)
                .setMaxWorkspaceBytes(100_000))
            .putSafeAttributes("tenant", "demo")
            .build();
    }

    private static PreviewRepositoryWorkspaceMetadataRequest metadataRequest() {
        return PreviewRepositoryWorkspaceMetadataRequest.newBuilder()
            .setRequestId("request-metadata")
            .setSchemaVersion("schema-v1")
            .setCorrelationId("correlation-1")
            .setRepository(RepositoryReference.newBuilder()
                .setRemoteUrl("https://example.com/acme/demo.git")
                .setProvider("github"))
            .setMetadataPolicy(MetadataPreviewPolicy.newBuilder().setTimeoutSeconds(30))
            .putSafeAttributes("tenant", "demo")
            .build();
    }

    private static CreateRepositoryWorkspaceRequest createWorkspaceRequest(String idempotencyKey) {
        return CreateRepositoryWorkspaceRequest.newBuilder()
            .setRequestId("request-workspace")
            .setIdempotencyKey(idempotencyKey)
            .setSchemaVersion("schema-v1")
            .setCorrelationId("correlation-1")
            .setRepository(RepositoryReference.newBuilder()
                .setRemoteUrl("https://example.com/acme/demo.git")
                .setProvider("github"))
            .setBranchSelector(RepositoryWorkspaceBranchSelector.newBuilder().setBranch("main"))
            .setWorkspacePolicy(workspacePolicy(60))
            .putSafeAttributes("tenant", "demo")
            .build();
    }

    private static RefreshRepositoryWorkspaceBranchRequest refreshRequest(
        String idempotencyKey,
        String workspaceId,
        String workspaceBranchId
    ) {
        return RefreshRepositoryWorkspaceBranchRequest.newBuilder()
            .setRequestId("request-refresh")
            .setIdempotencyKey(idempotencyKey)
            .setSchemaVersion("schema-v1")
            .setCorrelationId("correlation-1")
            .setWorkspaceId(workspaceId)
            .setWorkspaceBranchId(workspaceBranchId)
            .setWorkspacePolicy(workspacePolicy(60))
            .build();
    }

    private static ListRepositoryWorkspacesRequest listRequest(boolean includeCleaned) {
        return ListRepositoryWorkspacesRequest.newBuilder()
            .setRequestId("request-list")
            .setSchemaVersion("schema-v1")
            .setCorrelationId("correlation-1")
            .setIncludeCleaned(includeCleaned)
            .build();
    }

    private static CleanupRepositoryWorkspaceByIdRequest cleanupByIdRequest(
        String idempotencyKey,
        String workspaceId,
        Map<String, String> attributes
    ) {
        return CleanupRepositoryWorkspaceByIdRequest.newBuilder()
            .setRequestId("request-cleanup-by-id")
            .setIdempotencyKey(idempotencyKey)
            .setSchemaVersion("schema-v1")
            .setCorrelationId("correlation-1")
            .setWorkspaceId(workspaceId)
            .putAllSafeAttributes(attributes)
            .build();
    }

    private static WorkspacePolicy workspacePolicy(long timeoutSeconds) {
        return WorkspacePolicy.newBuilder()
            .setEphemeral(true)
            .setAllowShallowClone(true)
            .setTimeoutSeconds(timeoutSeconds)
            .setMaxWorkspaceBytes(100_000)
            .build();
    }

    private void saveInProgressWorkspace(String workspaceId) {
        var id = new WorkspaceId(workspaceId);
        var timestamp = Instant.parse("2026-05-16T10:15:30Z");
        workspaceRepository.save(new RepositoryWorkspace(
            id,
            new WorkspaceTitle("in-progress"),
            repositoryIdentity("progress"),
            de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceStatus.READY,
            timestamp,
            timestamp,
            List.of(new RepositoryWorkspaceBranch(
                new WorkspaceBranchId("workspace-branch-progress"),
                id,
                "main",
                "",
                "",
                null,
                de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceBranchStatus.CHECKING_OUT,
                List.of(),
                null,
                timestamp,
                List.of(Diagnostic.info("REPOSITORY_WORKSPACE_BRANCH_CREATED", "Repository workspace branch was created"))
            )),
            List.of(Diagnostic.info("REPOSITORY_WORKSPACE_READY", "Repository workspace is ready")),
            Map.of("tenant", "demo")
        ));
    }

    private static RepositoryIdentity repositoryIdentity(String repositoryName) {
        return RepositoryIdentity.from(
            new de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryReference(
                "https://example.com/acme/" + repositoryName + ".git",
                "github",
                Map.of()
            ),
            "main"
        );
    }

    private static final class FakeWorkspacePort implements RepositoryWorkspacePort {
        private int branchCheckouts;

        @Override
        public PreparedWorkspace prepare(
            de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.AnalysisRunId analysisRunId,
            de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspacePolicy policy
        ) {
            return new PreparedWorkspace(new WorkspaceId("workspace-" + analysisRunId.value()), Path.of("memory"));
        }

        @Override
        public PreparedWorkspace prepareBranchCheckout(
            WorkspaceId workspaceId,
            WorkspaceBranchId workspaceBranchId,
            de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspacePolicy policy
        ) {
            branchCheckouts++;
            return new PreparedWorkspace(workspaceId, Path.of("memory", workspaceId.value(), "branches", workspaceBranchId.value()));
        }

        @Override
        public void cleanup(WorkspaceId workspaceId) {
        }

        @Override
        public void cleanupBranchCheckout(WorkspaceId workspaceId, WorkspaceBranchId workspaceBranchId) {
        }
    }

    private static final class SequencedCheckoutPort implements RepositoryCheckoutPort {
        private final List<String> commits;
        private int calls;

        private SequencedCheckoutPort(String... commits) {
            this.commits = new ArrayList<>(List.of(commits));
        }

        @Override
        public CheckoutResult checkout(
            PreparedWorkspace workspace,
            de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryReference repository,
            de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RevisionSelector revision,
            de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspacePolicy policy
        ) {
            calls++;
            var commit = commits.isEmpty() ? "b".repeat(40) : commits.removeFirst();
            return new CheckoutResult(
                CheckoutStatus.CHECKED_OUT,
                repository.remoteUrl(),
                commit,
                revision.branch(),
                revision.commit(),
                true,
                5,
                List.of(Diagnostic.info("OK", "checkout")),
                false,
                false,
                List.of(new SourceRoot("src/main/java", "java"))
            );
        }
    }

    private static final class FakeMetadataPort implements RepositoryMetadataPort {
        private final String defaultBranch;
        private final boolean resolved;
        private int calls;

        private FakeMetadataPort(String defaultBranch, boolean resolved) {
            this.defaultBranch = defaultBranch;
            this.resolved = resolved;
        }

        @Override
        public RepositoryMetadataResolution resolveMetadata(
            de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryReference repository,
            de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryMetadataPreviewPolicy policy
        ) {
            calls++;
            return new RepositoryMetadataResolution(
                RepositoryIdentity.from(repository, defaultBranch),
                resolved,
                List.of(Diagnostic.info("DEFAULT_BRANCH_RESOLVED", "Repository default branch resolved"))
            );
        }
    }

    private static final class FixedRepositoryWorkspaceIdGenerator implements RepositoryWorkspaceIdGenerator {
        private int workspaceIds;
        private int branchIds;

        @Override
        public WorkspaceId newWorkspaceId() {
            workspaceIds++;
            return new WorkspaceId("workspace-%04d".formatted(workspaceIds));
        }

        @Override
        public WorkspaceBranchId newWorkspaceBranchId() {
            branchIds++;
            return new WorkspaceBranchId("workspace-branch-%04d".formatted(branchIds));
        }
    }

}
