package de.burger.forensics.analytics.services.btmgeneration.adapter.in.grpc;

import com.google.protobuf.ByteString;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactReference;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobId;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisRunId;
import de.burger.forensics.analytics.analysisjob.v1.ArtifactByteAccess;
import de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody;
import de.burger.forensics.analytics.analysisjob.v1.ArtifactReference;
import de.burger.forensics.analytics.analysisjob.v1.SourceSnapshotId;
import de.burger.forensics.analytics.btmgeneration.v1.BtmArtifactChunk;
import de.burger.forensics.analytics.btmgeneration.v1.BtmArtifactDeliveryMessage;
import de.burger.forensics.analytics.btmgeneration.v1.BtmArtifactDeliveryServiceGrpc;
import de.burger.forensics.analytics.btmgeneration.v1.BtmArtifactDescriptor;
import de.burger.forensics.analytics.btmgeneration.v1.BtmArtifactKind;
import de.burger.forensics.analytics.btmgeneration.v1.BtmArtifactManifest;
import de.burger.forensics.analytics.btmgeneration.v1.DownloadBtmArtifactsRequest;
import de.burger.forensics.analytics.btmgeneration.v1.InstrumentationTargetSelection;
import de.burger.forensics.analytics.btmgeneration.v1.OperationStatus;
import de.burger.forensics.analytics.btmgeneration.v1.ReproducibilityMetadata;
import de.burger.forensics.analytics.services.btmgeneration.application.BtmArtifactDeliveryApplicationService;
import de.burger.forensics.analytics.services.btmgeneration.application.BtmArtifactDeliveryException;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.BtmArtifactDeliveryCommand;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.BtmArtifactDeliveryMetadata;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.BtmArtifactDeliveryPlan;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ReadableBtmArtifact;
import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;

import static de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.sha256;

public final class BtmArtifactDeliveryGrpcEndpoint extends BtmArtifactDeliveryServiceGrpc.BtmArtifactDeliveryServiceImplBase {
    private static final Map<AnalysisCompleteness, de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisCompleteness> COMPLETENESS_TO_DOMAIN =
        Map.of(
            AnalysisCompleteness.ANALYSIS_COMPLETENESS_COMPLETE,
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisCompleteness.COMPLETE,
            AnalysisCompleteness.ANALYSIS_COMPLETENESS_INCOMPLETE,
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisCompleteness.INCOMPLETE,
            AnalysisCompleteness.ANALYSIS_COMPLETENESS_UNKNOWN,
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisCompleteness.UNKNOWN
        );
    private static final Map<de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisCompleteness, AnalysisCompleteness> COMPLETENESS_FROM_DOMAIN =
        Map.of(
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisCompleteness.COMPLETE,
            AnalysisCompleteness.ANALYSIS_COMPLETENESS_COMPLETE,
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisCompleteness.INCOMPLETE,
            AnalysisCompleteness.ANALYSIS_COMPLETENESS_INCOMPLETE,
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisCompleteness.UNKNOWN,
            AnalysisCompleteness.ANALYSIS_COMPLETENESS_UNKNOWN
        );
    private static final Map<AnalysisArtifactCategory, de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisArtifactCategory> CATEGORY_TO_DOMAIN =
        Map.of(
            AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_STATIC,
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisArtifactCategory.STATIC,
            AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_RUNTIME,
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisArtifactCategory.RUNTIME,
            AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_PROJECTION,
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisArtifactCategory.PROJECTION,
            AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_GENERATED,
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisArtifactCategory.GENERATED
        );
    private static final Map<ArtifactByteCustody, de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ArtifactByteCustody> BYTE_CUSTODY_TO_DOMAIN =
        Map.of(
            ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_PRODUCER_RETAINED,
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ArtifactByteCustody.PRODUCER_RETAINED,
            ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_SCOPED_OBJECT_ACCESS,
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ArtifactByteCustody.SCOPED_OBJECT_ACCESS,
            ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_EXPLICIT_HANDOFF,
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ArtifactByteCustody.EXPLICIT_HANDOFF
        );

    private final BtmArtifactDeliveryApplicationService applicationService;

    public BtmArtifactDeliveryGrpcEndpoint(BtmArtifactDeliveryApplicationService applicationService) {
        this.applicationService = Objects.requireNonNull(applicationService, "application service must not be null");
    }

