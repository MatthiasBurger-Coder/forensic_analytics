package de.burger.forensics.analytics.services.joernanalysis.adapter.in.grpc;

import de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobId;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisRunId;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind;
import de.burger.forensics.analytics.analysisjob.v1.ArtifactByteAccess;
import de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody;
import de.burger.forensics.analytics.analysisjob.v1.SourceSnapshotId;
import de.burger.forensics.analytics.joerncpganalysis.v1.AnalyzeJoernCpgRequest;
import de.burger.forensics.analytics.joerncpganalysis.v1.JoernMaterializationPolicy;
import de.burger.forensics.analytics.joerncpganalysis.v1.JoernCpgAnalysisServiceGrpc;
import de.burger.forensics.analytics.joerncpganalysis.v1.JoernCpgPolicy;
import de.burger.forensics.analytics.joerncpganalysis.v1.MaterializeJoernWorkspaceRequest;
import de.burger.forensics.analytics.joerncpganalysis.v1.SourceRoot;
import de.burger.forensics.analytics.joerncpganalysis.v1.SourceWorkspace;
import de.burger.forensics.analytics.services.joernanalysis.application.JoernCpgAnalysisApplicationService;
import de.burger.forensics.analytics.services.joernanalysis.application.JoernCpgAnalysisTimeoutException;
import de.burger.forensics.analytics.services.joernanalysis.application.JoernCpgArtifactException;
import de.burger.forensics.analytics.services.joernanalysis.application.JoernRuntimeUnavailableException;
import de.burger.forensics.analytics.services.joernanalysis.application.port.JoernArtifactCollectorPort;
import de.burger.forensics.analytics.services.joernanalysis.application.port.ResolvedJoernWorkspace;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.ArtifactReference;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.JoernArtifactCollectionResult;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.JoernCpgDiagnostic;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.JoernRuntimeResult;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.PRODUCER_SERVICE;
import static de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.SEMANTIC_ARTIFACT_SCHEMA_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JoernCpgAnalysisGrpcEndpointTest {
    private Server server;
    private ManagedChannel channel;
    private JoernCpgAnalysisServiceGrpc.JoernCpgAnalysisServiceBlockingStub stub;

    @AfterEach
    void stopServer() {
        if (channel != null) {
            channel.shutdownNow();
        }
        if (server != null) {
            server.shutdownNow();
        }
    }

    @Test
    void materializesSourceSnapshotThroughGrpcBoundary() throws Exception {
        startServer(applicationService(new JoernArtifactCollectionResult(List.of(reference("joern-cpg/run-1/cpg.bin.zip")), 0, List.of())));

        var response = stub.materializeSourceSnapshot(materializeRequest());

        assertEquals("MATERIALIZED", response.getStatus().getCode());
        assertEquals("joern-workspace-snapshot-1", response.getWorkspace().getWorkspaceId());
        assertEquals(2, response.getWorkspace().getInputArtifactsCount());
        assertEquals(
            List.of("build-artifact-worker-service", "repository-analysis-service"),
            response.getWorkspace().getInputArtifactsList().stream()
                .map(artifact -> artifact.getByteAccess().getOwnerService())
                .toList()
        );
        assertEquals(
            List.of("build-artifact-worker.v1.BuildOutputPackage", "repository-analysis.v1.GetRepositoryPreparation"),
            response.getWorkspace().getInputArtifactsList().stream()
                .map(artifact -> artifact.getByteAccess().getRetrievalContract())
                .toList()
        );
        assertEquals(
            List.of("source-snapshot/snapshot-1/build-output.zip", "source-snapshot/snapshot-1/source.zip"),
            response.getWorkspace().getInputArtifactsList().stream()
                .map(artifact -> artifact.getByteAccess().getRetrievalReference())
                .toList()
        );
        assertEquals(
            List.of(
                ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_PRODUCER_RETAINED,
                ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_PRODUCER_RETAINED
            ),
            response.getWorkspace().getInputArtifactsList().stream()
                .map(artifact -> artifact.getByteAccess().getByteCustody())
                .toList()
        );
        assertEquals("demo", response.getSafeAttributesOrThrow("tenant"));
    }

    @Test
    void rejectsPendingOrPrivatePackageMaterializationRequests() throws Exception {
        startServer(applicationService(new JoernArtifactCollectionResult(List.of(reference("joern-cpg/run-1/cpg.bin.zip")), 0, List.of())));

        var pending = assertThrows(
            StatusRuntimeException.class,
            () -> stub.materializeSourceSnapshot(materializeRequest().toBuilder()
                .setSourcePackage(sourcePackage(
                    de.burger.forensics.analytics.repositoryanalysis.v1.PackageAvailability.PACKAGE_AVAILABILITY_PENDING,
                    "source-snapshot/snapshot-1/source.zip"
                ))
                .build())
        );
        var privateReference = assertThrows(
            StatusRuntimeException.class,
            () -> stub.materializeSourceSnapshot(materializeRequest().toBuilder()
                .setBuildOutputPackage(buildOutputPackage(
                    de.burger.forensics.analytics.repositoryanalysis.v1.PackageAvailability.PACKAGE_AVAILABILITY_AVAILABLE,
                    "file:/tmp/private/build-output.zip"
                ))
                .build())
        );

        assertEquals(Status.Code.INVALID_ARGUMENT, pending.getStatus().getCode());
        assertEquals(Status.Code.INVALID_ARGUMENT, privateReference.getStatus().getCode());
    }

    @Test
    void analyzesSourceSnapshotThroughGrpcBoundary() throws Exception {
        startServer(applicationService(new JoernArtifactCollectionResult(
            List.of(reference("joern-cpg/run-1/cpg.bin.zip")),
            0,
            List.of()
        )));

        var response = stub.analyzeSourceSnapshot(request(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_JOERN_ANALYSIS));

        assertEquals("ANALYZED", response.getStatus().getCode());
        assertEquals(AnalysisCompleteness.ANALYSIS_COMPLETENESS_COMPLETE, response.getCompleteness());
        assertEquals("snapshot-1", response.getSourceSnapshotId().getValue());
        assertEquals(AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_STATIC, response.getSemanticArtifacts(0).getCategory());
        assertEquals(PRODUCER_SERVICE, response.getSemanticArtifacts(0).getByteAccess().getOwnerService());
        assertEquals(PRODUCER_SERVICE, response.getSummary().getProducerService());
        assertEquals("demo", response.getSafeAttributesOrThrow("tenant"));
    }

    @Test
    void keepsIncompleteStaticSemanticArtifactsExplicit() throws Exception {
        startServer(applicationService(command -> new JoernArtifactCollectionResult(
            List.of(reference("joern-cpg/run-1/cpg.bin.zip")),
            1,
            List.of(
                JoernCpgDiagnostic.info(command.metadata().sourceSnapshotId(), "JOERN_OPTIONAL_QUERY_SKIPPED", "optional script skipped"),
                JoernCpgDiagnostic.warning(
                    command.metadata().sourceSnapshotId(),
                    "JOERN_ARTIFACT_MISSING",
                    "callgraph missing",
                    "callgraph.json",
                    true
                ),
                JoernCpgDiagnostic.error(command.metadata().sourceSnapshotId(), "JOERN_QUERY_FAILED", "query failed", true)
            )
        )));

        var response = stub.analyzeSourceSnapshot(request(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_JOERN_ANALYSIS));

        assertEquals("ANALYZED_INCOMPLETE", response.getStatus().getCode());
        assertEquals(AnalysisCompleteness.ANALYSIS_COMPLETENESS_INCOMPLETE, response.getCompleteness());
        assertEquals(List.of("JOERN_ARTIFACT_MISSING", "JOERN_OPTIONAL_QUERY_SKIPPED", "JOERN_QUERY_FAILED"), response.getDiagnosticsList().stream()
            .map(diagnostic -> diagnostic.getCode())
            .toList());
        assertEquals(1, response.getSummary().getMissingArtifactCount());
    }

    @Test
    void mapsMissingArtifactsToUnknownCompleteness() throws Exception {
        startServer(applicationService(new JoernArtifactCollectionResult(List.of(), 0, List.of())));

        var response = stub.analyzeSourceSnapshot(request(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_JOERN_ANALYSIS));

        assertEquals("ANALYZED_INCOMPLETE", response.getStatus().getCode());
        assertEquals(AnalysisCompleteness.ANALYSIS_COMPLETENESS_UNKNOWN, response.getCompleteness());
    }

    @Test
    void mapsInvalidInputAndRuntimeFailuresToGrpcStatuses() throws Exception {
        startServer(applicationService(new JoernArtifactCollectionResult(List.of(reference("joern-cpg/run-1/cpg.bin.zip")), 0, List.of())));

        var invalid = assertThrows(
            StatusRuntimeException.class,
            () -> stub.analyzeSourceSnapshot(request(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS))
        );
        assertEquals(Status.Code.INVALID_ARGUMENT, invalid.getStatus().getCode());

        var noPolicy = assertThrows(
            StatusRuntimeException.class,
            () -> stub.analyzeSourceSnapshot(request(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_JOERN_ANALYSIS).toBuilder()
                .clearPolicy()
                .build())
        );
        assertEquals(Status.Code.INVALID_ARGUMENT, noPolicy.getStatus().getCode());

        var noWorkspace = assertThrows(
            StatusRuntimeException.class,
            () -> stub.analyzeSourceSnapshot(request(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_JOERN_ANALYSIS).toBuilder()
                .clearWorkspace()
                .build())
        );
        assertEquals(Status.Code.INVALID_ARGUMENT, noWorkspace.getStatus().getCode());

        var wrongArtifactCategory = assertThrows(
            StatusRuntimeException.class,
            () -> stub.analyzeSourceSnapshot(request(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_JOERN_ANALYSIS).toBuilder()
                .setWorkspace(SourceWorkspace.newBuilder()
                    .setWorkspaceId("joern-workspace-snapshot-1")
                    .addSourceRoots(SourceRoot.newBuilder().setRelativePath("src/main/java").setLanguage("java"))
                    .addInputArtifacts(inputArtifact("source-generated.json", AnalysisCompleteness.ANALYSIS_COMPLETENESS_COMPLETE).toBuilder()
                        .setCategory(AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_GENERATED)))
                .build())
        );
        assertEquals(Status.Code.INVALID_ARGUMENT, wrongArtifactCategory.getStatus().getCode());

        var mismatchedWorkspace = assertThrows(
            StatusRuntimeException.class,
            () -> stub.analyzeSourceSnapshot(request(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_JOERN_ANALYSIS).toBuilder()
                .setWorkspace(request(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_JOERN_ANALYSIS).getWorkspace().toBuilder()
                    .setWorkspaceId("joern-workspace-other-snapshot"))
                .build())
        );
        assertEquals(Status.Code.INVALID_ARGUMENT, mismatchedWorkspace.getStatus().getCode());

        stopServer();
        startServer(applicationServiceWithRuntimeFailure(new JoernRuntimeUnavailableException("Joern unavailable")));
        var unavailable = stub.analyzeSourceSnapshot(request(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_JOERN_ANALYSIS));
        assertEquals("ANALYZED_INCOMPLETE", unavailable.getStatus().getCode());
        assertEquals(AnalysisCompleteness.ANALYSIS_COMPLETENESS_UNKNOWN, unavailable.getCompleteness());
        assertEquals(List.of("JOERN_RUNTIME_UNAVAILABLE"), unavailable.getDiagnosticsList().stream()
            .map(diagnostic -> diagnostic.getCode())
            .toList());
        assertEquals("joern-cpg/run-1/joern-provenance.json", unavailable.getSemanticArtifacts(0).getArtifact().getPath());
        assertEquals(AnalysisCompleteness.ANALYSIS_COMPLETENESS_UNKNOWN, unavailable.getSemanticArtifacts(0).getCompleteness());

        stopServer();
        startServer(applicationServiceWithRuntimeFailure(new JoernCpgAnalysisTimeoutException("timeout")));
        var timeout = stub.analyzeSourceSnapshot(request(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_JOERN_ANALYSIS));
        assertEquals("ANALYZED_INCOMPLETE", timeout.getStatus().getCode());
        assertEquals(AnalysisCompleteness.ANALYSIS_COMPLETENESS_UNKNOWN, timeout.getCompleteness());
        assertEquals(List.of("JOERN_RUNTIME_TIMEOUT"), timeout.getDiagnosticsList().stream()
            .map(diagnostic -> diagnostic.getCode())
            .toList());

        stopServer();
        startServer(applicationService(command -> {
            throw new JoernCpgArtifactException("artifact failure");
        }));
        var artifactFailure = assertThrows(
            StatusRuntimeException.class,
            () -> stub.analyzeSourceSnapshot(request(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_JOERN_ANALYSIS))
        );
        assertEquals(Status.Code.FAILED_PRECONDITION, artifactFailure.getStatus().getCode());

        stopServer();
        startServer(applicationService(command -> {
            throw new JoernRuntimeUnavailableException("Joern unavailable after runtime handoff");
        }));
        var unavailableTransport = assertThrows(
            StatusRuntimeException.class,
            () -> stub.analyzeSourceSnapshot(request(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_JOERN_ANALYSIS))
        );
        assertEquals(Status.Code.UNAVAILABLE, unavailableTransport.getStatus().getCode());

        stopServer();
        startServer(applicationService(command -> {
            throw new JoernCpgAnalysisTimeoutException("timeout after runtime handoff");
        }));
        var timeoutTransport = assertThrows(
            StatusRuntimeException.class,
            () -> stub.analyzeSourceSnapshot(request(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_JOERN_ANALYSIS))
        );
        assertEquals(Status.Code.DEADLINE_EXCEEDED, timeoutTransport.getStatus().getCode());

        stopServer();
        startServer(applicationService(command -> {
            throw new IllegalStateException("unexpected");
        }));
        var unexpected = assertThrows(
            StatusRuntimeException.class,
            () -> stub.analyzeSourceSnapshot(request(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_JOERN_ANALYSIS))
        );
        assertEquals(Status.Code.FAILED_PRECONDITION, unexpected.getStatus().getCode());
    }

    @Test
    void rejectsIncompleteMaterializationRequestsAtGrpcBoundary() throws Exception {
        startServer(applicationService(new JoernArtifactCollectionResult(List.of(reference("joern-cpg/run-1/cpg.bin.zip")), 0, List.of())));

        assertEquals(Status.Code.INVALID_ARGUMENT, assertThrows(
            StatusRuntimeException.class,
            () -> stub.materializeSourceSnapshot(materializeRequest().toBuilder().clearAnalysisRunId().build())
        ).getStatus().getCode());
        assertEquals(Status.Code.INVALID_ARGUMENT, assertThrows(
            StatusRuntimeException.class,
            () -> stub.materializeSourceSnapshot(materializeRequest().toBuilder().clearAnalysisJobId().build())
        ).getStatus().getCode());
        assertEquals(Status.Code.INVALID_ARGUMENT, assertThrows(
            StatusRuntimeException.class,
            () -> stub.materializeSourceSnapshot(materializeRequest().toBuilder().clearSourceSnapshotId().build())
        ).getStatus().getCode());
        assertEquals(Status.Code.INVALID_ARGUMENT, assertThrows(
            StatusRuntimeException.class,
            () -> stub.materializeSourceSnapshot(materializeRequest().toBuilder().clearSourceRoots().build())
        ).getStatus().getCode());
        assertEquals(Status.Code.INVALID_ARGUMENT, assertThrows(
            StatusRuntimeException.class,
            () -> stub.materializeSourceSnapshot(materializeRequest().toBuilder().clearSourcePackage().build())
        ).getStatus().getCode());
        assertEquals(Status.Code.INVALID_ARGUMENT, assertThrows(
            StatusRuntimeException.class,
            () -> stub.materializeSourceSnapshot(materializeRequest().toBuilder().clearBuildOutputPackage().build())
        ).getStatus().getCode());
        assertEquals(Status.Code.INVALID_ARGUMENT, assertThrows(
            StatusRuntimeException.class,
            () -> stub.materializeSourceSnapshot(materializeRequest().toBuilder().clearPolicy().build())
        ).getStatus().getCode());
        assertEquals(Status.Code.INVALID_ARGUMENT, assertThrows(
            StatusRuntimeException.class,
            () -> stub.materializeSourceSnapshot(materializeRequest().toBuilder()
                .setSourcePackage(sourcePackage(
                    de.burger.forensics.analytics.repositoryanalysis.v1.PackageAvailability.PACKAGE_AVAILABILITY_PENDING,
                    "source-snapshot/snapshot-1/source.zip"
                ).toBuilder()
                    .setAvailability(de.burger.forensics.analytics.repositoryanalysis.v1.PackageAvailability.PACKAGE_AVAILABILITY_UNSPECIFIED))
                .build())
        ).getStatus().getCode());
        assertEquals(Status.Code.INVALID_ARGUMENT, assertThrows(
            StatusRuntimeException.class,
            () -> stub.materializeSourceSnapshot(materializeRequest().toBuilder()
                .setBuildOutputPackage(buildOutputPackage(
                    de.burger.forensics.analytics.repositoryanalysis.v1.PackageAvailability.PACKAGE_AVAILABILITY_PENDING,
                    "source-snapshot/snapshot-1/build-output.zip"
                ))
                .build())
        ).getStatus().getCode());
        assertEquals(Status.Code.INVALID_ARGUMENT, assertThrows(
            StatusRuntimeException.class,
            () -> stub.materializeSourceSnapshot(materializeRequest().toBuilder()
                .setSourcePackage(sourcePackage(
                    de.burger.forensics.analytics.repositoryanalysis.v1.PackageAvailability.PACKAGE_AVAILABILITY_UNAVAILABLE,
                    "source-snapshot/snapshot-1/source.zip"
                ))
                .build())
        ).getStatus().getCode());
        assertEquals(Status.Code.INVALID_ARGUMENT, assertThrows(
            StatusRuntimeException.class,
            () -> stub.materializeSourceSnapshot(materializeRequest().toBuilder()
                .setSourcePackage(sourcePackage(
                    de.burger.forensics.analytics.repositoryanalysis.v1.PackageAvailability.PACKAGE_AVAILABILITY_FAILED_INTEGRITY,
                    "source-snapshot/snapshot-1/source.zip"
                ))
                .build())
        ).getStatus().getCode());
        assertEquals(Status.Code.INVALID_ARGUMENT, assertThrows(
            StatusRuntimeException.class,
            () -> stub.materializeSourceSnapshot(materializeRequest().toBuilder()
                .setBuildOutputPackage(buildOutputPackage(
                    de.burger.forensics.analytics.repositoryanalysis.v1.PackageAvailability.PACKAGE_AVAILABILITY_AVAILABLE,
                    "source-snapshot/snapshot-1/build-output.zip"
                ).toBuilder()
                    .setByteAccess(byteAccess(
                        "build-artifact-worker-service",
                        "build-artifact-worker.v1.BuildOutputPackage",
                        "source-snapshot/snapshot-1/build-output.zip"
                    ).toBuilder()
                        .setByteCustody(ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_UNSPECIFIED)))
                .build())
        ).getStatus().getCode());

        assertEquals(Status.Code.INVALID_ARGUMENT, assertThrows(
            StatusRuntimeException.class,
            () -> stub.materializeSourceSnapshot(materializeRequest().toBuilder()
                .setSourcePackage(sourcePackage(
                    de.burger.forensics.analytics.repositoryanalysis.v1.PackageAvailability.PACKAGE_AVAILABILITY_AVAILABLE,
                    "source-snapshot/snapshot-1/source.zip"
                ).toBuilder()
                    .setCompleteness(de.burger.forensics.analytics.repositoryanalysis.v1.SourceSnapshotCompleteness.SOURCE_SNAPSHOT_COMPLETENESS_INCOMPLETE))
                .build())
        ).getStatus().getCode());
        assertEquals(Status.Code.INVALID_ARGUMENT, assertThrows(
            StatusRuntimeException.class,
            () -> stub.materializeSourceSnapshot(materializeRequest().toBuilder()
                .setBuildOutputPackage(buildOutputPackage(
                    de.burger.forensics.analytics.repositoryanalysis.v1.PackageAvailability.PACKAGE_AVAILABILITY_AVAILABLE,
                    "source-snapshot/snapshot-1/build-output.zip"
                ).toBuilder()
                    .setCompleteness(de.burger.forensics.analytics.repositoryanalysis.v1.SourceSnapshotCompleteness.SOURCE_SNAPSHOT_COMPLETENESS_UNKNOWN))
                .build())
        ).getStatus().getCode());
    }

    private void startServer(JoernCpgAnalysisApplicationService applicationService) throws Exception {
        var serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(new JoernCpgAnalysisGrpcEndpoint(applicationService))
            .build()
            .start();
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        stub = JoernCpgAnalysisServiceGrpc.newBlockingStub(channel);
    }

    private static JoernCpgAnalysisApplicationService applicationService(JoernArtifactCollectionResult result) {
        return applicationService(command -> result);
    }

    private static JoernCpgAnalysisApplicationService applicationService(Collector collector) {
        return new JoernCpgAnalysisApplicationService(
            command -> new de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.SourceWorkspace(
                "joern-workspace-" + command.metadata().sourceSnapshotId().value(),
                command.sourceRoots(),
                List.of(materializedInput(command.sourcePackage()), materializedInput(command.buildOutputPackage()))
            ),
            command -> new ResolvedJoernWorkspace(
                command.metadata().sourceSnapshotId(),
                "joern-workspace-" + command.metadata().sourceSnapshotId().value(),
                Path.of("build/test-workspace"),
                List.of(Path.of("build/test-workspace/src/main/java")),
                100
            ),
            (command, workspace) -> new JoernRuntimeResult("joern-test-1", image(), "joern-cpg/run-1", List.of()),
            (command, runtimeResult) -> collector.collect(command),
            result -> {
            }
        );
    }

    private static JoernCpgAnalysisApplicationService applicationServiceWithRuntimeFailure(RuntimeException failure) {
        return new JoernCpgAnalysisApplicationService(
            command -> new de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.SourceWorkspace(
                "joern-workspace-" + command.metadata().sourceSnapshotId().value(),
                command.sourceRoots(),
                List.of(materializedInput(command.sourcePackage()), materializedInput(command.buildOutputPackage()))
            ),
            command -> new ResolvedJoernWorkspace(
                command.metadata().sourceSnapshotId(),
                "joern-workspace-" + command.metadata().sourceSnapshotId().value(),
                Path.of("build/test-workspace"),
                List.of(Path.of("build/test-workspace/src/main/java")),
                100
            ),
            (command, workspace) -> {
                throw failure;
            },
            new JoernArtifactCollectorPort() {
                @Override
                public JoernArtifactCollectionResult collect(
                    de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalyzeJoernCpgCommand command,
                    JoernRuntimeResult runtimeResult
                ) {
                    return new JoernArtifactCollectionResult(List.of(reference("joern-cpg/run-1/cpg.bin.zip")), 0, List.of());
                }

                @Override
                public JoernArtifactCollectionResult collectUnavailable(
                    de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalyzeJoernCpgCommand command,
                    ResolvedJoernWorkspace workspace,
                    JoernCpgDiagnostic diagnostic
                ) {
                    return new JoernArtifactCollectionResult(
                        List.of(reference(
                            "joern-cpg/run-1/joern-provenance.json",
                            de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalysisCompleteness.UNKNOWN
                        )),
                        5,
                        List.of(diagnostic)
                    );
                }
            },
            result -> {
            }
        );
    }

    private static AnalysisArtifactReference materializedInput(
        de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.MaterializedPackageDescriptor descriptor
    ) {
        return new AnalysisArtifactReference(
            descriptor.packageArtifact(),
            de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalysisArtifactCategory.STATIC,
            descriptor.producerService(),
            descriptor.schemaVersion(),
            descriptor.completeness(),
            descriptor.byteAccess()
        );
    }

    private static AnalysisArtifactReference reference(String path) {
        return reference(
            path,
            de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalysisCompleteness.COMPLETE
        );
    }

    private static AnalysisArtifactReference reference(
        String path,
        de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalysisCompleteness completeness
    ) {
        return new AnalysisArtifactReference(
            new ArtifactReference(path, "application/vnd.forensic-analytics.joern-cpg.v1+binary", "a".repeat(64), 3),
            de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalysisArtifactCategory.STATIC,
            PRODUCER_SERVICE,
            SEMANTIC_ARTIFACT_SCHEMA_VERSION,
            completeness,
            new de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.ArtifactByteAccess(
                PRODUCER_SERVICE,
                "analysis-job.v1.ArtifactBytes",
                "artifacts/" + path,
                de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.ArtifactByteCustody.PRODUCER_RETAINED
            )
        );
    }

    private static AnalyzeJoernCpgRequest request(AnalysisWorkerKind workerKind) {
        return AnalyzeJoernCpgRequest.newBuilder()
            .setRequestId("request-1")
            .setIdempotencyKey("idempotency-1")
            .setSchemaVersion("joern-cpg-analysis-v1")
            .setCorrelationId("correlation-1")
            .setWorkerKind(workerKind)
            .setAnalysisRunId(AnalysisRunId.newBuilder().setValue("run-1"))
            .setAnalysisJobId(AnalysisJobId.newBuilder().setValue("job-1"))
            .setSourceSnapshotId(SourceSnapshotId.newBuilder().setValue("snapshot-1"))
            .setWorkerVersion("joern-analysis-service-test")
            .setPolicy(JoernCpgPolicy.newBuilder()
                .setMaxSourceRoots(2)
                .setMaxWorkspaceBytes(1_000_000)
                .setMaxArtifactBytes(1_000_000)
                .setTimeoutSeconds(60)
                .setJoernImageReference(image())
                .setQueryBundleVersion("queries-v1")
                .setRequireCallgraph(true)
                .setRequireControlflow(true)
                .setRequireDataflow(true))
            .setWorkspace(SourceWorkspace.newBuilder()
                .setWorkspaceId("joern-workspace-snapshot-1")
                .addSourceRoots(SourceRoot.newBuilder()
                    .setRelativePath("src/main/java")
                    .setLanguage("java"))
                .addInputArtifacts(inputArtifact("source-a.json", AnalysisCompleteness.ANALYSIS_COMPLETENESS_COMPLETE))
                .addInputArtifacts(inputArtifact("source-b.json", AnalysisCompleteness.ANALYSIS_COMPLETENESS_INCOMPLETE))
                .addInputArtifacts(inputArtifact("source-c.json", AnalysisCompleteness.ANALYSIS_COMPLETENESS_UNKNOWN)))
            .putSafeAttributes("tenant", "demo")
            .build();
    }

    private static MaterializeJoernWorkspaceRequest materializeRequest() {
        return MaterializeJoernWorkspaceRequest.newBuilder()
            .setRequestId("request-materialize")
            .setIdempotencyKey("materialize-1")
            .setSchemaVersion("joern-materialization-v1")
            .setCorrelationId("correlation-1")
            .setAnalysisRunId(AnalysisRunId.newBuilder().setValue("run-1"))
            .setAnalysisJobId(AnalysisJobId.newBuilder().setValue("job-1"))
            .setSourceSnapshotId(SourceSnapshotId.newBuilder().setValue("snapshot-1"))
            .addSourceRoots(SourceRoot.newBuilder()
                .setRelativePath("src/main/java")
                .setLanguage("java"))
            .setSourcePackage(sourcePackage(
                de.burger.forensics.analytics.repositoryanalysis.v1.PackageAvailability.PACKAGE_AVAILABILITY_AVAILABLE,
                "source-snapshot/snapshot-1/source.zip"
            ))
            .setBuildOutputPackage(buildOutputPackage(
                de.burger.forensics.analytics.repositoryanalysis.v1.PackageAvailability.PACKAGE_AVAILABILITY_AVAILABLE,
                "source-snapshot/snapshot-1/build-output.zip"
            ))
            .setPolicy(JoernMaterializationPolicy.newBuilder()
                .setMaxSourceRoots(2)
                .setMaxWorkspaceBytes(1_000_000)
                .setMaxArtifactBytes(1_000_000)
                .setMaxArchiveDepth(20)
                .setRejectSymlinks(true)
                .setRejectHardlinks(true)
                .setRejectDeviceFiles(true)
                .setRejectDuplicatePaths(true))
            .putSafeAttributes("tenant", "demo")
            .build();
    }

    private static de.burger.forensics.analytics.repositoryanalysis.v1.SourcePackageDescriptor sourcePackage(
        de.burger.forensics.analytics.repositoryanalysis.v1.PackageAvailability availability,
        String reference
    ) {
        var builder = de.burger.forensics.analytics.repositoryanalysis.v1.SourcePackageDescriptor.newBuilder()
            .setAvailability(availability)
            .setManifestArtifact(repositoryArtifact("source-snapshot/snapshot-1/source-manifest.json", "application/json"))
            .setSchemaVersion("source-package-descriptor-v1")
            .setProducerService("repository-analysis-service")
            .setCompleteness(de.burger.forensics.analytics.repositoryanalysis.v1.SourceSnapshotCompleteness.SOURCE_SNAPSHOT_COMPLETENESS_COMPLETE)
            .setByteAccess(byteAccess("repository-analysis-service", "repository-analysis.v1.GetRepositoryPreparation", reference));
        if (availability != de.burger.forensics.analytics.repositoryanalysis.v1.PackageAvailability.PACKAGE_AVAILABILITY_PENDING) {
            builder.setPackageArtifact(repositoryArtifact(reference, "application/zip"));
        }
        return builder.build();
    }

    private static de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputPackageDescriptor buildOutputPackage(
        de.burger.forensics.analytics.repositoryanalysis.v1.PackageAvailability availability,
        String reference
    ) {
        var builder = de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputPackageDescriptor.newBuilder()
            .setAvailability(availability)
            .setManifestArtifact(repositoryArtifact("source-snapshot/snapshot-1/build-output-manifest.json", "application/json"))
            .setSchemaVersion("build-output-package-descriptor-v1")
            .setProducerService("build-artifact-worker-service")
            .setCompleteness(de.burger.forensics.analytics.repositoryanalysis.v1.SourceSnapshotCompleteness.SOURCE_SNAPSHOT_COMPLETENESS_COMPLETE)
            .setByteAccess(byteAccess("build-artifact-worker-service", "build-artifact-worker.v1.BuildOutputPackage", reference));
        if (availability != de.burger.forensics.analytics.repositoryanalysis.v1.PackageAvailability.PACKAGE_AVAILABILITY_PENDING) {
            builder.setPackageArtifact(repositoryArtifact(reference, "application/zip"));
        }
        return builder.build();
    }

    private static de.burger.forensics.analytics.repositoryanalysis.v1.ArtifactReference repositoryArtifact(String reference, String type) {
        return de.burger.forensics.analytics.repositoryanalysis.v1.ArtifactReference.newBuilder()
            .setReference(reference)
            .setType(type)
            .setSha256("a".repeat(64))
            .setSizeBytes(42)
            .build();
    }

    private static ArtifactByteAccess byteAccess(String ownerService, String retrievalContract, String retrievalReference) {
        return ArtifactByteAccess.newBuilder()
            .setOwnerService(ownerService)
            .setRetrievalContract(retrievalContract)
            .setRetrievalReference(retrievalReference)
            .setByteCustody(ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_PRODUCER_RETAINED)
            .build();
    }

    private static String image() {
        return "ghcr.io/joernio/joern@sha256:" + "a".repeat(64);
    }

    private static de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactReference inputArtifact(
        String path,
        AnalysisCompleteness completeness
    ) {
        return de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactReference.newBuilder()
            .setArtifact(de.burger.forensics.analytics.analysisjob.v1.ArtifactReference.newBuilder()
                .setPath(path)
                .setType("application/json")
                .setSha256("a".repeat(64))
                .setSizeBytes(1))
            .setCategory(AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_STATIC)
            .setProducerService("repository-analysis-service")
            .setSchemaVersion("source-artifact-v1")
            .setCompleteness(completeness)
            .setByteAccess(ArtifactByteAccess.newBuilder()
                .setOwnerService("repository-analysis-service")
                .setRetrievalContract("repository-analysis.v1.SourcePackage")
                .setRetrievalReference("source-snapshot/snapshot-1/" + path)
                .setByteCustody(ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_PRODUCER_RETAINED))
            .build();
    }

    @FunctionalInterface
    private interface Collector {
        JoernArtifactCollectionResult collect(
            de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalyzeJoernCpgCommand command
        );
    }
}
