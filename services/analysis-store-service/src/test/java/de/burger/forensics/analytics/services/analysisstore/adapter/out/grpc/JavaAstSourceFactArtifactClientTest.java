package de.burger.forensics.analytics.services.analysisstore.adapter.out.grpc;

import com.google.protobuf.ByteString;
import de.burger.forensics.analytics.javaastanalysis.v1.GetSourceFactArtifactBytesRequest;
import de.burger.forensics.analytics.javaastanalysis.v1.GetSourceFactArtifactBytesResponse;
import de.burger.forensics.analytics.javaastanalysis.v1.JavaAstAnalysisServiceGrpc;
import de.burger.forensics.analytics.javaastanalysis.v1.OperationStatus;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactCategory;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJobId;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisRunId;
import de.burger.forensics.analytics.services.analysisstore.domain.ArtifactByteAccess;
import de.burger.forensics.analytics.services.analysisstore.domain.ArtifactByteCustody;
import de.burger.forensics.analytics.services.analysisstore.domain.ArtifactReference;
import de.burger.forensics.analytics.services.analysisstore.domain.SourceSnapshotId;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaAstSourceFactArtifactClientTest {
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
    void retrievesSourceFactBytesThroughJavaAstOwnerApi() throws Exception {
        var content = "{\"sourceFacts\":[]}".getBytes(StandardCharsets.UTF_8);
        var service = new CapturingJavaAstArtifactService(content, sha256(content), content.length);
        var client = startClient(service);

        var bytes = client.read(
            new AnalysisRunId("run-1"),
            new AnalysisJobId("job-ast-1"),
            new SourceSnapshotId("snapshot-1"),
            "request-bytes",
            "correlation-1",
            artifact(sha256(content), content.length),
            Map.of("tenant", "demo")
        );

        assertEquals("request-bytes", service.request.getRequestId());
        assertEquals("run-1", service.request.getAnalysisRunId().getValue());
        assertEquals("job-ast-1", service.request.getAnalysisJobId().getValue());
        assertEquals("snapshot-1", service.request.getSourceSnapshotId().getValue());
        assertEquals("java-ast/snapshot-1-source-facts.json", service.request.getRetrievalReference());
        assertEquals(1_000, service.request.getMaxBytes());
        assertEquals("demo", service.request.getSafeAttributesMap().get("tenant"));
        assertEquals("java-ast/snapshot-1-source-facts.json", bytes.artifact().path());
        assertEquals("java-ast-analysis-service", bytes.artifact().producerService());
        assertArrayEquals(content, bytes.content());
    }

    @Test
    void rejectsMismatchedChecksumAndSanitizesGrpcFailures() throws Exception {
        var content = "{\"sourceFacts\":[]}".getBytes(StandardCharsets.UTF_8);
        var mismatch = startClient(new CapturingJavaAstArtifactService(content, "b".repeat(64), content.length));

        var checksumFailure = assertThrows(
            IllegalStateException.class,
            () -> mismatch.read(
                new AnalysisRunId("run-1"),
                new AnalysisJobId("job-ast-1"),
                new SourceSnapshotId("snapshot-1"),
                "request-bytes",
                "correlation-1",
                artifact(sha256(content), content.length),
                Map.of()
            )
        );
        assertEquals("Java AST source fact artifact checksum mismatch", checksumFailure.getMessage());

        stopServer();
        var original = "source-facts-1".getBytes(StandardCharsets.UTF_8);
        var tampered = "source-facts-2".getBytes(StandardCharsets.UTF_8);
        var tamperedClient = startClient(new CapturingJavaAstArtifactService(tampered, sha256(original), original.length));
        var tamperedFailure = assertThrows(
            IllegalStateException.class,
            () -> tamperedClient.read(
                new AnalysisRunId("run-1"),
                new AnalysisJobId("job-ast-1"),
                new SourceSnapshotId("snapshot-1"),
                "request-bytes",
                "correlation-1",
                artifact(sha256(original), original.length),
                Map.of()
            )
        );
        assertEquals("Java AST source fact artifact checksum mismatch", tamperedFailure.getMessage());

        stopServer();
        var failing = startClient(new FailingJavaAstArtifactService());
        var grpcFailure = assertThrows(
            IllegalStateException.class,
            () -> failing.read(
                new AnalysisRunId("run-1"),
                new AnalysisJobId("job-ast-1"),
                new SourceSnapshotId("snapshot-1"),
                "request-bytes",
                "correlation-1",
                artifact(sha256(content), content.length),
                Map.of()
            )
        );
        assertEquals("Java AST source fact artifact retrieval failed with status UNAVAILABLE", grpcFailure.getMessage());
    }

    @Test
    void supportsOnlyJavaAstStaticSourceFactArtifacts() throws Exception {
        var client = startClient(new CapturingJavaAstArtifactService(new byte[] {1}, "a".repeat(64), 1));

        assertTrue(client.supports(artifact("a".repeat(64), 1)));
        assertTrue(client.supports(artifact(
            "a".repeat(64),
            1,
            AnalysisArtifactCategory.STATIC,
            "other-service",
            JavaAstSourceFactArtifactClient.OWNER_SERVICE,
            JavaAstSourceFactArtifactClient.RETRIEVAL_CONTRACT,
            AnalysisCompleteness.COMPLETE
        )));
        assertFalse(client.supports(artifact(
            "a".repeat(64),
            1,
            AnalysisArtifactCategory.RUNTIME,
            JavaAstSourceFactArtifactClient.OWNER_SERVICE,
            JavaAstSourceFactArtifactClient.RETRIEVAL_CONTRACT
        )));
        assertFalse(client.supports(artifact(
            "a".repeat(64),
            1,
            AnalysisArtifactCategory.STATIC,
            "other-service",
            JavaAstSourceFactArtifactClient.RETRIEVAL_CONTRACT
        )));
        assertFalse(client.supports(artifact(
            "a".repeat(64),
            1,
            AnalysisArtifactCategory.STATIC,
            JavaAstSourceFactArtifactClient.OWNER_SERVICE,
            "other.v1.Bytes"
        )));
    }

    @Test
    void rejectsMismatchedResponseSizeAndInvalidClientSettings() throws Exception {
        var content = "source-facts".getBytes(StandardCharsets.UTF_8);
        var reportedSizeMismatch = startClient(new CapturingJavaAstArtifactService(content, sha256(content), content.length + 1));

        var reportedSizeFailure = assertThrows(
            IllegalStateException.class,
            () -> reportedSizeMismatch.read(
                new AnalysisRunId("run-1"),
                new AnalysisJobId("job-ast-1"),
                new SourceSnapshotId("snapshot-1"),
                "request-bytes",
                "correlation-1",
                artifact(sha256(content), content.length),
                Map.of()
            )
        );
        assertEquals("Java AST source fact artifact size mismatch", reportedSizeFailure.getMessage());

        stopServer();
        var largerContent = "source-facts-with-extra-byte".getBytes(StandardCharsets.UTF_8);
        var contentSizeMismatch = startClient(
            new CapturingJavaAstArtifactService(largerContent, sha256(largerContent), content.length)
        );
        var contentSizeFailure = assertThrows(
            IllegalStateException.class,
            () -> contentSizeMismatch.read(
                new AnalysisRunId("run-1"),
                new AnalysisJobId("job-ast-1"),
                new SourceSnapshotId("snapshot-1"),
                "request-bytes",
                "correlation-1",
                artifact(sha256(largerContent), content.length),
                Map.of()
            )
        );
        assertEquals("Java AST source fact artifact size mismatch", contentSizeFailure.getMessage());

        stopServer();
        channel = InProcessChannelBuilder.forName(InProcessServerBuilder.generateName()).directExecutor().build();
        var stub = JavaAstAnalysisServiceGrpc.newBlockingStub(channel);
        assertThrows(IllegalArgumentException.class, () -> new JavaAstSourceFactArtifactClient(stub, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new JavaAstSourceFactArtifactClient(stub, 1, 0));
    }

    @Test
    void rejectsMismatchedOwnerResponseMetadata() throws Exception {
        var content = "source-facts".getBytes(StandardCharsets.UTF_8);
        var wrongProducer = startClient(new CapturingJavaAstArtifactService(
            content,
            sha256(content),
            content.length,
            "other-service",
            de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.ANALYSIS_COMPLETENESS_COMPLETE
        ));

        var producerFailure = assertThrows(
            IllegalStateException.class,
            () -> wrongProducer.read(
                new AnalysisRunId("run-1"),
                new AnalysisJobId("job-ast-1"),
                new SourceSnapshotId("snapshot-1"),
                "request-bytes",
                "correlation-1",
                artifact(sha256(content), content.length),
                Map.of()
            )
        );
        assertEquals("Java AST source fact artifact metadata mismatch", producerFailure.getMessage());

        stopServer();
        var incompleteResponse = startClient(new CapturingJavaAstArtifactService(
            content,
            sha256(content),
            content.length,
            JavaAstSourceFactArtifactClient.OWNER_SERVICE,
            de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.ANALYSIS_COMPLETENESS_INCOMPLETE
        ));
        var completenessFailure = assertThrows(
            IllegalStateException.class,
            () -> incompleteResponse.read(
                new AnalysisRunId("run-1"),
                new AnalysisJobId("job-ast-1"),
                new SourceSnapshotId("snapshot-1"),
                "request-bytes",
                "correlation-1",
                artifact(sha256(content), content.length),
                Map.of()
            )
        );
        assertEquals("Java AST source fact artifact metadata mismatch", completenessFailure.getMessage());
    }

    @Test
    void acceptsUnknownCompletenessFromOwnerResponse() throws Exception {
        var content = "source-facts".getBytes(StandardCharsets.UTF_8);
        var unknownResponse = startClient(new CapturingJavaAstArtifactService(
            content,
            sha256(content),
            content.length,
            JavaAstSourceFactArtifactClient.OWNER_SERVICE,
            de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.ANALYSIS_COMPLETENESS_UNKNOWN
        ));

        var bytes = unknownResponse.read(
            new AnalysisRunId("run-1"),
            new AnalysisJobId("job-ast-1"),
            new SourceSnapshotId("snapshot-1"),
            "request-bytes",
            "correlation-1",
            artifact(sha256(content), content.length, AnalysisCompleteness.UNKNOWN),
            Map.of()
        );

        assertEquals(AnalysisCompleteness.UNKNOWN, bytes.artifact().completeness());
    }

    @Test
    void rejectsNonJavaAstOwnerMetadataBeforeNetworkCall() throws Exception {
        var client = startClient(new CapturingJavaAstArtifactService(new byte[] {1}, "a".repeat(64), 1));

        var producerFailure = assertThrows(
            IllegalArgumentException.class,
            () -> client.read(
                new AnalysisRunId("run-1"),
                new AnalysisJobId("job-ast-1"),
                new SourceSnapshotId("snapshot-1"),
                "request-bytes",
                "correlation-1",
                artifact(
                    "a".repeat(64),
                    1,
                    AnalysisArtifactCategory.STATIC,
                    "other-service",
                    JavaAstSourceFactArtifactClient.OWNER_SERVICE,
                    JavaAstSourceFactArtifactClient.RETRIEVAL_CONTRACT,
                    AnalysisCompleteness.COMPLETE
                ),
                Map.of()
            )
        );
        assertEquals("source fact artifact bytes must be owned by Java AST Analysis", producerFailure.getMessage());

        var failure = assertThrows(
            IllegalArgumentException.class,
            () -> client.read(
                new AnalysisRunId("run-1"),
                new AnalysisJobId("job-ast-1"),
                new SourceSnapshotId("snapshot-1"),
                "request-bytes",
                "correlation-1",
                new AnalysisArtifactReference(
                    new ArtifactReference("java-ast/snapshot-1-source-facts.json", "application/json", "a".repeat(64), 1),
                    AnalysisArtifactCategory.STATIC,
                    "java-ast-analysis-service",
                    "java-ast-analysis-v1",
                    AnalysisCompleteness.COMPLETE,
                    new ArtifactByteAccess(
                        "analysis-store-service",
                        JavaAstSourceFactArtifactClient.RETRIEVAL_CONTRACT,
                        "java-ast/snapshot-1-source-facts.json",
                        ArtifactByteCustody.PRODUCER_RETAINED
                    )
                ),
                Map.of()
            )
        );

        assertEquals("source fact artifact bytes must be owned by Java AST Analysis", failure.getMessage());

        var contractFailure = assertThrows(
            IllegalArgumentException.class,
            () -> client.read(
                new AnalysisRunId("run-1"),
                new AnalysisJobId("job-ast-1"),
                new SourceSnapshotId("snapshot-1"),
                "request-bytes",
                "correlation-1",
                artifact(
                    "a".repeat(64),
                    1,
                    AnalysisArtifactCategory.STATIC,
                    JavaAstSourceFactArtifactClient.OWNER_SERVICE,
                    "other.v1.Bytes"
                ),
                Map.of()
            )
        );
        assertEquals(
            "source fact artifact retrieval contract is not the Java AST owner API",
            contractFailure.getMessage()
        );
    }

    private JavaAstSourceFactArtifactClient startClient(JavaAstAnalysisServiceGrpc.JavaAstAnalysisServiceImplBase service) throws Exception {
        var serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(service)
            .build()
            .start();
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        return new JavaAstSourceFactArtifactClient(JavaAstAnalysisServiceGrpc.newBlockingStub(channel), 5, 1_000);
    }

    private static AnalysisArtifactReference artifact(String sha256, long sizeBytes) {
        return artifact(sha256, sizeBytes, AnalysisCompleteness.COMPLETE);
    }

    private static AnalysisArtifactReference artifact(String sha256, long sizeBytes, AnalysisCompleteness completeness) {
        return artifact(
            sha256,
            sizeBytes,
            AnalysisArtifactCategory.STATIC,
            "java-ast-analysis-service",
            JavaAstSourceFactArtifactClient.OWNER_SERVICE,
            JavaAstSourceFactArtifactClient.RETRIEVAL_CONTRACT,
            completeness
        );
    }

    private static AnalysisArtifactReference artifact(
        String sha256,
        long sizeBytes,
        AnalysisArtifactCategory category,
        String ownerService,
        String retrievalContract
    ) {
        return artifact(
            sha256,
            sizeBytes,
            category,
            "java-ast-analysis-service",
            ownerService,
            retrievalContract,
            AnalysisCompleteness.COMPLETE
        );
    }

    private static AnalysisArtifactReference artifact(
        String sha256,
        long sizeBytes,
        AnalysisArtifactCategory category,
        String producerService,
        String ownerService,
        String retrievalContract,
        AnalysisCompleteness completeness
    ) {
        return new AnalysisArtifactReference(
            new ArtifactReference(
                "java-ast/snapshot-1-source-facts.json",
                "application/vnd.forensic-analytics.java-ast-source-facts.v1+json",
                sha256,
                sizeBytes
            ),
            category,
            producerService,
            "java-ast-analysis-v1",
            completeness,
            new ArtifactByteAccess(
                ownerService,
                retrievalContract,
                "java-ast/snapshot-1-source-facts.json",
                ArtifactByteCustody.PRODUCER_RETAINED
            )
        );
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
    }

    private static final class CapturingJavaAstArtifactService extends JavaAstAnalysisServiceGrpc.JavaAstAnalysisServiceImplBase {
        private final byte[] content;
        private final String sha256;
        private final long sizeBytes;
        private final String producerService;
        private final de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness completeness;
        private GetSourceFactArtifactBytesRequest request;

        private CapturingJavaAstArtifactService(byte[] content, String sha256, long sizeBytes) {
            this(
                content,
                sha256,
                sizeBytes,
                JavaAstSourceFactArtifactClient.OWNER_SERVICE,
                de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.ANALYSIS_COMPLETENESS_COMPLETE
            );
        }

        private CapturingJavaAstArtifactService(
            byte[] content,
            String sha256,
            long sizeBytes,
            String producerService,
            de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness completeness
        ) {
            this.content = content.clone();
            this.sha256 = sha256;
            this.sizeBytes = sizeBytes;
            this.producerService = producerService;
            this.completeness = completeness;
        }

        @Override
        public void getSourceFactArtifactBytes(
            GetSourceFactArtifactBytesRequest request,
            StreamObserver<GetSourceFactArtifactBytesResponse> responseObserver
        ) {
            this.request = request;
            responseObserver.onNext(GetSourceFactArtifactBytesResponse.newBuilder()
                .setStatus(OperationStatus.newBuilder()
                    .setCode("SOURCE_FACT_ARTIFACT_BYTES_RETRIEVED")
                    .setMessage("Java AST source fact artifact bytes retrieved")
                    .setCorrelationId(request.getCorrelationId()))
                .setAnalysisRunId(request.getAnalysisRunId())
                .setAnalysisJobId(request.getAnalysisJobId())
                .setSourceSnapshotId(request.getSourceSnapshotId())
                .setSourceFactArtifact(de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactReference.newBuilder()
                    .setArtifact(de.burger.forensics.analytics.analysisjob.v1.ArtifactReference.newBuilder()
                        .setPath(request.getRetrievalReference())
                        .setType("application/vnd.forensic-analytics.java-ast-source-facts.v1+json")
                        .setSha256(sha256)
                        .setSizeBytes(sizeBytes))
                    .setCategory(de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_STATIC)
                    .setProducerService(producerService)
                    .setSchemaVersion(request.getSchemaVersion())
                    .setCompleteness(completeness)
                    .setByteAccess(de.burger.forensics.analytics.analysisjob.v1.ArtifactByteAccess.newBuilder()
                        .setOwnerService(JavaAstSourceFactArtifactClient.OWNER_SERVICE)
                        .setRetrievalContract(JavaAstSourceFactArtifactClient.RETRIEVAL_CONTRACT)
                        .setRetrievalReference(request.getRetrievalReference())
                        .setByteCustody(de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_PRODUCER_RETAINED)))
                .setContent(ByteString.copyFrom(content))
                .setSha256(sha256)
                .setSizeBytes(sizeBytes)
                .putAllSafeAttributes(request.getSafeAttributesMap())
                .build());
            responseObserver.onCompleted();
        }
    }

    private static final class FailingJavaAstArtifactService extends JavaAstAnalysisServiceGrpc.JavaAstAnalysisServiceImplBase {
        @Override
        public void getSourceFactArtifactBytes(
            GetSourceFactArtifactBytesRequest request,
            StreamObserver<GetSourceFactArtifactBytesResponse> responseObserver
        ) {
            responseObserver.onError(new StatusRuntimeException(
                Status.UNAVAILABLE.withDescription("failed at /private/workspace")
            ));
        }
    }
}
