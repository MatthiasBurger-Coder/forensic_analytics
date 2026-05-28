package de.burger.forensics.analytics.services.btmgeneration.application;

import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisArtifactCategory;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisJobId;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ArtifactByteAccess;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ArtifactByteCustody;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ArtifactReference;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.BtmArtifactDeliveryCommand;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.BtmArtifactDeliveryMetadata;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ReproducibilityMetadata;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.SourceSnapshotId;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.StoredBtmArtifactManifest;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.TargetSelection;
import de.burger.forensics.analytics.services.btmgeneration.application.port.BtmArtifactReaderPort;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.BTM_DELIVERY_CONTRACT;
import static de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.PRODUCER_SERVICE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BtmArtifactDeliveryApplicationServiceTest {
    @Test
    void acceptsOnlyBtmOwnedGeneratedArtifactsAndHonorsRequestedSubset() {
        var readerCalled = new AtomicBoolean();
        var service = new BtmArtifactDeliveryApplicationService(reader(readerCalled));

        service.prepare(command(List.of(artifact(), manifestArtifact()), List.of("btm/rules.btm")));

        assertTrue(readerCalled.get());
    }

    @Test
    void rejectsNonGeneratedProducerMismatchAndInvalidByteAccess() {
        assertRejected(artifact(AnalysisArtifactCategory.STATIC, PRODUCER_SERVICE, PRODUCER_SERVICE, BTM_DELIVERY_CONTRACT, "btm/rules.btm", ArtifactByteCustody.PRODUCER_RETAINED));
        assertRejected(artifact(AnalysisArtifactCategory.GENERATED, "another-service", PRODUCER_SERVICE, BTM_DELIVERY_CONTRACT, "btm/rules.btm", ArtifactByteCustody.PRODUCER_RETAINED));
        assertRejected(artifact(AnalysisArtifactCategory.GENERATED, PRODUCER_SERVICE, "another-service", BTM_DELIVERY_CONTRACT, "btm/rules.btm", ArtifactByteCustody.PRODUCER_RETAINED));
        assertRejected(artifact(AnalysisArtifactCategory.GENERATED, PRODUCER_SERVICE, PRODUCER_SERVICE, "other.v1.Download", "btm/rules.btm", ArtifactByteCustody.PRODUCER_RETAINED));
        assertRejected(artifact(AnalysisArtifactCategory.GENERATED, PRODUCER_SERVICE, PRODUCER_SERVICE, BTM_DELIVERY_CONTRACT, "btm/other.btm", ArtifactByteCustody.PRODUCER_RETAINED));
        assertRejected(artifact(AnalysisArtifactCategory.GENERATED, PRODUCER_SERVICE, PRODUCER_SERVICE, BTM_DELIVERY_CONTRACT, "btm/rules.btm", ArtifactByteCustody.EXPLICIT_HANDOFF));
    }

    @Test
    void rejectsRequestedArtifactsThatWereNotAccepted() {
        var service = new BtmArtifactDeliveryApplicationService(reader(new AtomicBoolean()));

        var rejected = assertThrows(
            BtmArtifactDeliveryException.class,
            () -> service.prepare(command(List.of(artifact(), manifestArtifact()), List.of("btm/not-accepted.btm")))
        );

        assertEquals(BtmArtifactDeliveryException.Reason.FAILED_PRECONDITION, rejected.reason());
    }

    @Test
    void rejectsAcceptedRuleArtifactsNotDeclaredByStoredManifest() {
        var service = new BtmArtifactDeliveryApplicationService(reader(new AtomicBoolean(), List.of()));

        var rejected = assertThrows(
            BtmArtifactDeliveryException.class,
            () -> service.prepare(command(List.of(artifact(), manifestArtifact()), List.of("btm/rules.btm")))
        );

        assertEquals(BtmArtifactDeliveryException.Reason.FAILED_PRECONDITION, rejected.reason());
    }

    @Test
    void rejectsStoredManifestRuleArtifactsMissingFromAcceptedHandoff() {
        var service = new BtmArtifactDeliveryApplicationService(reader(new AtomicBoolean()));

        var rejected = assertThrows(
            BtmArtifactDeliveryException.class,
            () -> service.prepare(command(List.of(manifestArtifact()), List.of()))
        );

        assertEquals(BtmArtifactDeliveryException.Reason.FAILED_PRECONDITION, rejected.reason());
    }

    private static void assertRejected(AnalysisArtifactReference artifact) {
        var service = new BtmArtifactDeliveryApplicationService(reader(new AtomicBoolean()));
        var rejected = assertThrows(
            BtmArtifactDeliveryException.class,
            () -> service.prepare(command(List.of(artifact), List.of()))
        );
        assertEquals(BtmArtifactDeliveryException.Reason.FAILED_PRECONDITION, rejected.reason());
    }

    private static BtmArtifactDeliveryCommand command(
        List<AnalysisArtifactReference> artifacts,
        List<String> artifactReferences
    ) {
        return new BtmArtifactDeliveryCommand(
            new BtmArtifactDeliveryMetadata(
                "request-1",
                "idempotency-1",
                "btm-generation-v1",
                "correlation-1",
                new AnalysisRunId("run-1"),
                new AnalysisJobId("job-1"),
                Map.of()
            ),
            64,
            100_000,
            artifactReferences,
            artifacts
        );
    }

    private static AnalysisArtifactReference artifact() {
        return artifact(
            AnalysisArtifactCategory.GENERATED,
            PRODUCER_SERVICE,
            PRODUCER_SERVICE,
            BTM_DELIVERY_CONTRACT,
            "btm/rules.btm",
            "btm/rules.btm",
            "application/vnd.forensic-analytics.btm-rules.v1+btm",
            ArtifactByteCustody.PRODUCER_RETAINED
        );
    }

    private static AnalysisArtifactReference manifestArtifact() {
        return artifact(
            AnalysisArtifactCategory.GENERATED,
            PRODUCER_SERVICE,
            PRODUCER_SERVICE,
            BTM_DELIVERY_CONTRACT,
            "btm/rule-manifest.json",
            "btm/rule-manifest.json",
            "application/vnd.forensic-analytics.btm-rule-manifest.v1+json",
            ArtifactByteCustody.PRODUCER_RETAINED
        );
    }

    private static AnalysisArtifactReference artifact(
        AnalysisArtifactCategory category,
        String producerService,
        String byteOwner,
        String retrievalContract,
        String retrievalReference,
        ArtifactByteCustody custody
    ) {
        return artifact(
            category,
            producerService,
            byteOwner,
            retrievalContract,
            retrievalReference,
            "btm/rules.btm",
            "application/vnd.forensic-analytics.btm-rules.v1+btm",
            custody
        );
    }

    private static AnalysisArtifactReference artifact(
        AnalysisArtifactCategory category,
        String producerService,
        String byteOwner,
        String retrievalContract,
        String retrievalReference,
        String artifactPath,
        String artifactType,
        ArtifactByteCustody custody
    ) {
        return new AnalysisArtifactReference(
            new ArtifactReference(artifactPath, artifactType, "a".repeat(64), 42),
            category,
            producerService,
            "btm-rule-v1",
            AnalysisCompleteness.COMPLETE,
            new ArtifactByteAccess(byteOwner, retrievalContract, retrievalReference, custody)
        );
    }

    private static StoredBtmArtifactManifest manifest(List<ArtifactReference> generatedArtifacts) {
        return new StoredBtmArtifactManifest(
            new AnalysisRunId("run-1"),
            new AnalysisJobId("job-1"),
            new SourceSnapshotId("snapshot-1"),
            AnalysisCompleteness.COMPLETE,
            generatedArtifacts,
            new ReproducibilityMetadata("facts", "policy", "generation", "test", "sort"),
            new TargetSelection(
                "selection-1",
                "analysis-store-service",
                "target-policy-v1",
                "selection-fingerprint",
                AnalysisCompleteness.COMPLETE,
                "target_id_probe_kind_ascending",
                "correlation-1",
                1
            )
        );
    }

    private static BtmArtifactReaderPort reader(AtomicBoolean called) {
        return reader(called, List.of(artifact().artifact()));
    }

    private static BtmArtifactReaderPort reader(AtomicBoolean called, List<ArtifactReference> generatedArtifacts) {
        return new BtmArtifactReaderPort() {
            @Override
            public StoredBtmArtifactManifest readManifest(AnalysisArtifactReference manifestReference) {
                called.set(true);
                return manifest(generatedArtifacts);
            }

            @Override
            public void verify(AnalysisArtifactReference artifact) {
                called.set(true);
            }

            @Override
            public InputStream open(de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ReadableBtmArtifact artifact) {
                return new ByteArrayInputStream(new byte[0]);
            }
        };
    }
}