    @Override
    public void downloadBtmArtifacts(
        DownloadBtmArtifactsRequest request,
        StreamObserver<BtmArtifactDeliveryMessage> responseObserver
    ) {
        try {
            var serverObserver = serverObserver(responseObserver);
            if (serverObserver != null) {
                new DeliverySession(request, serverObserver).start();
                return;
            }
            var plan = applicationService.prepare(command(request));
            responseObserver.onNext(BtmArtifactDeliveryMessage.newBuilder()
                .setManifest(manifest(plan))
                .build());
            for (var artifact : plan.artifacts()) {
                streamArtifact(plan, artifact, responseObserver);
            }
            responseObserver.onNext(BtmArtifactDeliveryMessage.newBuilder()
                .setStatus(OperationStatus.newBuilder()
                    .setCode("DELIVERED")
                    .setMessage("BTM artifact delivery completed")
                    .setRetryable(false)
                    .setCorrelationId(request.getCorrelationId()))
                .build());
            responseObserver.onCompleted();
        } catch (RuntimeException error) {
            responseObserver.onError(status(error).asRuntimeException());
        }
    }

    private static BtmArtifactDeliveryCommand command(DownloadBtmArtifactsRequest request) {
        return new BtmArtifactDeliveryCommand(
            new BtmArtifactDeliveryMetadata(
                request.getRequestId(),
                request.getIdempotencyKey(),
                request.getSchemaVersion(),
                request.getCorrelationId(),
                new de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisRunId(
                    request.getAnalysisRunId().getValue()
                ),
                new de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisJobId(
                    request.getAnalysisJobId().getValue()
                ),
                request.getSafeAttributesMap()
            ),
            request.getMaxChunkBytes(),
            request.getMaxTotalBytes(),
            request.getArtifactReferencesList(),
            request.getAcceptedGeneratedArtifactsList().stream().map(BtmArtifactDeliveryGrpcEndpoint::artifact).toList()
        );
    }

    private static de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisArtifactReference artifact(
        AnalysisArtifactReference reference
    ) {
        if (!reference.hasByteAccess()) {
            throw new IllegalArgumentException("artifact byte access is required");
        }
        return new de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisArtifactReference(
            new de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ArtifactReference(
                reference.getArtifact().getPath(),
                reference.getArtifact().getType(),
                reference.getArtifact().getSha256(),
                reference.getArtifact().getSizeBytes()
            ),
            CATEGORY_TO_DOMAIN.getOrDefault(
                reference.getCategory(),
                de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisArtifactCategory.UNKNOWN
            ),
            reference.getProducerService(),
            reference.getSchemaVersion(),
            COMPLETENESS_TO_DOMAIN.getOrDefault(
                reference.getCompleteness(),
                de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisCompleteness.UNKNOWN
            ),
            new de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ArtifactByteAccess(
                reference.getByteAccess().getOwnerService(),
                reference.getByteAccess().getRetrievalContract(),
                reference.getByteAccess().getRetrievalReference(),
                BYTE_CUSTODY_TO_DOMAIN.getOrDefault(
                    reference.getByteAccess().getByteCustody(),
                    de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ArtifactByteCustody.UNKNOWN
                )
            )
        );
    }

    private static BtmArtifactManifest manifest(BtmArtifactDeliveryPlan plan) {
        var manifest = plan.manifest();
        var builder = BtmArtifactManifest.newBuilder()
            .setManifestId(manifest.manifestId())
            .setAnalysisRunId(AnalysisRunId.newBuilder().setValue(manifest.analysisRunId().value()))
            .setAnalysisJobId(AnalysisJobId.newBuilder().setValue(manifest.analysisJobId().value()))
            .setSourceSnapshotId(SourceSnapshotId.newBuilder().setValue(manifest.sourceSnapshotId().value()))
            .setCompleteness(COMPLETENESS_FROM_DOMAIN.get(manifest.completeness()))
            .setTotalSizeBytes(manifest.totalSizeBytes())
            .setManifestSha256(manifest.manifestSha256())
            .setDeliveryOrder(manifest.deliveryOrder())
            .setReproducibility(reproducibility(manifest.reproducibility()))
            .setTargetSelection(targetSelection(manifest.targetSelection()));
        manifest.artifacts().forEach(descriptor -> builder.addArtifacts(descriptor(descriptor)));
        return builder.build();
    }

    private static BtmArtifactDescriptor descriptor(
        de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.BtmArtifactDescriptor descriptor
    ) {
        return BtmArtifactDescriptor.newBuilder()
            .setArtifactReference(descriptor.artifactReference())
            .setArtifactKind(kind(descriptor.artifactKind()))
            .setRelativePath(descriptor.relativePath())
            .setSha256(descriptor.sha256())
            .setSizeBytes(descriptor.sizeBytes())
            .setChunkCount(descriptor.chunkCount())
            .setContentType(descriptor.contentType())
            .build();
    }

    private static ReproducibilityMetadata reproducibility(
        de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ReproducibilityMetadata metadata
    ) {
        return ReproducibilityMetadata.newBuilder()
            .setFactsFingerprint(metadata.factsFingerprint())
            .setPolicyFingerprint(metadata.policyFingerprint())
            .setGenerationFingerprint(metadata.generationFingerprint())
            .setGeneratorVersion(metadata.generatorVersion())
            .setDeterministicSort(metadata.deterministicSort())
            .build();
    }

