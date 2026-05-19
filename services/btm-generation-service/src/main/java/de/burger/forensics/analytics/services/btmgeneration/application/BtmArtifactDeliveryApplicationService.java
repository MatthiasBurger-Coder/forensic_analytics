package de.burger.forensics.analytics.services.btmgeneration.application;

import de.burger.forensics.analytics.services.btmgeneration.application.port.BtmArtifactReaderPort;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisArtifactCategory;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ArtifactReference;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ArtifactByteCustody;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.BtmArtifactDeliveryCommand;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.BtmArtifactDeliveryPlan;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.BtmArtifactDescriptor;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.BtmArtifactKind;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.BtmDeliveryManifest;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ReadableBtmArtifact;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.StoredBtmArtifactManifest;

import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.BTM_DELIVERY_CONTRACT;
import static de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.BTM_MANIFEST_ARTIFACT_TYPE;
import static de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.BTM_RULE_ARTIFACT_TYPE;
import static de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.PRODUCER_SERVICE;
import static de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.sha256;

public final class BtmArtifactDeliveryApplicationService {
    private static final String DELIVERY_ORDER = "artifact_reference_ascending";
    private final BtmArtifactReaderPort artifactReader;

    public BtmArtifactDeliveryApplicationService(BtmArtifactReaderPort artifactReader) {
        this.artifactReader = Objects.requireNonNull(artifactReader, "artifact reader must not be null");
    }

    public BtmArtifactDeliveryPlan prepare(BtmArtifactDeliveryCommand command) {
        var verifiedCommand = Objects.requireNonNull(command, "delivery command must not be null");
        validateAcceptedArtifacts(verifiedCommand);
        validateRequestedArtifacts(verifiedCommand);
        var manifestReference = manifestReference(verifiedCommand);
        artifactReader.verify(manifestReference);
        var storedManifest = artifactReader.readManifest(manifestReference);
        validateStoredIdentity(verifiedCommand, storedManifest);
        validateStoredManifestCoveredByAcceptedArtifacts(verifiedCommand, storedManifest);
        var selectedArtifacts = selectedArtifacts(verifiedCommand, manifestReference, storedManifest);
        validateTotalBytes(verifiedCommand.maxTotalBytes(), selectedArtifacts);
        selectedArtifacts.forEach(artifactReader::verify);
        var readableArtifacts = selectedArtifacts.stream()
            .map(reference -> new ReadableBtmArtifact(reference, kind(reference)))
            .toList();
        var descriptors = readableArtifacts.stream()
            .map(artifact -> descriptor(artifact, verifiedCommand.maxChunkBytes()))
            .toList();
        var totalBytes = selectedArtifacts.stream().mapToLong(reference -> reference.artifact().sizeBytes()).sum();
        return new BtmArtifactDeliveryPlan(
            deliveryManifest(storedManifest, manifestReference.artifact().sha256(), descriptors, totalBytes),
            readableArtifacts,
            verifiedCommand.maxChunkBytes()
        );
    }

    public InputStream open(ReadableBtmArtifact artifact) {
        return artifactReader.open(Objects.requireNonNull(artifact, "readable artifact must not be null"));
    }

    private static void validateAcceptedArtifacts(BtmArtifactDeliveryCommand command) {
        command.acceptedGeneratedArtifacts().forEach(BtmArtifactDeliveryApplicationService::validateAcceptedArtifact);
    }

