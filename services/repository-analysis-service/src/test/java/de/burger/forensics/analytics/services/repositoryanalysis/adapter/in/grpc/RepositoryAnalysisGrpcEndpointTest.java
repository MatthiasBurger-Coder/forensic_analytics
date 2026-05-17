package de.burger.forensics.analytics.services.repositoryanalysis.adapter.in.grpc;

import de.burger.forensics.analytics.repositoryanalysis.v1.CleanupRepositoryWorkspaceRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.GetRepositoryPreparationRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.PrepareRepositoryRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryAnalysisServiceGrpc;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryReference;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryWorkspaceStatus;
import de.burger.forensics.analytics.repositoryanalysis.v1.RevisionSelector;
import de.burger.forensics.analytics.repositoryanalysis.v1.WorkspacePolicy;
import de.burger.forensics.analytics.services.repositoryanalysis.adapter.out.memory.InMemoryRepositoryPreparationRepository;
import de.burger.forensics.analytics.services.repositoryanalysis.application.RepositoryAnalysisApplicationService;
import de.burger.forensics.analytics.services.repositoryanalysis.application.port.PreparedWorkspace;
import de.burger.forensics.analytics.services.repositoryanalysis.application.port.RepositoryCheckoutPort;
import de.burger.forensics.analytics.services.repositoryanalysis.application.port.RepositoryWorkspacePort;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.CheckoutResult;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.CheckoutStatus;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.Diagnostic;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceRoot;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.WorkspaceId;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

class RepositoryAnalysisGrpcEndpointTest {
    private Server server;
    private ManagedChannel channel;
    private RepositoryAnalysisServiceGrpc.RepositoryAnalysisServiceBlockingStub stub;

    @BeforeEach
    void startServer() throws IOException {
        var serverName = InProcessServerBuilder.generateName();
        var applicationService = new RepositoryAnalysisApplicationService(
            new InMemoryRepositoryPreparationRepository(),
            new FakeWorkspacePort(),
            new FakeCheckoutPort(),
            Clock.fixed(Instant.parse("2026-05-16T10:15:30Z"), ZoneOffset.UTC)
        );
        server = InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(new RepositoryAnalysisGrpcEndpoint(applicationService))
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
        assertEquals("src/main/java", loaded.getSourceSnapshot().getSourceRoots(0).getRelativePath());
        assertEquals(RepositoryWorkspaceStatus.REPOSITORY_WORKSPACE_STATUS_CLEANED, cleaned.getWorkspaceStatus());
        assertEquals("CLEANED", cleaned.getStatus().getCode());
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
        assertEquals("Invalid repository analysis request", invalid.getStatus().getDescription());
        assertEquals(Status.Code.ALREADY_EXISTS, conflict.getStatus().getCode());
        assertEquals(Status.Code.NOT_FOUND, missing.getStatus().getCode());
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
        var applicationService = new RepositoryAnalysisApplicationService(
            new InMemoryRepositoryPreparationRepository(),
            new FakeWorkspacePort(),
            checkoutPort,
            Clock.fixed(Instant.parse("2026-05-16T10:15:30Z"), ZoneOffset.UTC)
        );
        server = InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(new RepositoryAnalysisGrpcEndpoint(applicationService))
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
            de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.AnalysisRunId analysisRunId,
            de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.WorkspacePolicy policy
        ) {
            return new PreparedWorkspace(new WorkspaceId("workspace-" + analysisRunId.value()), Path.of("memory"));
        }

        @Override
        public void cleanup(WorkspaceId workspaceId) {
        }
    }

    private static final class FakeCheckoutPort implements RepositoryCheckoutPort {
        @Override
        public CheckoutResult checkout(
            PreparedWorkspace workspace,
            de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.RepositoryReference repository,
            de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.RevisionSelector revision,
            de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.WorkspacePolicy policy
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
