package de.burger.forensics.analytics.services.analysisstore.adapter.out.grpc;

import com.google.protobuf.ByteString;
import de.burger.forensics.analytics.javaastanalysis.v1.GetSourceFactArtifactBytesRequest;
import de.burger.forensics.analytics.javaastanalysis.v1.GetSourceFactArtifactBytesResponse;
import de.burger.forensics.analytics.javaastanalysis.v1.JavaAstAnalysisServiceGrpc;
import de.burger.forensics.analytics.services.analysisstore.adapter.out.javaast.JavaAstSourceFactArtifactPayloadParser;
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
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JavaAstSourceFactArtifactClientTest {
    private Server server;
    private ManagedChannel channel;
    private GetSourceFactArtifactBytesRequest capturedRequest;

    @AfterEach
    void stopGrpc() {
        if (channel != null) {
            channel.shutdownNow();
            channel = null;
        }
        if (server != null) {
            server.shutdownNow();
            server = null;
        }
    }

    @Test
    void verifiesSourceFactPayloadContractBeforeAcceptingRetrievedBytes() throws Exception {
        var content = validPayload().getBytes(StandardCharsets.UTF_8);
        var artifact = artifact(content);
        var client = clientReturning(artifact, content);

        var bytes = client.read(
            runId(),
            jobId(),
            snapshotId(),
            "request-1",
            "correlation-1",
            artifact,
            Map.of("tenant", "demo")
        );

        assertEquals("request-1", capturedRequest.getRequestId());
        assertEquals("run-1", capturedRequest.getAnalysisRunId().getValue());
        assertEquals("job-1", capturedRequest.getAnalysisJobId().getValue());
        assertEquals("snapshot-1", capturedRequest.getSourceSnapshotId().getValue());
        assertEquals("java-ast/snapshot-1-source-facts.json", capturedRequest.getRetrievalReference());
        assertEquals(1_048_576, capturedRequest.getMaxBytes());
        assertEquals("demo", capturedRequest.getSafeAttributesMap().get("tenant"));
        assertEquals(artifact, bytes.artifact());
        assertArrayEquals(content, bytes.content());
    }

    @Test
    void rejectsContractInvalidRetrievedPayloadDespiteMatchingMetadataAndChecksum() throws Exception {
        var content = validPayload()
            .replace("\"factType\": \"java-method\"", "\"factType\": \"java-field\"")
            .getBytes(StandardCharsets.UTF_8);
        var artifact = artifact(content);
        var client = clientReturning(artifact, content);

        var failure = assertThrows(IllegalStateException.class, () -> client.verify(
            runId(),
            jobId(),
            snapshotId(),
            "request-1",
            "correlation-1",
            artifact,
            Map.of("tenant", "demo")
        ));

        assertEquals("Java AST source fact artifact payload violates the v1 contract.", failure.getMessage());
    }

    @Test
    void rejectsSchemaInvalidRetrievedPayloadDespiteMatchingMetadataAndChecksum() throws Exception {
        var content = "{}".getBytes(StandardCharsets.UTF_8);
        var artifact = artifact(content);
        var client = clientReturning(artifact, content);

        var failure = assertThrows(IllegalStateException.class, () -> client.verify(
            runId(),
            jobId(),
            snapshotId(),
            "request-1",
            "correlation-1",
            artifact,
            Map.of("tenant", "demo")
        ));

        assertEquals("Java AST source fact artifact payload violates the v1 contract.", failure.getMessage());
    }

    @Test
    void rejectsPayloadCompletenessThatDoesNotMatchReturnedMetadata() throws Exception {
        var content = validPayload().replace("\"diagnostics\": []", """
            "diagnostics": [
                {
                  "code": "SYMBOL_RESOLUTION_NOT_CONFIGURED",
                  "message": "symbol solving is not configured",
                  "severity": "WARNING",
                  "sourceSnapshotId": "snapshot-1",
                  "sourcePath": "",
                  "lineNumber": 0,
                  "columnNumber": 0,
                  "retryable": false,
                  "affectsCompleteness": true
                }
              ]""").getBytes(StandardCharsets.UTF_8);
        var artifact = artifact(content);
        var client = clientReturning(artifact, content);

        var failure = assertThrows(IllegalStateException.class, () -> client.verify(
            runId(),
            jobId(),
            snapshotId(),
            "request-1",
            "correlation-1",
            artifact,
            Map.of("tenant", "demo")
        ));

        assertEquals("Java AST source fact artifact payload completeness does not match metadata.", failure.getMessage());
    }

    @Test
    void rejectsArtifactMetadataIncompleteWhenPayloadHasNoCompletenessDiagnostic() throws Exception {
        var content = validPayload().getBytes(StandardCharsets.UTF_8);
        var artifact = artifact(
            sha256(content),
            content.length,
            AnalysisArtifactCategory.STATIC,
            JavaAstSourceFactArtifactClient.OWNER_SERVICE,
            JavaAstSourceFactArtifactClient.OWNER_SERVICE,
            JavaAstSourceFactArtifactClient.RETRIEVAL_CONTRACT,
            AnalysisCompleteness.INCOMPLETE
        );
        var client = clientReturning(artifact, content);

        var failure = assertThrows(IllegalStateException.class, () -> client.verify(
            runId(),
            jobId(),
            snapshotId(),
            "request-1",
            "correlation-1",
            artifact,
            Map.of("tenant", "demo")
        ));

        assertEquals("Java AST source fact artifact payload completeness does not match metadata.", failure.getMessage());
    }

    @Test
    void supportsOnlyStaticArtifactsOwnedByJavaAstByteApi() {
        var content = validPayload().getBytes(StandardCharsets.UTF_8);
        var client = clientWithoutServer();

        assertTrue(client.supports(artifact(content)));
        assertTrue(client.supports(artifact(
            content,
            AnalysisArtifactCategory.STATIC,
            "other-service",
            JavaAstSourceFactArtifactClient.OWNER_SERVICE,
            JavaAstSourceFactArtifactClient.RETRIEVAL_CONTRACT
        )));
        assertTrue(!client.supports(artifact(
            content,
            AnalysisArtifactCategory.RUNTIME,
            JavaAstSourceFactArtifactClient.OWNER_SERVICE,
            JavaAstSourceFactArtifactClient.OWNER_SERVICE,
            JavaAstSourceFactArtifactClient.RETRIEVAL_CONTRACT
        )));
        assertTrue(!client.supports(artifact(
            content,
            AnalysisArtifactCategory.STATIC,
            "other-service",
            "other-service",
            JavaAstSourceFactArtifactClient.RETRIEVAL_CONTRACT
        )));
        assertTrue(!client.supports(artifact(
            content,
            AnalysisArtifactCategory.STATIC,
            JavaAstSourceFactArtifactClient.OWNER_SERVICE,
            JavaAstSourceFactArtifactClient.OWNER_SERVICE,
            "other.contract"
        )));
    }

    @Test
    void rejectsArtifactsOutsideJavaAstOwnerApiBeforeGrpcCall() {
        var content = validPayload().getBytes(StandardCharsets.UTF_8);
        var client = clientWithoutServer();

        assertThrows(IllegalArgumentException.class, () -> client.verify(
            runId(),
            jobId(),
            snapshotId(),
            "request-1",
            "correlation-1",
            artifact(
                content,
                AnalysisArtifactCategory.RUNTIME,
                JavaAstSourceFactArtifactClient.OWNER_SERVICE,
                JavaAstSourceFactArtifactClient.OWNER_SERVICE,
                JavaAstSourceFactArtifactClient.RETRIEVAL_CONTRACT
            ),
            Map.of()
        ));
        assertThrows(IllegalArgumentException.class, () -> client.verify(
            runId(),
            jobId(),
            snapshotId(),
            "request-1",
            "correlation-1",
            artifact(
                content,
                AnalysisArtifactCategory.STATIC,
                "other-service",
                JavaAstSourceFactArtifactClient.OWNER_SERVICE,
                JavaAstSourceFactArtifactClient.RETRIEVAL_CONTRACT
            ),
            Map.of()
        ));
        assertThrows(IllegalArgumentException.class, () -> client.verify(
            runId(),
            jobId(),
            snapshotId(),
            "request-1",
            "correlation-1",
            artifact(
                content,
                AnalysisArtifactCategory.STATIC,
                JavaAstSourceFactArtifactClient.OWNER_SERVICE,
                JavaAstSourceFactArtifactClient.OWNER_SERVICE,
                "other.contract"
            ),
            Map.of()
        ));
    }

    @Test
    void rejectsGrpcChecksumSizeAndMetadataMismatches() throws Exception {
        var content = validPayload().getBytes(StandardCharsets.UTF_8);
        var artifact = artifact(content);
        var checksumMismatch = clientReturning(
            artifact,
            content,
            response -> response.setSha256("b".repeat(64))
        );
        assertThrows(IllegalStateException.class, () -> checksumMismatch.verify(
            runId(),
            jobId(),
            snapshotId(),
            "request-1",
            "correlation-1",
            artifact,
            Map.of()
        ));
        stopGrpc();

        var expectedContent = validPayload().getBytes(StandardCharsets.UTF_8);
        var tamperedContent = validPayload().replace("\"factId\": \"fact-1\"", "\"factId\": \"fact-2\"")
            .getBytes(StandardCharsets.UTF_8);
        var contentHashMismatchArtifact = artifact(sha256(expectedContent), expectedContent.length);
        var contentHashMismatch = clientReturning(
            contentHashMismatchArtifact,
            tamperedContent,
            response -> response
                .setSourceFactArtifact(protoArtifact(contentHashMismatchArtifact))
                .setSha256(contentHashMismatchArtifact.artifact().sha256())
                .setSizeBytes(expectedContent.length)
        );
        assertThrows(IllegalStateException.class, () -> contentHashMismatch.verify(
            runId(),
            jobId(),
            snapshotId(),
            "request-1",
            "correlation-1",
            contentHashMismatchArtifact,
            Map.of()
        ));
        stopGrpc();

        var sizeMismatch = clientReturning(
            artifact,
            content,
            response -> response.setSizeBytes(content.length + 1)
        );
        assertThrows(IllegalStateException.class, () -> sizeMismatch.verify(
            runId(),
            jobId(),
            snapshotId(),
            "request-1",
            "correlation-1",
            artifact,
            Map.of()
        ));
        stopGrpc();

        var largerContent = (validPayload() + "\n").getBytes(StandardCharsets.UTF_8);
        var contentSizeMismatchArtifact = artifact(sha256(largerContent), content.length);
        var contentSizeMismatch = clientReturning(
            contentSizeMismatchArtifact,
            largerContent,
            response -> response
                .setSourceFactArtifact(protoArtifact(contentSizeMismatchArtifact))
                .setSha256(contentSizeMismatchArtifact.artifact().sha256())
                .setSizeBytes(content.length)
        );
        assertThrows(IllegalStateException.class, () -> contentSizeMismatch.verify(
            runId(),
            jobId(),
            snapshotId(),
            "request-1",
            "correlation-1",
            contentSizeMismatchArtifact,
            Map.of()
        ));
        stopGrpc();

        var metadataMismatch = clientReturning(
            artifact,
            content,
            response -> response.setSourceFactArtifact(protoArtifact(artifact).toBuilder().setSchemaVersion("java-ast-analysis-v2"))
        );
        assertThrows(IllegalStateException.class, () -> metadataMismatch.verify(
            runId(),
            jobId(),
            snapshotId(),
            "request-1",
            "correlation-1",
            artifact,
            Map.of()
        ));
        stopGrpc();

        var producerMismatch = clientReturning(
            artifact,
            content,
            response -> response.setSourceFactArtifact(protoArtifact(artifact).toBuilder().setProducerService("other-service"))
        );
        assertThrows(IllegalStateException.class, () -> producerMismatch.verify(
            runId(),
            jobId(),
            snapshotId(),
            "request-1",
            "correlation-1",
            artifact,
            Map.of()
        ));
        stopGrpc();

        var completenessMismatch = clientReturning(
            artifact,
            content,
            response -> response.setSourceFactArtifact(protoArtifact(artifact).toBuilder()
                .setCompleteness(de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.ANALYSIS_COMPLETENESS_INCOMPLETE))
        );
        assertThrows(IllegalStateException.class, () -> completenessMismatch.verify(
            runId(),
            jobId(),
            snapshotId(),
            "request-1",
            "correlation-1",
            artifact,
            Map.of()
        ));
    }

    @Test
    void reportsGrpcStatusFailuresWithoutAcceptingBytes() throws Exception {
        var artifact = artifact(validPayload().getBytes(StandardCharsets.UTF_8));
        var client = clientFailingWith(Status.UNAVAILABLE.asRuntimeException());

        var failure = assertThrows(IllegalStateException.class, () -> client.verify(
            runId(),
            jobId(),
            snapshotId(),
            "request-1",
            "correlation-1",
            artifact,
            Map.of()
        ));

        assertEquals("Java AST source fact artifact retrieval failed with status UNAVAILABLE", failure.getMessage());
    }

    @Test
    void validatesConstructorAndRequestTextArguments() throws Exception {
        var serverName = InProcessServerBuilder.generateName();
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        var stub = JavaAstAnalysisServiceGrpc.newBlockingStub(channel);

        assertThrows(IllegalArgumentException.class, () -> new JavaAstSourceFactArtifactClient(stub, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new JavaAstSourceFactArtifactClient(stub, 1, 0));
        stopGrpc();

        var content = validPayload().getBytes(StandardCharsets.UTF_8);
        var artifact = artifact(content);
        var client = clientReturning(artifact, content);
        assertThrows(IllegalArgumentException.class, () -> client.verify(
            runId(),
            jobId(),
            snapshotId(),
            "",
            "correlation-1",
            artifact,
            Map.of()
        ));
        assertThrows(IllegalArgumentException.class, () -> client.verify(
            runId(),
            jobId(),
            snapshotId(),
            "request-1",
            "",
            artifact,
            Map.of()
        ));
    }

    private JavaAstSourceFactArtifactClient clientWithoutServer() {
        var serverName = InProcessServerBuilder.generateName();
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        return new JavaAstSourceFactArtifactClient(JavaAstAnalysisServiceGrpc.newBlockingStub(channel), 5, 1_048_576);
    }

    private JavaAstSourceFactArtifactClient clientReturning(AnalysisArtifactReference artifact, byte[] content) throws IOException {
        return clientReturning(artifact, content, UnaryOperator.identity());
    }

    private JavaAstSourceFactArtifactClient clientReturning(
        AnalysisArtifactReference artifact,
        byte[] content,
        UnaryOperator<GetSourceFactArtifactBytesResponse.Builder> responseCustomizer
    ) throws IOException {
        var serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(new JavaAstAnalysisServiceGrpc.JavaAstAnalysisServiceImplBase() {
                @Override
                public void getSourceFactArtifactBytes(
                    GetSourceFactArtifactBytesRequest request,
                    StreamObserver<GetSourceFactArtifactBytesResponse> responseObserver
                ) {
                    capturedRequest = request;
                    var response = GetSourceFactArtifactBytesResponse.newBuilder()
                        .setAnalysisRunId(de.burger.forensics.analytics.analysisjob.v1.AnalysisRunId.newBuilder()
                            .setValue(request.getAnalysisRunId().getValue()))
                        .setAnalysisJobId(de.burger.forensics.analytics.analysisjob.v1.AnalysisJobId.newBuilder()
                            .setValue(request.getAnalysisJobId().getValue()))
                        .setSourceSnapshotId(de.burger.forensics.analytics.analysisjob.v1.SourceSnapshotId.newBuilder()
                            .setValue(request.getSourceSnapshotId().getValue()))
                        .setSourceFactArtifact(protoArtifact(artifact))
                        .setContent(ByteString.copyFrom(content))
                        .setSha256(sha256(content))
                        .setSizeBytes(content.length)
                        .build();
                    responseObserver.onNext(responseCustomizer.apply(response.toBuilder()).build());
                    responseObserver.onCompleted();
                }
            })
            .build()
            .start();
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        return new JavaAstSourceFactArtifactClient(JavaAstAnalysisServiceGrpc.newBlockingStub(channel), 5, 1_048_576);
    }

    private JavaAstSourceFactArtifactClient clientFailingWith(io.grpc.StatusRuntimeException error) throws IOException {
        var serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(new JavaAstAnalysisServiceGrpc.JavaAstAnalysisServiceImplBase() {
                @Override
                public void getSourceFactArtifactBytes(
                    GetSourceFactArtifactBytesRequest request,
                    StreamObserver<GetSourceFactArtifactBytesResponse> responseObserver
                ) {
                    responseObserver.onError(error);
                }
            })
            .build()
            .start();
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        return new JavaAstSourceFactArtifactClient(JavaAstAnalysisServiceGrpc.newBlockingStub(channel), 5, 1_048_576);
    }

    private static de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactReference protoArtifact(
        AnalysisArtifactReference artifact
    ) {
        return de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactReference.newBuilder()
            .setArtifact(de.burger.forensics.analytics.analysisjob.v1.ArtifactReference.newBuilder()
                .setPath(artifact.artifact().path())
                .setType(artifact.artifact().type())
                .setSha256(artifact.artifact().sha256())
                .setSizeBytes(artifact.artifact().sizeBytes()))
            .setCategory(de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_STATIC)
            .setProducerService(artifact.producerService())
            .setSchemaVersion(artifact.schemaVersion())
            .setCompleteness(protoCompleteness(artifact.completeness()))
            .setByteAccess(de.burger.forensics.analytics.analysisjob.v1.ArtifactByteAccess.newBuilder()
                .setOwnerService(artifact.byteAccess().ownerService())
                .setRetrievalContract(artifact.byteAccess().retrievalContract())
                .setRetrievalReference(artifact.byteAccess().retrievalReference())
                .setByteCustody(de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_PRODUCER_RETAINED))
            .build();
    }

    private static de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness protoCompleteness(
        AnalysisCompleteness completeness
    ) {
        return switch (completeness) {
            case COMPLETE -> de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.ANALYSIS_COMPLETENESS_COMPLETE;
            case INCOMPLETE -> de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.ANALYSIS_COMPLETENESS_INCOMPLETE;
            case UNKNOWN -> de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.ANALYSIS_COMPLETENESS_UNKNOWN;
        };
    }

    private static AnalysisArtifactReference artifact(byte[] content) {
        return artifact(sha256(content), content.length);
    }

    private static AnalysisArtifactReference artifact(String sha256, long sizeBytes) {
        return artifact(
            sha256,
            sizeBytes,
            AnalysisArtifactCategory.STATIC,
            JavaAstSourceFactArtifactClient.OWNER_SERVICE,
            JavaAstSourceFactArtifactClient.OWNER_SERVICE,
            JavaAstSourceFactArtifactClient.RETRIEVAL_CONTRACT,
            AnalysisCompleteness.COMPLETE
        );
    }

    private static AnalysisArtifactReference artifact(
        byte[] content,
        AnalysisArtifactCategory category,
        String producerService,
        String ownerService,
        String retrievalContract
    ) {
        return artifact(
            sha256(content),
            content.length,
            category,
            producerService,
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
        return artifact(
            sha256,
            sizeBytes,
            category,
            producerService,
            ownerService,
            retrievalContract,
            completeness,
            JavaAstSourceFactArtifactPayloadParser.MEDIA_TYPE
        );
    }

    private static AnalysisArtifactReference artifact(
        String sha256,
        long sizeBytes,
        AnalysisArtifactCategory category,
        String producerService,
        String ownerService,
        String retrievalContract,
        AnalysisCompleteness completeness,
        String artifactType
    ) {
        return new AnalysisArtifactReference(
            new ArtifactReference(
                "java-ast/snapshot-1-source-facts.json",
                artifactType,
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

    private static String validPayload() {
        return """
            {
              "schemaVersion": "java-ast-analysis-v1",
              "analysisRunId": "run-1",
              "analysisJobId": "job-1",
              "sourceSnapshotId": "snapshot-1",
              "summary": {
                "receivedFileCount": 1,
                "parsedFileCount": 1,
                "skippedFileCount": 0,
                "parseErrorCount": 0,
                "sourceFactCount": 1,
                "parser": "JavaParser",
                "parserVersion": "3.27.1"
              },
              "sourceFacts": [
                {
                  "factId": "fact-1",
                  "factType": "java-method",
                  "location": {
                    "sourcePath": "src/main/java/a/A.java",
                    "fullyQualifiedClassName": "a.A",
                    "methodName": "run",
                    "lineNumber": 4,
                    "columnNumber": 9
                  },
                  "signature": "a.A#run()",
                  "summary": "AST method a.A#run()",
                  "evidenceKind": "STATIC_SOURCE_FACT"
                }
              ],
              "diagnostics": []
            }
            """;
    }

    private static AnalysisRunId runId() {
        return new AnalysisRunId("run-1");
    }

    private static AnalysisJobId jobId() {
        return new AnalysisJobId("job-1");
    }

    private static SourceSnapshotId snapshotId() {
        return new SourceSnapshotId("snapshot-1");
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
    }
}
