package de.burger.forensics.analytics.services.analysisstore.adapter.out.grpc;

import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobId;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisRunId;
import de.burger.forensics.analytics.analysisjob.v1.SourceSnapshotId;
import de.burger.forensics.analytics.javaastanalysis.v1.GetSourceFactArtifactBytesRequest;
import de.burger.forensics.analytics.javaastanalysis.v1.JavaAstAnalysisServiceGrpc;
import de.burger.forensics.analytics.services.analysisstore.application.port.EvidenceArtifactIntegrityException;
import de.burger.forensics.analytics.services.analysisstore.application.port.SourceFactArtifactByteVerifierPort;
import de.burger.forensics.analytics.services.analysisstore.application.port.SourceFactArtifactReaderPort;
import de.burger.forensics.analytics.services.analysisstore.application.port.WorkerOwnerApiUnavailableException;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactCategory;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.analysisstore.domain.ArtifactByteAccess;
import de.burger.forensics.analytics.services.analysisstore.domain.ArtifactByteCustody;
import de.burger.forensics.analytics.services.analysisstore.domain.ArtifactReference;
import de.burger.forensics.analytics.services.analysisstore.adapter.out.javaast.JavaAstSourceFactArtifactPayloadParser;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class JavaAstSourceFactArtifactClient
    implements SourceFactArtifactByteVerifierPort, SourceFactArtifactReaderPort, AutoCloseable {
    public static final String OWNER_SERVICE = "java-ast-analysis-service";
    public static final String RETRIEVAL_CONTRACT =
        "java-ast-analysis.v1.JavaAstAnalysisService.GetSourceFactArtifactBytes";
    private static final Map<AnalysisCompleteness, de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness>
        PROTO_COMPLETENESS = Map.of(
            AnalysisCompleteness.COMPLETE,
            de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.ANALYSIS_COMPLETENESS_COMPLETE,
            AnalysisCompleteness.INCOMPLETE,
            de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.ANALYSIS_COMPLETENESS_INCOMPLETE,
            AnalysisCompleteness.UNKNOWN,
            de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.ANALYSIS_COMPLETENESS_UNKNOWN
        );
    private static final Map<de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness, AnalysisCompleteness>
        DOMAIN_COMPLETENESS = Map.of(
            de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.ANALYSIS_COMPLETENESS_COMPLETE,
            AnalysisCompleteness.COMPLETE,
            de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.ANALYSIS_COMPLETENESS_INCOMPLETE,
            AnalysisCompleteness.INCOMPLETE,
            de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.ANALYSIS_COMPLETENESS_UNKNOWN,
            AnalysisCompleteness.UNKNOWN,
            de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.ANALYSIS_COMPLETENESS_UNSPECIFIED,
            AnalysisCompleteness.UNKNOWN
        );
    private static final Map<ArtifactByteCustody, de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody>
        PROTO_BYTE_CUSTODY = Map.of(
            ArtifactByteCustody.PRODUCER_RETAINED,
            de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_PRODUCER_RETAINED,
            ArtifactByteCustody.SCOPED_OBJECT_ACCESS,
            de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_SCOPED_OBJECT_ACCESS,
            ArtifactByteCustody.EXPLICIT_HANDOFF,
            de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_EXPLICIT_HANDOFF
        );
    private static final Map<de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody, ArtifactByteCustody>
        DOMAIN_BYTE_CUSTODY = Map.of(
            de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_PRODUCER_RETAINED,
            ArtifactByteCustody.PRODUCER_RETAINED,
            de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_SCOPED_OBJECT_ACCESS,
            ArtifactByteCustody.SCOPED_OBJECT_ACCESS,
            de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_EXPLICIT_HANDOFF,
            ArtifactByteCustody.EXPLICIT_HANDOFF
        );

    private final ManagedChannel channel;
    private final JavaAstAnalysisServiceGrpc.JavaAstAnalysisServiceBlockingStub stub;
    private final JavaAstSourceFactArtifactPayloadParser sourceFactPayloadParser;
    private final long deadlineSeconds;
    private final long maxBytes;

    public JavaAstSourceFactArtifactClient(String host, int port, long deadlineSeconds, long maxBytes) {
        this(ManagedChannelBuilder
            .forAddress(TrustedPlaintextGrpcTargets.requireTrustedHost(host, "java ast analysis gRPC host"), port)
            .usePlaintext()
            .build(), deadlineSeconds, maxBytes);
    }

    JavaAstSourceFactArtifactClient(
        JavaAstAnalysisServiceGrpc.JavaAstAnalysisServiceBlockingStub stub,
        long deadlineSeconds,
        long maxBytes
    ) {
        this.channel = null;
        this.stub = Objects.requireNonNull(stub, "stub must not be null");
        this.sourceFactPayloadParser = new JavaAstSourceFactArtifactPayloadParser();
        this.deadlineSeconds = requirePositive(deadlineSeconds, "deadlineSeconds");
        this.maxBytes = requirePositive(maxBytes, "maxBytes");
    }

    private JavaAstSourceFactArtifactClient(ManagedChannel channel, long deadlineSeconds, long maxBytes) {
        this.channel = Objects.requireNonNull(channel, "channel must not be null");
        this.stub = JavaAstAnalysisServiceGrpc.newBlockingStub(channel);
        this.sourceFactPayloadParser = new JavaAstSourceFactArtifactPayloadParser();
        this.deadlineSeconds = requirePositive(deadlineSeconds, "deadlineSeconds");
        this.maxBytes = requirePositive(maxBytes, "maxBytes");
    }

    @Override
    public boolean supports(AnalysisArtifactReference artifact) {
        return artifact.category() == AnalysisArtifactCategory.STATIC
            && OWNER_SERVICE.equals(artifact.byteAccess().ownerService())
            && RETRIEVAL_CONTRACT.equals(artifact.byteAccess().retrievalContract());
    }

    @Override
    public AnalysisArtifactReference verify(
        de.burger.forensics.analytics.services.analysisstore.domain.AnalysisRunId analysisRunId,
        de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJobId analysisJobId,
        de.burger.forensics.analytics.services.analysisstore.domain.SourceSnapshotId sourceSnapshotId,
        String requestId,
        String correlationId,
        AnalysisArtifactReference artifact,
        Map<String, String> safeAttributes
    ) {
        return read(analysisRunId, analysisJobId, sourceSnapshotId, requestId, correlationId, artifact, safeAttributes)
            .artifact();
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
            var content = response.getContent().toByteArray();
            var contentSha256 = sha256(content);
            if (!artifactReference.artifact().sha256().equals(response.getSha256())
                || !artifactReference.artifact().sha256().equals(contentSha256)) {
                throw new EvidenceArtifactIntegrityException(
                    "JAVA_AST_SOURCE_FACT_CHECKSUM_MISMATCH",
                    "Java AST source fact artifact checksum verification failed."
                );
            }
            if (artifactReference.artifact().sizeBytes() != response.getSizeBytes()
                || artifactReference.artifact().sizeBytes() != content.length) {
                throw new EvidenceArtifactIntegrityException(
                    "JAVA_AST_SOURCE_FACT_SIZE_MISMATCH",
                    "Java AST source fact artifact size verification failed."
                );
            }
            requireVerifiedArtifactMetadata(artifactReference, response.getSourceFactArtifact());
            var canonicalArtifact = canonicalArtifact(response.getSourceFactArtifact());
            requireContractPayload(analysisRunId, analysisJobId, sourceSnapshotId, canonicalArtifact, content);
            return new SourceFactArtifactBytes(canonicalArtifact, content);
        } catch (StatusRuntimeException error) {
            throw new WorkerOwnerApiUnavailableException(
                "Java AST source fact artifact reader",
                error.getStatus().getCode().name()
            );
        }
    }

    @Override
    public SourceFactArtifact readFacts(
        de.burger.forensics.analytics.services.analysisstore.domain.AnalysisRunId analysisRunId,
        de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJobId analysisJobId,
        de.burger.forensics.analytics.services.analysisstore.domain.SourceSnapshotId sourceSnapshotId,
        String requestId,
        String correlationId,
        AnalysisArtifactReference artifact,
        Map<String, String> safeAttributes
    ) {
        var bytes = read(analysisRunId, analysisJobId, sourceSnapshotId, requestId, correlationId, artifact, safeAttributes);
        var parsed = sourceFactPayloadParser.parse(
            analysisRunId,
            analysisJobId,
            sourceSnapshotId,
            bytes.artifact(),
            bytes.content()
        );
        return new SourceFactArtifact(
            bytes.artifact(),
            parsed.facts(),
            parsed.completeness(),
            parsed.diagnostics().stream()
                .map(diagnostic -> new SourceFactDiagnostic(
                    diagnostic.code(),
                    diagnostic.message(),
                    diagnostic.affectsCompleteness()
                ))
                .toList()
        );
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
        if (artifact.category() != AnalysisArtifactCategory.STATIC) {
            throw new IllegalArgumentException("source fact artifact must be static");
        }
        if (!OWNER_SERVICE.equals(artifact.producerService())
            || !OWNER_SERVICE.equals(artifact.byteAccess().ownerService())) {
            throw new IllegalArgumentException("source fact artifact bytes must be owned by Java AST Analysis");
        }
        if (!RETRIEVAL_CONTRACT.equals(artifact.byteAccess().retrievalContract())) {
            throw new IllegalArgumentException("source fact artifact retrieval contract is not the Java AST owner API");
        }
    }

    private static void requireVerifiedArtifactMetadata(
        AnalysisArtifactReference expected,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactReference actual
    ) {
        if (!metadataSignature(expected).equals(metadataSignature(actual))) {
            throw new EvidenceArtifactIntegrityException(
                "JAVA_AST_SOURCE_FACT_METADATA_MISMATCH",
                "Java AST source fact artifact metadata verification failed."
            );
        }
    }

    private void requireContractPayload(
        de.burger.forensics.analytics.services.analysisstore.domain.AnalysisRunId analysisRunId,
        de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJobId analysisJobId,
        de.burger.forensics.analytics.services.analysisstore.domain.SourceSnapshotId sourceSnapshotId,
        AnalysisArtifactReference artifact,
        byte[] content
    ) {
        var parsed = sourceFactPayloadParser.parse(analysisRunId, analysisJobId, sourceSnapshotId, artifact, content);
        var violations = parsed.diagnostics().stream()
            .filter(JavaAstSourceFactArtifactClient::isPayloadContractViolation)
            .toList();
        if (!violations.isEmpty()) {
            throw new EvidenceArtifactIntegrityException(
                "JAVA_AST_SOURCE_FACT_PAYLOAD_CONTRACT_VIOLATION",
                "Java AST source fact artifact payload violates the v1 contract."
            );
        }
        if (parsed.completeness() != artifact.completeness()) {
            throw new EvidenceArtifactIntegrityException(
                "JAVA_AST_SOURCE_FACT_COMPLETENESS_MISMATCH",
                "Java AST source fact artifact payload completeness does not match metadata."
            );
        }
    }

    private static boolean isPayloadContractViolation(JavaAstSourceFactArtifactPayloadParser.SourceFactPayloadDiagnostic diagnostic) {
        return diagnostic.code().startsWith("SOURCE_FACT_ARTIFACT")
            || diagnostic.code().startsWith("UNSUPPORTED_STATIC_");
    }

    private static List<String> metadataSignature(AnalysisArtifactReference artifact) {
        return List.of(
            de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_STATIC.name(),
            artifact.producerService(),
            artifact.artifact().path(),
            artifact.artifact().type(),
            artifact.artifact().sha256(),
            Long.toString(artifact.artifact().sizeBytes()),
            artifact.schemaVersion(),
            completeness(artifact.completeness()).name(),
            artifact.byteAccess().ownerService(),
            artifact.byteAccess().retrievalContract(),
            artifact.byteAccess().retrievalReference(),
            byteCustody(artifact.byteAccess().byteCustody()).name()
        );
    }

    private static List<String> metadataSignature(
        de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactReference artifact
    ) {
        return List.of(
            artifact.getCategory().name(),
            artifact.getProducerService(),
            artifact.getArtifact().getPath(),
            artifact.getArtifact().getType(),
            artifact.getArtifact().getSha256(),
            Long.toString(artifact.getArtifact().getSizeBytes()),
            artifact.getSchemaVersion(),
            artifact.getCompleteness().name(),
            artifact.getByteAccess().getOwnerService(),
            artifact.getByteAccess().getRetrievalContract(),
            artifact.getByteAccess().getRetrievalReference(),
            artifact.getByteAccess().getByteCustody().name()
        );
    }

    private static AnalysisArtifactReference canonicalArtifact(
        de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactReference artifact
    ) {
        return new AnalysisArtifactReference(
            new ArtifactReference(
                artifact.getArtifact().getPath(),
                artifact.getArtifact().getType(),
                artifact.getArtifact().getSha256(),
                artifact.getArtifact().getSizeBytes()
            ),
            AnalysisArtifactCategory.STATIC,
            artifact.getProducerService(),
            artifact.getSchemaVersion(),
            completeness(artifact.getCompleteness()),
            new ArtifactByteAccess(
                artifact.getByteAccess().getOwnerService(),
                artifact.getByteAccess().getRetrievalContract(),
                artifact.getByteAccess().getRetrievalReference(),
                byteCustody(artifact.getByteAccess().getByteCustody())
            )
        );
    }

    private static de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness completeness(
        AnalysisCompleteness completeness
    ) {
        return PROTO_COMPLETENESS.get(completeness);
    }

    private static AnalysisCompleteness completeness(
        de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness completeness
    ) {
        return DOMAIN_COMPLETENESS.getOrDefault(completeness, AnalysisCompleteness.UNKNOWN);
    }

    private static de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody byteCustody(
        ArtifactByteCustody custody
    ) {
        return PROTO_BYTE_CUSTODY.get(custody);
    }

    private static ArtifactByteCustody byteCustody(
        de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody custody
    ) {
        return java.util.Optional.ofNullable(DOMAIN_BYTE_CUSTODY.get(custody))
            .orElseThrow(() -> new IllegalStateException("Java AST source fact artifact byte custody must be specified"));
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

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
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
