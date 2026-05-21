package de.burger.forensics.analytics.services.joernanalysis.adapter.out.grpc;

import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobServiceGrpc;
import de.burger.forensics.analytics.analysisjob.v1.OperationStatus;
import de.burger.forensics.analytics.analysisjob.v1.RegisterAnalysisArtifactsRequest;
import de.burger.forensics.analytics.analysisjob.v1.RegisterAnalysisArtifactsResponse;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalysisArtifactCategory;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalysisJobId;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalyzeJoernCpgResult;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.ArtifactByteAccess;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.ArtifactByteCustody;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.ArtifactReference;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.JoernCpgSummary;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.RequestMetadata;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.SourceSnapshotId;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.PRODUCER_SERVICE;
import static de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.SEMANTIC_ARTIFACT_SCHEMA_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisStoreArtifactRegistryGrpcClientTest {
    private Server server;
    private ManagedChannel channel;

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
    void registersSemanticArtifactsWithMetadataAndByteAccess() throws Exception {
        var request = new AtomicReference<RegisterAnalysisArtifactsRequest>();
        var client = startClient(new AnalysisJobServiceGrpc.AnalysisJobServiceImplBase() {
            @Override
            public void registerAnalysisArtifacts(
                RegisterAnalysisArtifactsRequest value,
                StreamObserver<RegisterAnalysisArtifactsResponse> responseObserver
            ) {
                request.set(value);
                responseObserver.onNext(RegisterAnalysisArtifactsResponse.newBuilder()
                    .setStatus(OperationStatus.newBuilder()
                        .setCode("ACCEPTED")
                        .setCorrelationId(value.getCorrelationId()))
                    .addAllArtifacts(value.getArtifactsList())
                    .build());
                responseObserver.onCompleted();
            }
        });

        client.registerSemanticArtifacts(result(List.of(
            reference("joern-cpg/run-1/cpg.bin.zip", AnalysisCompleteness.COMPLETE, ArtifactByteCustody.PRODUCER_RETAINED),
            reference("joern-cpg/run-1/callgraph.json", AnalysisCompleteness.INCOMPLETE, ArtifactByteCustody.SCOPED_OBJECT_ACCESS),
            reference("joern-cpg/run-1/dataflow.json", AnalysisCompleteness.UNKNOWN, ArtifactByteCustody.EXPLICIT_HANDOFF)
        )));

        assertEquals("request-1-semantic-artifacts", request.get().getRequestId());
        assertEquals("idempotency-1:semantic-artifacts", request.get().getIdempotencyKey());
        assertEquals("correlation-1", request.get().getCorrelationId());
        assertEquals("run-1", request.get().getAnalysisRunId().getValue());
        assertEquals("job-1", request.get().getJobId().getValue());
        assertEquals(3, request.get().getArtifactsCount());
        assertEquals(List.of("joern-cpg/run-1/callgraph.json", "joern-cpg/run-1/cpg.bin.zip", "joern-cpg/run-1/dataflow.json"), request.get()
            .getArtifactsList()
            .stream()
            .map(value -> value.getArtifact().getPath())
            .toList());
        assertEquals("analysis-job.v1.ArtifactBytes", request.get().getArtifacts(1).getByteAccess().getRetrievalContract());
        assertEquals("artifacts/joern-cpg/run-1/cpg.bin.zip", request.get().getArtifacts(1).getByteAccess().getRetrievalReference());
        assertEquals(
            de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.ANALYSIS_COMPLETENESS_INCOMPLETE,
            request.get().getArtifacts(0).getCompleteness()
        );
        assertEquals(
            de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.ANALYSIS_COMPLETENESS_UNKNOWN,
            request.get().getArtifacts(2).getCompleteness()
        );
        assertEquals(
            de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_SCOPED_OBJECT_ACCESS,
            request.get().getArtifacts(0).getByteAccess().getByteCustody()
        );
        assertEquals(
            de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_EXPLICIT_HANDOFF,
            request.get().getArtifacts(2).getByteAccess().getByteCustody()
        );
    }

    @Test
    void failsWhenAnalysisStoreReturnsNonAcceptedStatus() throws Exception {
        var client = startClient(new AnalysisJobServiceGrpc.AnalysisJobServiceImplBase() {
            @Override
            public void registerAnalysisArtifacts(
                RegisterAnalysisArtifactsRequest request,
                StreamObserver<RegisterAnalysisArtifactsResponse> responseObserver
            ) {
                responseObserver.onNext(RegisterAnalysisArtifactsResponse.newBuilder()
                    .setStatus(OperationStatus.newBuilder()
                        .setCode("REJECTED")
                        .setCorrelationId(request.getCorrelationId()))
                    .build());
                responseObserver.onCompleted();
            }
        });

        var error = assertThrows(IllegalStateException.class, () -> client.registerSemanticArtifacts(result(AnalysisCompleteness.COMPLETE)));

        assertTrue(error.getMessage().contains("REJECTED"));
    }

    @Test
    void failsWhenAnalysisStoreRejectsRegistration() throws Exception {
        var client = startClient(new AnalysisJobServiceGrpc.AnalysisJobServiceImplBase() {
            @Override
            public void registerAnalysisArtifacts(
                RegisterAnalysisArtifactsRequest request,
                StreamObserver<RegisterAnalysisArtifactsResponse> responseObserver
            ) {
                responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("bad artifact").asRuntimeException());
            }
        });

        var error = assertThrows(IllegalStateException.class, () -> client.registerSemanticArtifacts(result(AnalysisCompleteness.COMPLETE)));

        assertTrue(error.getMessage().contains("INVALID_ARGUMENT"));
    }

    @Test
    void validatesDeadlinesAndAllowsClosingBorrowedStubs() throws Exception {
        var client = startClient(new AnalysisJobServiceGrpc.AnalysisJobServiceImplBase() {
        });
        var borrowed = new AnalysisStoreArtifactRegistryGrpcClient(AnalysisJobServiceGrpc.newBlockingStub(channel), 5);

        assertThrows(IllegalArgumentException.class, () -> new AnalysisStoreArtifactRegistryGrpcClient(channel, 0));
        borrowed.close();
        client.close();
    }

    private AnalysisStoreArtifactRegistryGrpcClient startClient(AnalysisJobServiceGrpc.AnalysisJobServiceImplBase service) throws Exception {
        var serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(service)
            .build()
            .start();
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        return new AnalysisStoreArtifactRegistryGrpcClient(channel, 5);
    }

    private static AnalyzeJoernCpgResult result(AnalysisCompleteness completeness) {
        return result(List.of(reference("joern-cpg/run-1/cpg.bin.zip", completeness, ArtifactByteCustody.PRODUCER_RETAINED)));
    }

    private static AnalyzeJoernCpgResult result(List<AnalysisArtifactReference> artifacts) {
        return new AnalyzeJoernCpgResult(
            new RequestMetadata(
                "request-1",
                "idempotency-1",
                "joern-cpg-analysis-v1",
                "correlation-1",
                new AnalysisRunId("run-1"),
                new AnalysisJobId("job-1"),
                new SourceSnapshotId("snapshot-1"),
                "joern-analysis-service-test",
                Map.of("tenant", "demo")
            ),
            artifacts.stream()
                .map(AnalysisArtifactReference::completeness)
                .filter(value -> value != AnalysisCompleteness.COMPLETE)
                .findAny()
                .map(ignored -> AnalysisCompleteness.INCOMPLETE)
                .orElse(AnalysisCompleteness.COMPLETE),
            artifacts,
            new JoernCpgSummary(
                1,
                artifacts.size(),
                artifacts.stream().anyMatch(reference -> reference.completeness() != AnalysisCompleteness.COMPLETE) ? 1 : 0,
                "joern-test",
                image(),
                "queries-v1",
                PRODUCER_SERVICE,
                SEMANTIC_ARTIFACT_SCHEMA_VERSION
            ),
            List.of()
        );
    }

    private static AnalysisArtifactReference reference(
        String path,
        AnalysisCompleteness completeness,
        ArtifactByteCustody custody
    ) {
        return new AnalysisArtifactReference(
            new ArtifactReference(
                path,
                "application/vnd.forensic-analytics.joern-cpg.v1+binary",
                "a".repeat(64),
                128
            ),
            AnalysisArtifactCategory.STATIC,
            PRODUCER_SERVICE,
            SEMANTIC_ARTIFACT_SCHEMA_VERSION,
            completeness,
            new ArtifactByteAccess(
                PRODUCER_SERVICE,
                "analysis-job.v1.ArtifactBytes",
                "artifacts/" + path,
                custody
            )
        );
    }

    private static String image() {
        return "ghcr.io/joernio/joern@sha256:" + "a".repeat(64);
    }
}