    private static void validateAcceptedArtifact(AnalysisArtifactReference reference) {
        if (reference.category() != AnalysisArtifactCategory.GENERATED) {
            throw new BtmArtifactDeliveryException(
                BtmArtifactDeliveryException.Reason.FAILED_PRECONDITION,
                "Accepted artifact is not a generated BTM artifact."
            );
        }
        if (!PRODUCER_SERVICE.equals(reference.producerService())) {
            throw new BtmArtifactDeliveryException(
                BtmArtifactDeliveryException.Reason.FAILED_PRECONDITION,
                "Accepted artifact is not owned by BTM generation."
            );
        }
        if (!PRODUCER_SERVICE.equals(reference.byteAccess().ownerService())
            || !BTM_DELIVERY_CONTRACT.equals(reference.byteAccess().retrievalContract())
            || !reference.artifact().path().equals(reference.byteAccess().retrievalReference())
            || reference.byteAccess().byteCustody() != ArtifactByteCustody.PRODUCER_RETAINED) {
            throw new BtmArtifactDeliveryException(
                BtmArtifactDeliveryException.Reason.FAILED_PRECONDITION,
                "Accepted artifact byte access is not owned by BTM generation."
            );
        }
        kind(reference);
    }

    private static void validateRequestedArtifacts(BtmArtifactDeliveryCommand command) {
        if (command.artifactReferences().isEmpty()) {
            return;
        }
        var accepted = command.acceptedGeneratedArtifacts().stream()
            .map(reference -> reference.artifact().path())
            .collect(Collectors.toUnmodifiableSet());
        var missing = command.artifactReferences().stream()
            .filter(reference -> !accepted.contains(reference))
            .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        if (!missing.isEmpty()) {
            throw new BtmArtifactDeliveryException(
                BtmArtifactDeliveryException.Reason.FAILED_PRECONDITION,
                "Requested artifact is not accepted for delivery."
            );
        }
    }

    private static AnalysisArtifactReference manifestReference(BtmArtifactDeliveryCommand command) {
        return command.acceptedGeneratedArtifacts().stream()
            .filter(reference -> kind(reference) == BtmArtifactKind.MANIFEST)
            .min(Comparator.comparing(reference -> reference.artifact().path()))
            .orElseThrow(() -> new BtmArtifactDeliveryException(
                BtmArtifactDeliveryException.Reason.FAILED_PRECONDITION,
                "Accepted BTM manifest artifact is required."
            ));
    }

    private static List<AnalysisArtifactReference> selectedArtifacts(
        BtmArtifactDeliveryCommand command,
        AnalysisArtifactReference manifestReference,
        StoredBtmArtifactManifest storedManifest
    ) {
        var requested = command.artifactReferences();
        return command.acceptedGeneratedArtifacts().stream()
            .filter(reference -> requested.isEmpty()
                || requested.contains(reference.artifact().path())
                || sameArtifact(reference.artifact(), manifestReference.artifact()))
            .sorted(Comparator.comparing(reference -> reference.artifact().path()))
            .peek(reference -> validateStoredBinding(reference, manifestReference, storedManifest))
            .toList();
    }

    private static void validateStoredBinding(
        AnalysisArtifactReference reference,
        AnalysisArtifactReference manifestReference,
        StoredBtmArtifactManifest storedManifest
    ) {
        if (kind(reference) == BtmArtifactKind.MANIFEST) {
            if (!sameArtifact(reference.artifact(), manifestReference.artifact())) {
                throw new BtmArtifactDeliveryException(
                    BtmArtifactDeliveryException.Reason.FAILED_PRECONDITION,
                    "Accepted BTM manifest artifact is not the selected delivery manifest."
                );
            }
            return;
        }
        var declared = storedManifest.generatedArtifacts().stream()
            .anyMatch(storedReference -> sameArtifact(storedReference, reference.artifact()));
        if (!declared) {
            throw new BtmArtifactDeliveryException(
                BtmArtifactDeliveryException.Reason.FAILED_PRECONDITION,
                "Accepted BTM artifact is not declared by the stored manifest."
            );
        }
    }

    private static void validateStoredManifestCoveredByAcceptedArtifacts(
        BtmArtifactDeliveryCommand command,
        StoredBtmArtifactManifest storedManifest
    ) {
        var acceptedRuleArtifacts = command.acceptedGeneratedArtifacts().stream()
            .filter(reference -> kind(reference) == BtmArtifactKind.RULE_FILE)
            .map(AnalysisArtifactReference::artifact)
            .toList();
        var missing = storedManifest.generatedArtifacts().stream()
            .filter(storedReference -> acceptedRuleArtifacts.stream()
                .noneMatch(acceptedArtifact -> sameArtifact(storedReference, acceptedArtifact)))
            .toList();
        if (!missing.isEmpty()) {
            throw new BtmArtifactDeliveryException(
                BtmArtifactDeliveryException.Reason.FAILED_PRECONDITION,
                "Stored BTM manifest declares artifacts that are missing from the accepted handoff."
            );
        }
    }

