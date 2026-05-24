package de.burger.forensics.analytics.services.repositorysource.adapter.in.grpc;

import de.burger.forensics.analytics.repositoryanalysis.v1.AnalyzeSourceSnapshotWithJavaAstRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.CleanupRepositoryWorkspaceRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.GetRepositoryPreparationRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputProducer;
import de.burger.forensics.analytics.repositoryanalysis.v1.PackageAvailability;
import de.burger.forensics.analytics.repositoryanalysis.v1.PrepareRepositoryRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryAnalysisServiceGrpc;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryReference;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryWorkspaceStatus;
import de.burger.forensics.analytics.repositoryanalysis.v1.RevisionSelector;
import de.burger.forensics.analytics.repositoryanalysis.v1.WorkspacePolicy;
import de.burger.forensics.analytics.services.repositorysource.adapter.out.memory.InMemoryRepositoryPreparationRepository;
import de.burger.forensics.analytics.services.repositorysource.application.RepositorySourceApplicationService;
import de.burger.forensics.analytics.services.repositorysource.application.port.PreparedWorkspace;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryCheckoutPort;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryWorkspacePort;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.ArtifactByteCustody;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.CheckoutResult;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.CheckoutStatus;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.Diagnostic;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.SourceRoot;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceBranchId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceId;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RepositorySourceGrpcEndpointTest {
    private Server server;
    private ManagedChannel channel;
    private RepositoryAnalysisServiceGrpc.RepositoryAnalysisServiceBlockingStub stub;

    @BeforeEach
    void startServer() throws IOException {
        var serverName = InProcessServerBuilder.generateName();
        var repository = new InMemoryRepositoryPreparationRepository();
        var applicationService = new RepositorySourceApplicationService(
            repository,
            new FakeWorkspacePort(),
            new FakeCheckoutPort(),
            Clock.fixed(Instant.parse("2026-05-16T10:15:30Z"), ZoneOffset.UTC)
        );
        server = InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(new RepositorySourceGrpcEndpoint(applicationService))
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
        var applicationService = new RepositorySourceApplicationService(
            repository,
            new FakeWorkspacePort(),
            checkoutPort,
            Clock.fixed(Instant.parse("2026-05-16T10:15:30Z"), ZoneOffset.UTC)
        );
        server = InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(new RepositorySourceGrpcEndpoint(applicationService))
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

    private static final class FakeWorkspacePort implements RepositoryWorkspacePort {
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
            return new PreparedWorkspace(workspaceId, Path.of("memory", workspaceId.value(), "branches", workspaceBranchId.value()));
        }

        @Override
        public void cleanup(WorkspaceId workspaceId) {
        }

        @Override
        public void cleanupBranchCheckout(WorkspaceId workspaceId, WorkspaceBranchId workspaceBranchId) {
        }
    }

    private static final class FakeCheckoutPort implements RepositoryCheckoutPort {
        @Override
        public CheckoutResult checkout(
            PreparedWorkspace workspace,
            de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryReference repository,
            de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RevisionSelector revision,
            de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspacePolicy policy
        ) {
            return new CheckoutResult(
                CheckoutStatus.CHECKED_OUT,
                repository.remoteUrl(),
                "b".repeat(40),
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

}