    private static InstrumentationTargetSelection targetSelection(
        de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.TargetSelection selection
    ) {
        return InstrumentationTargetSelection.newBuilder()
            .setSelectionId(selection.selectionId())
            .setOwnerService(selection.ownerService())
            .setPolicyVersion(selection.policyVersion())
            .setSelectionFingerprint(selection.selectionFingerprint())
            .setCompleteness(COMPLETENESS_FROM_DOMAIN.get(selection.completeness()))
            .setDeterministicOrder(selection.deterministicOrder())
            .setCorrelationId(selection.correlationId())
            .setTargetCount(selection.targetCount())
            .build();
    }

    private void streamArtifact(
        BtmArtifactDeliveryPlan plan,
        ReadableBtmArtifact artifact,
        StreamObserver<BtmArtifactDeliveryMessage> responseObserver
    ) {
        var chunkIndex = 0;
        var offset = 0L;
        var buffer = new byte[plan.chunkBytes()];
        var digest = sha256Digest();
        try (var inputStream = applicationService.open(artifact)) {
            for (var read = inputStream.read(buffer); read != -1; read = inputStream.read(buffer)) {
                digest.update(buffer, 0, read);
                var data = ByteString.copyFrom(buffer, 0, read);
                responseObserver.onNext(BtmArtifactDeliveryMessage.newBuilder()
                    .setChunk(BtmArtifactChunk.newBuilder()
                        .setArtifactReference(artifact.reference().artifact().path())
                        .setArtifactKind(kind(artifact.kind()))
                        .setRelativePath(artifact.reference().artifact().path())
                        .setChunkIndex(chunkIndex)
                        .setByteOffset(offset)
                        .setData(data)
                        .setChunkSha256(sha256(data.toByteArray()))
                        .setFinalChunk(offset + read >= artifact.reference().artifact().sizeBytes()))
                    .build());
                offset += read;
                chunkIndex++;
            }
        } catch (IOException error) {
            throw new BtmArtifactDeliveryException(
                BtmArtifactDeliveryException.Reason.FAILED_PRECONDITION,
                "Failed to stream accepted BTM artifact.",
                error
            );
        }
        if (offset != artifact.reference().artifact().sizeBytes()) {
            throw new BtmArtifactDeliveryException(
                BtmArtifactDeliveryException.Reason.FAILED_PRECONDITION,
                "BTM artifact size changed during delivery."
            );
        }
        if (!HexFormat.of().formatHex(digest.digest()).equals(artifact.reference().artifact().sha256())) {
            throw new BtmArtifactDeliveryException(
                BtmArtifactDeliveryException.Reason.FAILED_PRECONDITION,
                "BTM artifact checksum changed during delivery."
            );
        }
    }

    @SuppressWarnings("unchecked")
    private static ServerCallStreamObserver<BtmArtifactDeliveryMessage> serverObserver(
        StreamObserver<BtmArtifactDeliveryMessage> responseObserver
    ) {
        if (responseObserver instanceof ServerCallStreamObserver<?>) {
            return (ServerCallStreamObserver<BtmArtifactDeliveryMessage>) responseObserver;
        }
        return null;
    }

