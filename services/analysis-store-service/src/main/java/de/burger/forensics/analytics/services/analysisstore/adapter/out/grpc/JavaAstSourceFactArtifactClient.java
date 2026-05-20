package de.burger.forensics.analytics.services.analysisstore.adapter.out.grpc;

import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobId;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisRunId;
import de.burger.forensics.analytics.analysisjob.v1.SourceSnapshotId;
import de.burger.forensics.analytics.javaastanalysis.v1.GetSourceFactArtifactBytesRequest;
import de.burger.forensics.analytics.javaastanalysis.v1.JavaAstAnalysisServiceGrpc;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactReference;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class JavaAstSourceFactArtifactClient implements AutoCloseable {
    public static final String OWNER_SERVICE = "java-ast-analysis-service";
    public static final String RETRIEVAL_CONTRACT =
        "java-ast-analysis.v1.JavaAstAnalysisService.GetSourceFactArtifactBytes";

    private final ManagedChannel channel;
    private final JavaAstAnalysisServiceGrpc.JavaAstAnalysisServiceBlockingStub stub;
    private final long deadlineSeconds;
    private final long maxBytes;

    public JavaAstSourceFactArtifactClient(String host, int port, long deadlineSeconds, long maxBytes) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext().build(), deadlineSeconds, maxBytes);
    }

    JavaAstSourceFactArtifactClient(
        JavaAstAnalysisServiceGrpc.JavaAstAnalysisServiceBlockingStub stub,
        long deadlineSeconds,
        long maxBytes
    ) {
        this.channel = null;
        this.stub = Objects.requireNonNull(stub, "stub must not be null");
        this.deadlineSeconds = requirePositive(deadlineSeconds, "deadlineSeconds");
        this.maxBytes = requirePositive(maxBytes, "maxBytes");
    }

    private JavaAstSourceFactArtifactClient(ManagedChannel channel, long deadlineSeconds, long maxBytes) {
        this.channel = Objects.requireNonNull(channel, "channel must not be null");
        this.stub = JavaAstAnalysisServiceGrpc.newBlockingStub(channel);
        this.deadlineSeconds = requirePositive(deadlineSeconds, "deadlineSeconds");
        this.maxBytes = requirePositive(maxBytes, "maxBytes");
    }

    public SourceFactArtifactBytes read(
        de.burger.forensics.analytics.services.analysisstore.domain.AnalysisRunId analysisRunId,
        de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJobId analysisJobId,
        de.burger.forensics.analytics.services.analysisstore.domain.SourceSnapshotId sourceSnapshotId,
        String requestId,
        String correlationId,
        AnalysisArtifactReference artifact,
        Map<String, String> safeAttributes
    ) {
        Objects.requireNonNull(analysisRunId, "analysisRunId must not be null");
        Objects.requireNonNull(analysisJobId, "analysisJobId must not be null");
        Objects.requireNonNull(sourceSnapshotId, "sourceSnapshotId must not be null");
        var artifactReference = Objects.requireNonNull(artifact, "artifact must not be null");
        requireJavaAstOwnerApi(artifactReference);
        try {
            var response = stub
                .withDeadlineAfter(deadlineSeconds, TimeUnit.SECONDS)
                .getSourceFactArtifactBytes(request(
                    analysisRunId,
                    analysisJobId,
                    sourceSnapshotId,
                    requestId,
                    correlationId,
                    artifactReference,
                    safeAttributes
                ));
            if (!artifactReference.artifact().sha256().equals(response.getSha256())) {
                throw new IllegalStateException("Java AST source fact artifact checksum mismatch");
            }
            if (artifactReference.artifact().sizeBytes() != response.getSizeBytes()
                || artifactReference.artifact().sizeBytes() != response.getContent().size()) {
                throw new IllegalStateException("Java AST source fact artifact size mismatch");
            }
            return new SourceFactArtifactBytes(artifactReference, response.getContent().toByteArray());
        } catch (StatusRuntimeException error) {
            throw new IllegalStateException("Java AST source fact artifact retrieval failed with status "
                + error.getStatus().getCode());
        }
    }

    private GetSourceFactArtifactBytesRequest request(
        de.burger.forensics.analytics.services.analysisstore.domain.AnalysisRunId analysisRunId,
        de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJobId analysisJobId,
        de.burger.forensics.analytics.services.analysisstore.domain.SourceSnapshotId sourceSnapshotId,
        String requestId,
        String correlationId,
        AnalysisArtifactReference artifact,
        Map<String, String> safeAttributes
    ) {
        return GetSourceFactArtifactBytesRequest.newBuilder()
            .setRequestId(requireText(requestId, "requestId"))
            .setCorrelationId(requireText(correlationId, "correlationId"))
            .setAnalysisRunId(AnalysisRunId.newBuilder().setValue(analysisRunId.value()))
            .setAnalysisJobId(AnalysisJobId.newBuilder().setValue(analysisJobId.value()))
            .setSourceSnapshotId(SourceSnapshotId.newBuilder().setValue(sourceSnapshotId.value()))
            .setRetrievalReference(artifact.byteAccess().retrievalReference())
            .setExpectedSha256(artifact.artifact().sha256())
            .setExpectedSizeBytes(artifact.artifact().sizeBytes())
            .setMaxBytes(maxBytes)
            .setSchemaVersion(artifact.schemaVersion())
            .putAllSafeAttributes(Map.copyOf(Objects.requireNonNullElse(safeAttributes, Map.of())))
            .build();
    }

    private static void requireJavaAstOwnerApi(AnalysisArtifactReference artifact) {
        if (!OWNER_SERVICE.equals(artifact.byteAccess().ownerService())) {
            throw new IllegalArgumentException("source fact artifact bytes must be owned by Java AST Analysis");
        }
        if (!RETRIEVAL_CONTRACT.equals(artifact.byteAccess().retrievalContract())) {
            throw new IllegalArgumentException("source fact artifact retrieval contract is not the Java AST owner API");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static long requirePositive(long value, String fieldName) {
        if (value < 1) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    @Override
    public void close() {
        if (channel != null) {
            channel.shutdownNow();
        }
    }

    public record SourceFactArtifactBytes(AnalysisArtifactReference artifact, byte[] content) {
        public SourceFactArtifactBytes {
            Objects.requireNonNull(artifact, "artifact must not be null");
            content = Objects.requireNonNull(content, "content must not be null").clone();
        }

        @Override
        public byte[] content() {
            return content.clone();
        }
    }
}