    private static boolean sameArtifact(ArtifactReference left, ArtifactReference right) {
        return left.path().equals(right.path())
            && left.type().equals(right.type())
            && left.sha256().equals(right.sha256())
            && left.sizeBytes() == right.sizeBytes();
    }

    private static void validateStoredIdentity(BtmArtifactDeliveryCommand command, StoredBtmArtifactManifest storedManifest) {
        if (!storedManifest.analysisRunId().equals(command.metadata().analysisRunId())
            || !storedManifest.analysisJobId().equals(command.metadata().analysisJobId())) {
            throw new BtmArtifactDeliveryException(
                BtmArtifactDeliveryException.Reason.FAILED_PRECONDITION,
                "Stored BTM manifest identity does not match the delivery request."
            );
        }
    }

    private static void validateTotalBytes(long maxTotalBytes, List<AnalysisArtifactReference> selectedArtifacts) {
        var totalBytes = selectedArtifacts.stream().mapToLong(reference -> reference.artifact().sizeBytes()).sum();
        if (totalBytes > maxTotalBytes) {
            throw new BtmArtifactDeliveryException(
                BtmArtifactDeliveryException.Reason.RESOURCE_EXHAUSTED,
                "BTM artifact delivery exceeds requested total byte limit."
            );
        }
    }

    private static BtmArtifactDescriptor descriptor(ReadableBtmArtifact artifact, int chunkBytes) {
        return new BtmArtifactDescriptor(
            artifact.reference().artifact().path(),
            artifact.kind(),
            artifact.reference().artifact().path(),
            artifact.reference().artifact().sha256(),
            artifact.reference().artifact().sizeBytes(),
            chunkCount(artifact.reference().artifact().sizeBytes(), chunkBytes),
            artifact.reference().artifact().type()
        );
    }

    private static int chunkCount(long byteCount, int chunkBytes) {
        return byteCount == 0 ? 0 : Math.toIntExact(Math.floorDiv(byteCount - 1, chunkBytes) + 1);
    }

    private static BtmDeliveryManifest deliveryManifest(
        StoredBtmArtifactManifest storedManifest,
        String manifestSha256,
        List<BtmArtifactDescriptor> descriptors,
        long totalBytes
    ) {
        var descriptorFingerprint = descriptors.stream()
            .map(BtmArtifactDescriptor::canonical)
            .collect(Collectors.joining("\n"));
        return new BtmDeliveryManifest(
            "btm-delivery:" + sha256(storedManifest.analysisRunId().value()
                + "\n" + storedManifest.analysisJobId().value()
                + "\n" + descriptorFingerprint),
            storedManifest.analysisRunId(),
            storedManifest.analysisJobId(),
            storedManifest.sourceSnapshotId(),
            storedManifest.completeness(),
            descriptors,
            totalBytes,
            manifestSha256,
            DELIVERY_ORDER,
            storedManifest.reproducibility(),
            storedManifest.targetSelection()
        );
    }

    private static BtmArtifactKind kind(AnalysisArtifactReference reference) {
        var type = reference.artifact().type();
        if (BTM_RULE_ARTIFACT_TYPE.equals(type)) {
            return BtmArtifactKind.RULE_FILE;
        }
        if (BTM_MANIFEST_ARTIFACT_TYPE.equals(type)) {
            return BtmArtifactKind.MANIFEST;
        }
        throw new BtmArtifactDeliveryException(
            BtmArtifactDeliveryException.Reason.FAILED_PRECONDITION,
            "Accepted generated artifact is not a BTM delivery artifact."
        );
    }
}