    private static MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
    }

    private final class DeliverySession {
        private final DownloadBtmArtifactsRequest request;
        private final ServerCallStreamObserver<BtmArtifactDeliveryMessage> observer;
        private BtmArtifactDeliveryPlan plan;
        private byte[] buffer;
        private int phase;
        private int artifactIndex;
        private ReadableBtmArtifact currentArtifact;
        private java.io.InputStream currentStream;
        private MessageDigest currentDigest;
        private int chunkIndex;
        private long offset;

        private DeliverySession(
            DownloadBtmArtifactsRequest request,
            ServerCallStreamObserver<BtmArtifactDeliveryMessage> observer
        ) {
            this.request = request;
            this.observer = observer;
        }

        private void start() {
            observer.setOnCancelHandler(this::cancel);
            observer.setOnReadyHandler(this::drain);
            drain();
        }

        private synchronized void drain() {
            try {
                while (!observer.isCancelled() && observer.isReady() && phase < 3) {
                    switch (phase) {
                        case 0 -> sendManifest();
                        case 1 -> streamNextChunk();
                        case 2 -> sendStatus();
                        default -> phase = 3;
                    }
                }
            } catch (RuntimeException error) {
                fail(error);
            }
        }

        private void sendManifest() {
            plan = applicationService.prepare(command(request));
            buffer = new byte[plan.chunkBytes()];
            observer.onNext(BtmArtifactDeliveryMessage.newBuilder()
                .setManifest(manifest(plan))
                .build());
            phase = 1;
        }

        private void streamNextChunk() {
            if (artifactIndex >= plan.artifacts().size()) {
                phase = 2;
                return;
            }
            if (currentArtifact == null) {
                openCurrentArtifact();
            }
            var read = readCurrentChunk();
            if (read == -1) {
                finishCurrentArtifact();
                return;
            }
            currentDigest.update(buffer, 0, read);
            var data = ByteString.copyFrom(buffer, 0, read);
            observer.onNext(BtmArtifactDeliveryMessage.newBuilder()
                .setChunk(BtmArtifactChunk.newBuilder()
                    .setArtifactReference(currentArtifact.reference().artifact().path())
                    .setArtifactKind(kind(currentArtifact.kind()))
                    .setRelativePath(currentArtifact.reference().artifact().path())
                    .setChunkIndex(chunkIndex)
                    .setByteOffset(offset)
                    .setData(data)
                    .setChunkSha256(sha256(data.toByteArray()))
                    .setFinalChunk(offset + read >= currentArtifact.reference().artifact().sizeBytes()))
                .build());
            offset += read;
            chunkIndex++;
        }

        private void openCurrentArtifact() {
            currentArtifact = plan.artifacts().get(artifactIndex);
            currentStream = applicationService.open(currentArtifact);
            currentDigest = sha256Digest();
            chunkIndex = 0;
            offset = 0L;
        }

        private int readCurrentChunk() {
            try {
                return currentStream.read(buffer);
            } catch (IOException error) {
                throw new BtmArtifactDeliveryException(
                    BtmArtifactDeliveryException.Reason.FAILED_PRECONDITION,
                    "Failed to stream accepted BTM artifact.",
                    error
                );
            }
        }

        private void finishCurrentArtifact() {
            closeCurrentStream();
            if (offset != currentArtifact.reference().artifact().sizeBytes()) {
                throw new BtmArtifactDeliveryException(
                    BtmArtifactDeliveryException.Reason.FAILED_PRECONDITION,
                    "BTM artifact size changed during delivery."
                );
            }
            if (!HexFormat.of().formatHex(currentDigest.digest()).equals(currentArtifact.reference().artifact().sha256())) {
                throw new BtmArtifactDeliveryException(
                    BtmArtifactDeliveryException.Reason.FAILED_PRECONDITION,
                    "BTM artifact checksum changed during delivery."
                );
            }
            currentArtifact = null;
            currentDigest = null;
            artifactIndex++;
        }

        private void sendStatus() {
            observer.onNext(BtmArtifactDeliveryMessage.newBuilder()
                .setStatus(OperationStatus.newBuilder()
                    .setCode("DELIVERED")
                    .setMessage("BTM artifact delivery completed")
                    .setRetryable(false)
                    .setCorrelationId(request.getCorrelationId()))
                .build());
            phase = 3;
            observer.onCompleted();
        }

        private synchronized void cancel() {
            closeCurrentStream();
            phase = 3;
        }

        private void fail(RuntimeException error) {
            closeCurrentStream();
            phase = 3;
            if (!observer.isCancelled()) {
                observer.onError(status(error).asRuntimeException());
            }
        }

        private void closeCurrentStream() {
            if (currentStream == null) {
                return;
            }
            try {
                currentStream.close();
            } catch (IOException ignored) {
                // Delivery is already ending; the original stream failure is reported separately.
            } finally {
                currentStream = null;
            }
        }
    }

    private static BtmArtifactKind kind(
        de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.BtmArtifactKind kind
    ) {
        return switch (kind) {
            case RULE_FILE -> BtmArtifactKind.BTM_ARTIFACT_KIND_RULE_FILE;
            case MANIFEST -> BtmArtifactKind.BTM_ARTIFACT_KIND_MANIFEST;
        };
    }

    private static Status status(RuntimeException error) {
        return switch (error) {
            case IllegalArgumentException ignored -> Status.INVALID_ARGUMENT.withDescription("Invalid BTM artifact delivery request");
            case NullPointerException ignored -> Status.INVALID_ARGUMENT.withDescription("Invalid BTM artifact delivery request");
            case BtmArtifactDeliveryException deliveryError -> switch (deliveryError.reason()) {
                case INVALID_REQUEST -> Status.INVALID_ARGUMENT.withDescription("Invalid BTM artifact delivery request");
                case NOT_FOUND -> Status.NOT_FOUND.withDescription("BTM artifact is not available");
                case FAILED_PRECONDITION -> Status.FAILED_PRECONDITION.withDescription("BTM artifact delivery precondition failed");
                case RESOURCE_EXHAUSTED -> Status.RESOURCE_EXHAUSTED.withDescription("BTM artifact delivery size limit exceeded");
            };
            default -> Status.FAILED_PRECONDITION.withDescription("BTM artifact delivery failed");
        };
    }
}
