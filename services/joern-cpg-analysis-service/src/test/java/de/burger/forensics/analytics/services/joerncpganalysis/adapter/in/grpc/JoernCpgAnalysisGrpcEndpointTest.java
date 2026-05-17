package de.burger.forensics.analytics.services.joerncpganalysis.adapter.in.grpc;

import de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobId;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisRunId;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind;
import de.burger.forensics.analytics.analysisjob.v1.SourceSnapshotId;
import de.burger.forensics.analytics.joerncpganalysis.v1.AnalyzeJoernCpgRequest;
import de.burger.forensics.analytics.joerncpganalysis.v1.JoernCpgAnalysisServiceGrpc;
import de.burger.forensics.analytics.joerncpganalysis.v1.JoernCpgPolicy;
import de.burger.forensics.analytics.joerncpganalysis.v1.SourceRoot;
import de.burger.forensics.analytics.joerncpganalysis.v1.SourceWorkspace;
import de.burger.forensics.analytics.services.joerncpganalysis.application.JoernCpgAnalysisApplicationService;
import de.burger.forensics.analytics.services.joerncpganalysis.application.JoernCpgAnalysisTimeoutException;
import de.burger.forensics.analytics.services.joerncpganalysis.application.JoernCpgArtifactException;
import de.burger.forensics.analytics.services.joerncpganalysis.application.JoernRuntimeUnavailableException;
import de.burger.forensics.analytics.services.joerncpganalysis.application.port.ResolvedJoernWorkspace;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.ArtifactReference;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.JoernArtifactCollectionResult;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.JoernCpgDiagnostic;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.JoernRuntimeResult;
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

import static de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.PRODUCER_SERVICE;
import static de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.SEMANTIC_ARTIFACT_SCHEMA_VERSION;
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

        stopServer();
        startServer(applicationService(command -> {
            throw new JoernRuntimeUnavailableException("Joern unavailable");
        }));
        var unavailable = assertThrows(
            StatusRuntimeException.class,
            () -> stub.analyzeSourceSnapshot(request(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_JOERN_ANALYSIS))
        );
        assertEquals(Status.Code.UNAVAILABLE, unavailable.getStatus().getCode());

        stopServer();
        startServer(applicationService(command -> {
            throw new JoernCpgAnalysisTimeoutException("timeout");
        }));
        var timeout = assertThrows(
            StatusRuntimeException.class,
            () -> stub.analyzeSourceSnapshot(request(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_JOERN_ANALYSIS))
        );
        assertEquals(Status.Code.DEADLINE_EXCEEDED, timeout.getStatus().getCode());

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
            throw new IllegalStateException("unexpected");
        }));
        var unexpected = assertThrows(
            StatusRuntimeException.class,
            () -> stub.analyzeSourceSnapshot(request(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_JOERN_ANALYSIS))
        );
        assertEquals(Status.Code.FAILED_PRECONDITION, unexpected.getStatus().getCode());
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
            command -> new ResolvedJoernWorkspace(
                command.metadata().sourceSnapshotId(),
                "workspace-1",
                Path.of("build/test-workspace"),
                List.of(Path.of("build/test-workspace/src/main/java")),
                100
            ),
            (command, workspace) -> new JoernRuntimeResult("joern-test-1", image(), "joern-cpg/run-1", List.of()),
            (command, runtimeResult) -> collector.collect(command)
        );
    }

    private static AnalysisArtifactReference reference(String path) {
        return new AnalysisArtifactReference(
            new ArtifactReference(path, "application/vnd.forensic-analytics.joern-cpg.v1+binary", "a".repeat(64), 3),
            de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisArtifactCategory.STATIC,
            PRODUCER_SERVICE,
            SEMANTIC_ARTIFACT_SCHEMA_VERSION,
            de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisCompleteness.COMPLETE
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
            .setWorkerVersion("joern-cpg-analysis-service-test")
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
                .setWorkspaceId("workspace-1")
                .addSourceRoots(SourceRoot.newBuilder()
                    .setRelativePath("src/main/java")
                    .setLanguage("java"))
                .addInputArtifacts(inputArtifact("source-a.json", AnalysisCompleteness.ANALYSIS_COMPLETENESS_COMPLETE))
                .addInputArtifacts(inputArtifact("source-b.json", AnalysisCompleteness.ANALYSIS_COMPLETENESS_INCOMPLETE))
                .addInputArtifacts(inputArtifact("source-c.json", AnalysisCompleteness.ANALYSIS_COMPLETENESS_UNKNOWN)))
            .putSafeAttributes("tenant", "demo")
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
            .build();
    }

    @FunctionalInterface
    private interface Collector {
        JoernArtifactCollectionResult collect(
            de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalyzeJoernCpgCommand command
        );
    }
}
