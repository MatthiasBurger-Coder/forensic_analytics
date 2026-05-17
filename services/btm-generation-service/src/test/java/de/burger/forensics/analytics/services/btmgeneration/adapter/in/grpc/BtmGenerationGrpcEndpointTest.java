package de.burger.forensics.analytics.services.btmgeneration.adapter.in.grpc;

import de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactReference;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobId;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisRunId;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind;
import de.burger.forensics.analytics.analysisjob.v1.ArtifactReference;
import de.burger.forensics.analytics.analysisjob.v1.SourceSnapshotId;
import de.burger.forensics.analytics.btmgeneration.v1.BtmGenerationPolicy;
import de.burger.forensics.analytics.btmgeneration.v1.BtmGenerationServiceGrpc;
import de.burger.forensics.analytics.btmgeneration.v1.DeliveredAnalysisFacts;
import de.burger.forensics.analytics.btmgeneration.v1.GenerateBtmRulesRequest;
import de.burger.forensics.analytics.btmgeneration.v1.InstrumentationTarget;
import de.burger.forensics.analytics.btmgeneration.v1.ProbeKind;
import de.burger.forensics.analytics.services.btmgeneration.adapter.out.filesystem.FileSystemBtmArtifactWriter;
import de.burger.forensics.analytics.services.btmgeneration.application.BtmArtifactException;
import de.burger.forensics.analytics.services.btmgeneration.application.BtmGenerationApplicationService;
import de.burger.forensics.analytics.services.btmgeneration.application.port.BtmArtifactWriterPort;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.GeneratedBtmArtifacts;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BtmGenerationGrpcEndpointTest {
    @TempDir
    Path tempDir;

    private Server server;
    private ManagedChannel channel;
    private BtmGenerationServiceGrpc.BtmGenerationServiceBlockingStub stub;

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
    void generatesBtmRulesThroughGrpcBoundary() throws Exception {
        startServer(new FileSystemBtmArtifactWriter(tempDir));

        var response = stub.generateBtmRules(request(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_BTM_GENERATION));

        assertEquals("GENERATED", response.getStatus().getCode());
        assertEquals("correlation-1", response.getStatus().getCorrelationId());
        assertEquals(AnalysisCompleteness.ANALYSIS_COMPLETENESS_COMPLETE, response.getCompleteness());
        assertEquals(2, response.getGeneratedArtifactsCount());
        assertEquals(AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_GENERATED, response.getGeneratedArtifacts(0).getCategory());
        assertEquals("btm-generation-service", response.getGeneratedArtifacts(0).getProducerService());
        assertEquals(1, response.getGeneratedRulesCount());
        assertEquals(ProbeKind.PROBE_KIND_METHOD_ENTRY, response.getGeneratedRules(0).getProbeKind());
        assertEquals("target-1", response.getGeneratedRules(0).getTargetId());
        assertEquals("demo", response.getSafeAttributesOrThrow("tenant"));
    }

    @Test
    void keepsIncompleteInputsExplicitWithoutFabricatingRules() throws Exception {
        startServer(new FileSystemBtmArtifactWriter(tempDir));

        var response = stub.generateBtmRules(request(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_BTM_GENERATION).toBuilder()
            .setFacts(DeliveredAnalysisFacts.newBuilder()
                .addSourceFactArtifacts(artifact("source-facts.json"))
                .setInputCompleteness(AnalysisCompleteness.ANALYSIS_COMPLETENESS_INCOMPLETE)
                .addTargets(InstrumentationTarget.newBuilder()
                    .setTargetId("target-missing")
                    .setSourceFactId("fact-missing")
                    .setProbeKind(ProbeKind.PROBE_KIND_METHOD_ENTRY)))
            .build());

        assertEquals("GENERATED_INCOMPLETE", response.getStatus().getCode());
        assertEquals(AnalysisCompleteness.ANALYSIS_COMPLETENESS_INCOMPLETE, response.getCompleteness());
        assertEquals(0, response.getGeneratedRulesCount());
        assertEquals(List.of("INPUT_FACTS_INCOMPLETE", "AMBIGUOUS_TARGET_MAPPING"), response.getDiagnosticsList().stream()
            .map(diagnostic -> diagnostic.getCode())
            .toList());
    }

    @Test
    void mapsInvalidRequestsAndFailuresToGrpcStatuses() throws Exception {
        startServer(new FileSystemBtmArtifactWriter(tempDir));

        var invalidWorker = assertThrows(
            StatusRuntimeException.class,
            () -> stub.generateBtmRules(request(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_REPORT))
        );
        assertEquals(Status.Code.INVALID_ARGUMENT, invalidWorker.getStatus().getCode());

        var noPolicy = assertThrows(
            StatusRuntimeException.class,
            () -> stub.generateBtmRules(request(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_BTM_GENERATION).toBuilder()
                .clearPolicy()
                .build())
        );
        assertEquals(Status.Code.INVALID_ARGUMENT, noPolicy.getStatus().getCode());

        var unsafePath = assertThrows(
            StatusRuntimeException.class,
            () -> stub.generateBtmRules(request(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_BTM_GENERATION).toBuilder()
                .setFacts(DeliveredAnalysisFacts.newBuilder()
                    .setInputCompleteness(AnalysisCompleteness.ANALYSIS_COMPLETENESS_COMPLETE)
                    .addTargets(target().toBuilder().setRelativePath("../A.java")))
                .build())
        );
        assertEquals(Status.Code.INVALID_ARGUMENT, unsafePath.getStatus().getCode());

        stopServer();
        startServer(request -> {
            throw new BtmArtifactException("Generated BTM artifacts exceed configured output limit.");
        });
        var outputLimit = assertThrows(
            StatusRuntimeException.class,
            () -> stub.generateBtmRules(request(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_BTM_GENERATION))
        );
        assertEquals(Status.Code.RESOURCE_EXHAUSTED, outputLimit.getStatus().getCode());

        stopServer();
        startServer(request -> {
            throw new BtmArtifactException("cannot write /private/source");
        });
        var writeFailure = assertThrows(
            StatusRuntimeException.class,
            () -> stub.generateBtmRules(request(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_BTM_GENERATION))
        );
        assertEquals(Status.Code.FAILED_PRECONDITION, writeFailure.getStatus().getCode());
        assertEquals("BTM artifact write failed", writeFailure.getStatus().getDescription());

        stopServer();
        startServer(request -> {
            throw new IllegalStateException("unexpected /private/source failure");
        });
        var unexpected = assertThrows(
            StatusRuntimeException.class,
            () -> stub.generateBtmRules(request(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_BTM_GENERATION))
        );
        assertEquals(Status.Code.FAILED_PRECONDITION, unexpected.getStatus().getCode());
        assertEquals("BTM generation failed", unexpected.getStatus().getDescription());
    }

    @Test
    void mapsTimeoutToDeadlineExceeded() throws Exception {
        startServer(request -> {
            try {
                Thread.sleep(2_000);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
            }
            return new GeneratedBtmArtifacts(List.of(), 0);
        });

        var timeout = assertThrows(
            StatusRuntimeException.class,
            () -> stub.generateBtmRules(request(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_BTM_GENERATION).toBuilder()
                .setPolicy(policy().toBuilder().setTimeoutSeconds(1))
                .build())
        );
        assertEquals(Status.Code.DEADLINE_EXCEEDED, timeout.getStatus().getCode());
    }

    private void startServer(BtmArtifactWriterPort writer) throws Exception {
        var serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(new BtmGenerationGrpcEndpoint(new BtmGenerationApplicationService(writer)))
            .build()
            .start();
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        stub = BtmGenerationServiceGrpc.newBlockingStub(channel);
    }

    private static GenerateBtmRulesRequest request(AnalysisWorkerKind workerKind) {
        return GenerateBtmRulesRequest.newBuilder()
            .setRequestId("request-1")
            .setIdempotencyKey("idempotency-1")
            .setSchemaVersion("btm-generation-v1")
            .setCorrelationId("correlation-1")
            .setWorkerKind(workerKind)
            .setAnalysisRunId(AnalysisRunId.newBuilder().setValue("run-1"))
            .setAnalysisJobId(AnalysisJobId.newBuilder().setValue("job-1"))
            .setSourceSnapshotId(SourceSnapshotId.newBuilder().setValue("snapshot-1"))
            .setWorkerVersion("btm-generation-service-test")
            .setPolicy(policy())
            .setFacts(DeliveredAnalysisFacts.newBuilder()
                .addSourceFactArtifacts(artifact("source-facts.json"))
                .addSemanticArtifacts(artifact("joern-cpg/cpg.bin.zip"))
                .addTargets(target())
                .setInputCompleteness(AnalysisCompleteness.ANALYSIS_COMPLETENESS_COMPLETE))
            .putSafeAttributes("tenant", "demo")
            .build();
    }

    private static BtmGenerationPolicy policy() {
        return BtmGenerationPolicy.newBuilder()
            .setMaxTargets(10)
            .setMaxArtifactBytes(100_000)
            .setTimeoutSeconds(60)
            .setRuleSchemaVersion("btm-rule-v1")
            .build();
    }

    private static InstrumentationTarget target() {
        return InstrumentationTarget.newBuilder()
            .setTargetId("target-1")
            .setSourceFactId("fact-1")
            .setSemanticNodeId("semantic-1")
            .setRelativePath("src/main/java/a/A.java")
            .setFullyQualifiedClassName("a.A")
            .setMethodName("run")
            .setSignature("a.A#run()")
            .setLineNumber(12)
            .setProbeKind(ProbeKind.PROBE_KIND_METHOD_ENTRY)
            .build();
    }

    private static AnalysisArtifactReference artifact(String path) {
        return AnalysisArtifactReference.newBuilder()
            .setArtifact(ArtifactReference.newBuilder()
                .setPath(path)
                .setType("application/vnd.forensic-analytics.source-facts.v1+json")
                .setSha256("a".repeat(64))
                .setSizeBytes(12))
            .setCategory(AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_STATIC)
            .setProducerService("analysis-store-service")
            .setSchemaVersion("source-facts-v1")
            .setCompleteness(AnalysisCompleteness.ANALYSIS_COMPLETENESS_COMPLETE)
            .build();
    }
}
