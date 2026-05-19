package de.burger.forensics.analytics.services.btmgeneration.adapter.out.filesystem;

import de.burger.forensics.analytics.services.btmgeneration.application.BtmArtifactDeliveryException;
import de.burger.forensics.analytics.services.btmgeneration.application.BtmArtifactDeliveryApplicationService;
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
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.BtmArtifactKind;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.BtmArtifactWriteRequest;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.BtmGenerationPolicy;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.BtmGenerationSummary;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.GeneratedBtmArtifacts;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.GeneratedRule;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ProbeKind;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ReproducibilityMetadata;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.RequestMetadata;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.RuleTarget;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.SourceSnapshotId;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.TargetSelection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.BTM_DELIVERY_CONTRACT;
import static de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.PRODUCER_SERVICE;
import static de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.sha256;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileSystemBtmArtifactReaderTest {
    @TempDir
    Path tempDir;

    @Test
    void deliversRequestedArtifactSubsetUsingStoredManifestMetadata() {
        var artifacts = writeArtifacts();
        var btmArtifact = artifacts.artifacts().getFirst();

        var plan = service(tempDir).prepare(command(artifacts, List.of(btmArtifact.artifact().path())));

        assertEquals(2, plan.artifacts().size());
        assertEquals(BtmArtifactKind.MANIFEST, plan.artifacts().getFirst().kind());
        assertEquals(BtmArtifactKind.RULE_FILE, plan.artifacts().get(1).kind());
        assertEquals("snapshot-1", plan.manifest().sourceSnapshotId().value());
        assertEquals("selection-1", plan.manifest().targetSelection().selectionId());
        assertEquals(artifacts.artifacts().get(1).artifact().path(), plan.manifest().artifacts().getFirst().artifactReference());
        assertEquals(btmArtifact.artifact().path(), plan.manifest().artifacts().get(1).artifactReference());
    }

    @Test
    void rejectsMissingManifestUnknownArtifactTypeMalformedManifestAndUnsafeFiles() throws Exception {
        var artifacts = writeArtifacts();
        var initialArtifacts = artifacts;

        var missingManifest = assertThrows(BtmArtifactDeliveryException.class, () -> service(tempDir)
            .prepare(command(new GeneratedBtmArtifacts(List.of(initialArtifacts.artifacts().getFirst()), initialArtifacts.artifacts().getFirst().artifact().sizeBytes()), List.of())));
        assertEquals(BtmArtifactDeliveryException.Reason.FAILED_PRECONDITION, missingManifest.reason());

        var unknownType = assertThrows(BtmArtifactDeliveryException.class, () -> service(tempDir)
            .prepare(command(new GeneratedBtmArtifacts(List.of(
                initialArtifacts.artifacts().getFirst(),
                initialArtifacts.artifacts().get(1),
                generatedArtifact("btm/unknown.bin", "application/octet-stream", "a".repeat(64), 1)
            ), initialArtifacts.totalBytes() + 1), List.of())));
        assertEquals(BtmArtifactDeliveryException.Reason.FAILED_PRECONDITION, unknownType.reason());

        var malformed = "{}".getBytes(StandardCharsets.UTF_8);
        var manifestPath = artifacts.artifacts().get(1).artifact().path();
        Files.write(tempDir.resolve(manifestPath), malformed);
        var malformedManifestArtifacts = new GeneratedBtmArtifacts(List.of(
            artifacts.artifacts().getFirst(),
            generatedArtifact(manifestPath, artifacts.artifacts().get(1).artifact().type(), sha256(malformed), malformed.length)
        ), artifacts.artifacts().getFirst().artifact().sizeBytes() + malformed.length);
        var malformedManifest = assertThrows(BtmArtifactDeliveryException.class, () -> service(tempDir)
            .prepare(command(malformedManifestArtifacts, List.of())));
        assertEquals(BtmArtifactDeliveryException.Reason.FAILED_PRECONDITION, malformedManifest.reason());

        var directoryRoot = tempDir.resolve("directory-case");
        var directoryArtifacts = new FileSystemBtmArtifactWriter(directoryRoot).write(writeRequest());
        var btmPath = directoryRoot.resolve(directoryArtifacts.artifacts().getFirst().artifact().path());
        Files.delete(btmPath);
        Files.createDirectory(btmPath);
        var directoryArtifact = assertThrows(BtmArtifactDeliveryException.class, () -> service(directoryRoot)
            .prepare(command(directoryArtifacts, List.of())));
        assertEquals(BtmArtifactDeliveryException.Reason.FAILED_PRECONDITION, directoryArtifact.reason());
    }

    @Test
    void rejectsManifestWithoutGeneratedArtifactDeclarationsAndSymlinkSegments() throws Exception {
        var artifacts = writeArtifacts();
        var manifestPath = tempDir.resolve(artifacts.artifacts().get(1).artifact().path());
        var missingGeneratedArtifacts = Files.readString(manifestPath).replaceFirst(
            "\\s+\"generatedArtifacts\": \\[[\\s\\S]*?\\],\\R",
            "\n"
        );
        Files.writeString(manifestPath, missingGeneratedArtifacts);
        var rewrittenManifest = Files.readAllBytes(manifestPath);
        var missingDeclarationArtifacts = new GeneratedBtmArtifacts(List.of(
            artifacts.artifacts().getFirst(),
            generatedArtifact(
                artifacts.artifacts().get(1).artifact().path(),
                artifacts.artifacts().get(1).artifact().type(),
                sha256(rewrittenManifest),
                rewrittenManifest.length
            )
        ), artifacts.artifacts().getFirst().artifact().sizeBytes() + rewrittenManifest.length);

        var missingDeclaration = assertThrows(BtmArtifactDeliveryException.class, () -> service(tempDir)
            .prepare(command(missingDeclarationArtifacts, List.of())));
        assertEquals(BtmArtifactDeliveryException.Reason.FAILED_PRECONDITION, missingDeclaration.reason());

        var symlinkRoot = tempDir.resolve("symlink-case");
        var symlinkArtifacts = new FileSystemBtmArtifactWriter(symlinkRoot).write(writeRequest());
        var btmDirectory = symlinkRoot.resolve("btm");
        var movedDirectory = symlinkRoot.resolve("btm-real");
        Files.move(btmDirectory, movedDirectory);
        Files.createSymbolicLink(btmDirectory, movedDirectory);
        var symlinkSegment = assertThrows(BtmArtifactDeliveryException.class, () -> service(symlinkRoot)
            .prepare(command(symlinkArtifacts, List.of())));
        assertEquals(BtmArtifactDeliveryException.Reason.FAILED_PRECONDITION, symlinkSegment.reason());
    }

    @Test
    void rejectsEmptyGeneratedArtifactDeclarations() throws Exception {
        var artifacts = writeArtifacts();
        var manifestPath = tempDir.resolve(artifacts.artifacts().get(1).artifact().path());
        var emptyGeneratedArtifacts = Files.readString(manifestPath).replaceFirst(
            "\"generatedArtifacts\": \\[[\\s\\S]*?\\]",
            "\"generatedArtifacts\": []"
        );
        Files.writeString(manifestPath, emptyGeneratedArtifacts);
        var rewrittenManifest = Files.readAllBytes(manifestPath);
        var emptyDeclarationArtifacts = new GeneratedBtmArtifacts(List.of(
            artifacts.artifacts().getFirst(),
            generatedArtifact(
                artifacts.artifacts().get(1).artifact().path(),
                artifacts.artifacts().get(1).artifact().type(),
                sha256(rewrittenManifest),
                rewrittenManifest.length
            )
        ), artifacts.artifacts().getFirst().artifact().sizeBytes() + rewrittenManifest.length);

        var emptyDeclaration = assertThrows(BtmArtifactDeliveryException.class, () -> service(tempDir)
            .prepare(command(emptyDeclarationArtifacts, List.of())));

        assertEquals(BtmArtifactDeliveryException.Reason.FAILED_PRECONDITION, emptyDeclaration.reason());
    }

    @Test
    void rejectsMetadataSizeMismatchBeforeTreatingBytesAsDeliverable() {
        var artifacts = writeArtifacts();
        var btm = artifacts.artifacts().getFirst();
        var wrongSize = new GeneratedBtmArtifacts(List.of(
            generatedArtifact(btm.artifact().path(), btm.artifact().type(), btm.artifact().sha256(), btm.artifact().sizeBytes() + 1),
            artifacts.artifacts().get(1)
        ), artifacts.totalBytes() + 1);

        var mismatch = assertThrows(BtmArtifactDeliveryException.class, () -> service(tempDir)
            .prepare(command(wrongSize, List.of())));

        assertEquals(BtmArtifactDeliveryException.Reason.FAILED_PRECONDITION, mismatch.reason());
    }

    @Test
    void rejectsStoredManifestWithMissingNestedMetadata() throws Exception {
        var artifacts = writeArtifacts();
        var manifestPath = tempDir.resolve(artifacts.artifacts().get(1).artifact().path());
        var missingTargetSelection = Files.readString(manifestPath).replaceFirst(
            "\\s+\"targetSelection\": \\{[\\s\\S]*?\\},\\R",
            "\n"
        );
        Files.writeString(manifestPath, missingTargetSelection);
        var rewrittenManifest = Files.readAllBytes(manifestPath);
        var missingMetadataArtifacts = new GeneratedBtmArtifacts(List.of(
            artifacts.artifacts().getFirst(),
            generatedArtifact(
                artifacts.artifacts().get(1).artifact().path(),
                artifacts.artifacts().get(1).artifact().type(),
                sha256(rewrittenManifest),
                rewrittenManifest.length
            )
        ), artifacts.artifacts().getFirst().artifact().sizeBytes() + rewrittenManifest.length);

        var missingMetadata = assertThrows(BtmArtifactDeliveryException.class, () -> service(tempDir)
            .prepare(command(missingMetadataArtifacts, List.of())));

        assertEquals(BtmArtifactDeliveryException.Reason.FAILED_PRECONDITION, missingMetadata.reason());
    }

    @Test
    void rejectsDeliveryRequestWithDifferentAnalysisIdentityThanStoredManifest() {
        var artifacts = writeArtifacts();

        var mismatch = assertThrows(BtmArtifactDeliveryException.class, () -> service(tempDir)
            .prepare(command(artifacts, List.of(), new AnalysisRunId("other-run"), new AnalysisJobId("job-1"))));

        assertEquals(BtmArtifactDeliveryException.Reason.FAILED_PRECONDITION, mismatch.reason());
    }

    @Test
    void rejectsRuleArtifactsThatAreNotDeclaredByTheStoredManifest() {
        var artifacts = writeArtifacts();
        var foreign = generatedArtifact(
            "btm/foreign-rules.btm",
            artifacts.artifacts().getFirst().artifact().type(),
            "c".repeat(64),
            1
        );
        var mixed = new GeneratedBtmArtifacts(List.of(
            artifacts.artifacts().getFirst(),
            artifacts.artifacts().get(1),
            foreign
        ), artifacts.totalBytes() + 1);

        var mismatch = assertThrows(BtmArtifactDeliveryException.class, () -> service(tempDir)
            .prepare(command(mixed, List.of("btm/foreign-rules.btm"))));

        assertEquals(BtmArtifactDeliveryException.Reason.FAILED_PRECONDITION, mismatch.reason());
    }

    private GeneratedBtmArtifacts writeArtifacts() {
        return new FileSystemBtmArtifactWriter(tempDir).write(writeRequest());
    }

    private static BtmArtifactDeliveryApplicationService service(Path root) {
        return new BtmArtifactDeliveryApplicationService(new FileSystemBtmArtifactReader(root));
    }

    private static BtmArtifactDeliveryCommand command(GeneratedBtmArtifacts artifacts, List<String> artifactReferences) {
        return command(artifacts, artifactReferences, new AnalysisRunId("run-1"), new AnalysisJobId("job-1"));
    }

    private static BtmArtifactDeliveryCommand command(
        GeneratedBtmArtifacts artifacts,
        List<String> artifactReferences,
        AnalysisRunId analysisRunId,
        AnalysisJobId analysisJobId
    ) {
        return new BtmArtifactDeliveryCommand(
            new BtmArtifactDeliveryMetadata(
                "request-1",
                "idempotency-1",
                "btm-generation-v1",
                "correlation-1",
                analysisRunId,
                analysisJobId,
                Map.of()
            ),
            64,
            100_000,
            artifactReferences,
            artifacts.artifacts()
        );
    }

    private static AnalysisArtifactReference generatedArtifact(String path, String type, String sha256, long sizeBytes) {
        return new AnalysisArtifactReference(
            new ArtifactReference(path, type, sha256, sizeBytes),
            AnalysisArtifactCategory.GENERATED,
            PRODUCER_SERVICE,
            "btm-rule-v1",
            AnalysisCompleteness.COMPLETE,
            new ArtifactByteAccess(PRODUCER_SERVICE, BTM_DELIVERY_CONTRACT, path, ArtifactByteCustody.PRODUCER_RETAINED)
        );
    }

    private static BtmArtifactWriteRequest writeRequest() {
        var metadata = new RequestMetadata(
            "request-1",
            "idempotency-1",
            "btm-generation-v1",
            "correlation-1",
            new AnalysisRunId("run-1"),
            new AnalysisJobId("job-1"),
            new SourceSnapshotId("snapshot-1"),
            "btm-generation-service-test",
            Map.of("tenant", "demo")
        );
        var rule = new GeneratedRule(
            "btm-rule:test",
            new RuleTarget(
                "target-1",
                "fact-1",
                "semantic-1",
                "src/main/java/a/A.java",
                "a.A",
                "run",
                "a.A#run()",
                12,
                ProbeKind.METHOD_ENTRY,
                "source-facts.json",
                "semantic.json",
                0,
                AnalysisCompleteness.COMPLETE,
                "internal"
            )
        );
        return new BtmArtifactWriteRequest(
            metadata,
            new BtmGenerationPolicy(10, 100_000, 60, "btm-rule-v1", false),
            List.of(rule),
            List.of(),
            new BtmGenerationSummary(1, 1, 0, 1, 1, PRODUCER_SERVICE, "test", "btm-rule-v1"),
            new ReproducibilityMetadata("facts", "policy", "generation", "test", "target_id_probe_kind_ascending"),
            AnalysisCompleteness.COMPLETE,
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
}
