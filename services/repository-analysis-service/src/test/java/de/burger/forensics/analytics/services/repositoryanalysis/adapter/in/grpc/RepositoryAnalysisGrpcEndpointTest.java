package de.burger.forensics.analytics.services.repositoryanalysis.adapter.in.grpc;

import de.burger.forensics.analytics.repositoryanalysis.v1.CleanupRepositoryWorkspaceRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.GetRepositoryPreparationRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.AnalyzeSourceSnapshotWithJavaAstRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputProducer;
import de.burger.forensics.analytics.repositoryanalysis.v1.PackageAvailability;
import de.burger.forensics.analytics.repositoryanalysis.v1.PrepareRepositoryRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryAnalysisServiceGrpc;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryReference;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryWorkspaceStatus;
import de.burger.forensics.analytics.repositoryanalysis.v1.RevisionSelector;
import de.burger.forensics.analytics.repositoryanalysis.v1.SourceSnapshotHandoffPolicy;
import de.burger.forensics.analytics.repositoryanalysis.v1.WorkspacePolicy;
import de.burger.forensics.analytics.services.repositoryanalysis.adapter.out.memory.InMemoryRepositoryPreparationRepository;
import de.burger.forensics.analytics.services.repositoryanalysis.application.RepositoryAnalysisApplicationService;
import de.burger.forensics.analytics.services.repositoryanalysis.application.RepositorySourceSnapshotHandoffService;
import de.burger.forensics.analytics.services.repositoryanalysis.application.port.JavaAstAnalysisPort;
import de.burger.forensics.analytics.services.repositoryanalysis.application.port.PreparedWorkspace;
import de.burger.forensics.analytics.services.repositoryanalysis.application.port.RepositoryCheckoutPort;
import de.burger.forensics.analytics.services.repositoryanalysis.application.port.SourceSnapshotFileCollectorPort;
import de.burger.forensics.analytics.services.repositoryanalysis.application.port.RepositoryWorkspacePort;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.AnalysisJobId;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.ArtifactByteAccess;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.ArtifactByteCustody;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.ArtifactReference;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.CheckoutResult;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.CheckoutStatus;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.Diagnostic;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.JavaAstAnalysisHandoffCommand;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.JavaAstAnalysisHandoffResult;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.JavaAstScanSummary;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceRoot;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceSnapshotCompleteness;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceSnapshotSourceFile;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.sha256Hex;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RepositoryAnalysisGrpcEndpointTest {
    private Server server;
    private ManagedChannel channel;
    private RepositoryAnalysisServiceGrpc.RepositoryAnalysisServiceBlockingStub stub;

    @BeforeEach
    void startServer() throws IOException {
        var serverName = InProcessServerBuilder.generateName();
        var repository = new InMemoryRepositoryPreparationRepository();
        var applicationService = new RepositoryAnalysisApplicationService(
            repository,
            new FakeWorkspacePort(),
            new FakeCheckoutPort(),
            Clock.fixed(Instant.parse("2026-05-16T10:15:30Z"), ZoneOffset.UTC)
        );
        var handoffService = new RepositorySourceSnapshotHandoffService(
            repository,
            new FakeSourceFileCollector(),
            new FakeJavaAstPort()
        );
        server = InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(new RepositoryAnalysisGrpcEndpoint(applicationService, handoffService))
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
        var handoff = stub.analyzeSourceSnapshotWithJavaAst(AnalyzeSourceSnapshotWithJavaAstRequest.newBuilder()
            .setRequestId("request-handoff")
            .setIdempotencyKey("handoff-1")
            .setSchemaVersion("java-ast-analysis-v1")
            .setCorrelationId("correlation-1")
            .setAnalysisRunId("run-1")
            .setAnalysisJobId("job-ast-1")
            .setSourceSnapshotId(prepared.getPreparation().getSourceSnapshotId())
            .setHandoffPolicy(SourceSnapshotHandoffPolicy.newBuilder()
                .setMaxFiles(10)
                .setMaxSourceBytes(10_000)
                .setTimeoutSeconds(30))
            .putSafeAttributes("tenant", "demo")
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
        assertEquals(PackageAvailability.PACKAGE_AVAILABILITY_PENDING, loaded.getSourceSnapshot().getSourcePackage().getAvailability());
        assertEquals("repository-analysis-service", loaded.getSourceSnapshot().getSourcePackage().getByteAccess().getOwnerService());
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
        assertEquals("JAVA_AST_HANDOFF_COMPLETED", handoff.getStatus().getCode());
        assertEquals("java-ast-analysis-service", handoff.getSourceFactArtifact().getProducerService());
        assertEquals("java-ast-analysis-v1", handoff.getSourceFactArtifact().getSchemaVersion());
        assertEquals("java-ast-analysis-service", handoff.getSourceFactArtifact().getByteAccess().getOwnerService());
        assertEquals(
            "java-ast-analysis.v1.JavaAstAnalysisService.GetSourceFactArtifactBytes",
            handoff.getSourceFactArtifact().getByteAccess().getRetrievalContract()
        );
        assertEquals("java-ast/snapshot-1-job-ast-1-source-facts.json", handoff.getSourceFactArtifact().getByteAccess().getRetrievalReference());
        assertEquals(1, handoff.getSummary().getSourceFactCount());
        assertEquals("demo", handoff.getSafeAttributesMap().get("tenant"));
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
    void mapsPackageDescriptorEnumsAcrossGrpcBoundary() {
        assertEquals(
            PackageAvailability.PACKAGE_AVAILABILITY_AVAILABLE,
            RepositoryAnalysisGrpcEndpoint.packageAvailability(
                de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.PackageAvailability.AVAILABLE
            )
        );
        assertEquals(
            PackageAvailability.PACKAGE_AVAILABILITY_UNAVAILABLE,
            RepositoryAnalysisGrpcEndpoint.packageAvailability(
                de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.PackageAvailability.UNAVAILABLE
            )
        );
        assertEquals(
            PackageAvailability.PACKAGE_AVAILABILITY_FAILED_INTEGRITY,
            RepositoryAnalysisGrpcEndpoint.packageAvailability(
                de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.PackageAvailability.FAILED_INTEGRITY
            )
        );
        assertEquals(
            de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputProducerStatus.BUILD_OUTPUT_PRODUCER_STATUS_AVAILABLE,
            RepositoryAnalysisGrpcEndpoint.buildOutputProducerStatus(
                de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.BuildOutputProducerStatus.AVAILABLE
            )
        );
        assertEquals(
            de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputProducerStatus.BUILD_OUTPUT_PRODUCER_STATUS_MISSING,
            RepositoryAnalysisGrpcEndpoint.buildOutputProducerStatus(
                de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.BuildOutputProducerStatus.MISSING
            )
        );
        assertEquals(
            de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputProducerStatus.BUILD_OUTPUT_PRODUCER_STATUS_TERMINAL_INTEGRITY_FAILURE,
            RepositoryAnalysisGrpcEndpoint.buildOutputProducerStatus(
                de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.BuildOutputProducerStatus.TERMINAL_INTEGRITY_FAILURE
            )
        );
        assertEquals(
            de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_SCOPED_OBJECT_ACCESS,
            RepositoryAnalysisGrpcEndpoint.byteCustody(
                de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.ArtifactByteCustody.SCOPED_OBJECT_ACCESS
            )
        );
        assertEquals(
            de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_EXPLICIT_HANDOFF,
            RepositoryAnalysisGrpcEndpoint.byteCustody(
                de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.ArtifactByteCustody.EXPLICIT_HANDOFF
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
        var applicationService = new RepositoryAnalysisApplicationService(
            repository,
            new FakeWorkspacePort(),
            checkoutPort,
            Clock.fixed(Instant.parse("2026-05-16T10:15:30Z"), ZoneOffset.UTC)
        );
        var handoffService = new RepositorySourceSnapshotHandoffService(
            repository,
            new FakeSourceFileCollector(),
            new FakeJavaAstPort()
        );
        server = InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(new RepositoryAnalysisGrpcEndpoint(applicationService, handoffService))
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

    private static final class FakeSourceFileCollector implements SourceSnapshotFileCollectorPort {
        @Override
        public List<SourceSnapshotSourceFile> collect(
            WorkspaceId workspaceId,
            List<SourceRoot> sourceRoots,
            de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceSnapshotHandoffPolicy policy
        ) {
            var content = "package a; class A {}";
            return List.of(new SourceSnapshotSourceFile(
                "src/main/java",
                "a/A.java",
                content,
                sha256Hex(content),
                content.getBytes(StandardCharsets.UTF_8).length
            ));
        }
    }

    private static final class FakeJavaAstPort implements JavaAstAnalysisPort {
        @Override
        public JavaAstAnalysisHandoffResult analyze(JavaAstAnalysisHandoffCommand command) {
            return new JavaAstAnalysisHandoffResult(
                command.analysisRunId(),
                command.analysisJobId(),
                command.sourceSnapshotId(),
                SourceSnapshotCompleteness.COMPLETE,
                new ArtifactReference(
                    "java-ast/snapshot-1-job-ast-1-source-facts.json",
                    "application/vnd.forensic-analytics.java-ast-source-facts.v1+json",
                    "c".repeat(64),
                    100
                ),
                "java-ast-analysis-service",
                "java-ast-analysis-v1",
                new ArtifactByteAccess(
                    "java-ast-analysis-service",
                    "java-ast-analysis.v1.JavaAstAnalysisService.GetSourceFactArtifactBytes",
                    "java-ast/snapshot-1-job-ast-1-source-facts.json",
                    ArtifactByteCustody.PRODUCER_RETAINED
                ),
                new JavaAstScanSummary(1, 1, 0, 0, 1, "JavaParser", "3.27.1"),
                List.of(),
                command.safeAttributes()
            );
        }
    }
}
