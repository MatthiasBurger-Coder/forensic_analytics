package de.burger.forensics.analytics.services.joerncpganalysis.adapter.out.grpc;

import de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactReference;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobId;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobServiceGrpc;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisRunId;
import de.burger.forensics.analytics.analysisjob.v1.ArtifactByteAccess;
import de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody;
import de.burger.forensics.analytics.analysisjob.v1.ArtifactReference;
import de.burger.forensics.analytics.analysisjob.v1.RegisterAnalysisArtifactsRequest;
import de.burger.forensics.analytics.services.joerncpganalysis.application.port.AnalysisStoreArtifactRegistryPort;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalyzeJoernCpgResult;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class AnalysisStoreArtifactRegistryGrpcClient implements AnalysisStoreArtifactRegistryPort, AutoCloseable {
    private static final String ACCEPTED = "ACCEPTED";

    private final AnalysisJobServiceGrpc.AnalysisJobServiceBlockingStub stub;
    private final ManagedChannel ownedChannel;
    private final long deadlineSeconds;

    public AnalysisStoreArtifactRegistryGrpcClient(String host, int port, long deadlineSeconds) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext().build(), deadlineSeconds);
    }

    AnalysisStoreArtifactRegistryGrpcClient(ManagedChannel channel, long deadlineSeconds) {
        this(AnalysisJobServiceGrpc.newBlockingStub(Objects.requireNonNull(channel, "channel must not be null")), channel, deadlineSeconds);
    }

    AnalysisStoreArtifactRegistryGrpcClient(
        AnalysisJobServiceGrpc.AnalysisJobServiceBlockingStub stub,
        long deadlineSeconds
    ) {
        this(stub, null, deadlineSeconds);
    }

    private AnalysisStoreArtifactRegistryGrpcClient(
        AnalysisJobServiceGrpc.AnalysisJobServiceBlockingStub stub,
        ManagedChannel ownedChannel,
        long deadlineSeconds
    ) {
        this.stub = Objects.requireNonNull(stub, "analysis store stub must not be null");
        this.ownedChannel = ownedChannel;
        if (deadlineSeconds < 1 || deadlineSeconds > 86_400) {
            throw new IllegalArgumentException("analysis store deadline seconds must be between 1 and 86400");
        }
        this.deadlineSeconds = deadlineSeconds;
    }

    @Override
    public void registerSemanticArtifacts(AnalyzeJoernCpgResult result) {
        var verifiedResult = Objects.requireNonNull(result, "analysis result must not be null");
        try {
            var response = stub.withDeadlineAfter(deadlineSeconds, TimeUnit.SECONDS)
                .registerAnalysisArtifacts(request(verifiedResult));
            if (!ACCEPTED.equals(response.getStatus().getCode())) {
                throw new IllegalStateException(
                    "Analysis Store artifact registration failed with status " + response.getStatus().getCode()
                );
            }
        } catch (StatusRuntimeException error) {
            throw new IllegalStateException(
                "Analysis Store artifact registration failed with status " + error.getStatus().getCode(),
                error
            );
        }
    }

    @Override
    public void close() {
        if (ownedChannel != null) {
            ownedChannel.shutdown();
        }
    }

    private static RegisterAnalysisArtifactsRequest request(AnalyzeJoernCpgResult result) {
        var metadata = result.metadata();
        var builder = RegisterAnalysisArtifactsRequest.newBuilder()
            .setRequestId(metadata.requestId() + "-semantic-artifacts")
            .setIdempotencyKey(metadata.idempotencyKey() + ":semantic-artifacts")
            .setCorrelationId(metadata.correlationId())
            .setAnalysisRunId(AnalysisRunId.newBuilder().setValue(metadata.analysisRunId().value()))
            .setJobId(AnalysisJobId.newBuilder().setValue(metadata.analysisJobId().value()));
        result.semanticArtifacts().forEach(reference -> builder.addArtifacts(artifact(reference)));
        return builder.build();
    }

    private static AnalysisArtifactReference artifact(
        de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisArtifactReference reference
    ) {
        return AnalysisArtifactReference.newBuilder()
            .setArtifact(ArtifactReference.newBuilder()
                .setPath(reference.artifact().path())
                .setType(reference.artifact().type())
                .setSha256(reference.artifact().sha256())
                .setSizeBytes(reference.artifact().sizeBytes()))
            .setCategory(AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_STATIC)
            .setProducerService(reference.producerService())
            .setSchemaVersion(reference.schemaVersion())
            .setCompleteness(completeness(reference.completeness()))
            .setByteAccess(byteAccess(reference.byteAccess()))
            .build();
    }

    private static de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness completeness(
        de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisCompleteness completeness
    ) {
        return switch (completeness) {
            case COMPLETE -> de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.ANALYSIS_COMPLETENESS_COMPLETE;
            case INCOMPLETE -> de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.ANALYSIS_COMPLETENESS_INCOMPLETE;
            case UNKNOWN -> de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.ANALYSIS_COMPLETENESS_UNKNOWN;
        };
    }

    private static ArtifactByteAccess byteAccess(
        de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.ArtifactByteAccess byteAccess
    ) {
        return ArtifactByteAccess.newBuilder()
            .setOwnerService(byteAccess.ownerService())
            .setRetrievalContract(byteAccess.retrievalContract())
            .setRetrievalReference(byteAccess.retrievalReference())
            .setByteCustody(byteCustody(byteAccess.byteCustody()))
            .build();
    }

    private static ArtifactByteCustody byteCustody(
        de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.ArtifactByteCustody custody
    ) {
        return switch (custody) {
            case PRODUCER_RETAINED -> ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_PRODUCER_RETAINED;
            case SCOPED_OBJECT_ACCESS -> ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_SCOPED_OBJECT_ACCESS;
            case EXPLICIT_HANDOFF -> ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_EXPLICIT_HANDOFF;
        };
    }
}
