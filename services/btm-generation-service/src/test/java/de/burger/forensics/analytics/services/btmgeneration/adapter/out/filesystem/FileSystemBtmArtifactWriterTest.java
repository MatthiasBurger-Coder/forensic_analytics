package de.burger.forensics.analytics.services.btmgeneration.adapter.out.filesystem;

import de.burger.forensics.analytics.services.btmgeneration.application.BtmArtifactException;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisJobId;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.BtmArtifactWriteRequest;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.BtmGenerationPolicy;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.BtmGenerationSummary;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.GeneratedRule;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ProbeKind;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ReproducibilityMetadata;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.RequestMetadata;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.RuleTarget;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.SourceSnapshotId;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.TargetSelection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileSystemBtmArtifactWriterTest {
    @TempDir
    Path tempDir;

    @Test
    void writesDeterministicBtmAndManifestArtifacts() throws Exception {
        var writer = new FileSystemBtmArtifactWriter(tempDir);
        var request = request(100_000);

        var artifacts = writer.write(request);
        var repeated = writer.write(request);
        var btm = artifacts.artifacts().getFirst();
        var manifest = artifacts.artifacts().get(1);
        var content = Files.readString(tempDir.resolve(btm.artifact().path()));
        var manifestContent = Files.readString(tempDir.resolve(manifest.artifact().path()));

        assertEquals("btm/snapshot-1-job-1-rules.btm", btm.artifact().path());
        assertEquals("btm/snapshot-1-job-1-rule-manifest.json", manifest.artifact().path());
        assertEquals("btm-generation-service", btm.producerService());
        assertEquals(64, btm.artifact().sha256().length());
        assertEquals(Files.size(tempDir.resolve(btm.artifact().path())), btm.artifact().sizeBytes());
        assertTrue(content.contains("RULE btm-rule:test-0"));
        assertTrue(content.contains("AT ENTRY\n"));
        assertTrue(content.contains("Source fact artifact: source-facts.json"));
        assertTrue(content.contains("Semantic artifact: semantic.json"));
        assertTrue(content.contains("Policy fingerprint: policy"));
        assertTrue(content.contains("not observed runtime evidence"));
        assertTrue(manifestContent.contains("\"generationFingerprint\": \"generation\""));
        assertTrue(manifestContent.contains("\"targetSelection\""));
        assertTrue(manifestContent.contains("\"generatedArtifacts\""));
        assertTrue(manifestContent.contains("\"path\": \"btm/snapshot-1-job-1-rules.btm\""));
        assertTrue(manifestContent.contains("\"sourceFactArtifactReference\": \"source-facts.json\""));
        assertTrue(manifestContent.contains("\"semanticArtifactReference\": \"semantic.json\""));
        assertTrue(manifestContent.contains("\"maxTargets\": 10"));
        assertEquals(-1, content.indexOf("\r"));
        assertEquals("btm-generation-service", btm.byteAccess().ownerService());
        assertEquals(btm.artifact().path(), btm.byteAccess().retrievalReference());
        assertEquals(artifacts.artifacts(), repeated.artifacts());
    }

    @Test
    void outputLimitIncludesBtmAndManifestBytes() {
        var writer = new FileSystemBtmArtifactWriter(tempDir);

        assertThrows(BtmArtifactException.class, () -> writer.write(request(10)));
    }

    @Test
    void rendersSupportedProbeKindsAndRejectsUnknownProbeKind() throws Exception {
        var writer = new FileSystemBtmArtifactWriter(tempDir.resolve("probe-kinds"));

        var artifacts = writer.write(request(100_000, List.of(ProbeKind.METHOD_EXIT, ProbeKind.THROW)));
        var content = Files.readString(tempDir.resolve("probe-kinds").resolve(artifacts.artifacts().getFirst().artifact().path()));

        assertTrue(content.contains("AT EXIT\n"));
        assertTrue(content.contains("AT THROW\n"));
        assertThrows(IllegalArgumentException.class, () -> writer.write(request(100_000, List.of(ProbeKind.UNKNOWN))));
    }

    @Test
    void reportsWriteFailuresDeterministically() throws Exception {
        var fileRoot = Files.writeString(tempDir.resolve("occupied"), "not a directory");
        var writer = new FileSystemBtmArtifactWriter(fileRoot);

        assertThrows(BtmArtifactException.class, () -> writer.write(request(100_000)));
    }

    @Test
    void createsMissingArtifactRootBeforeWritingBtmArtifacts() {
        var artifactRoot = tempDir.resolve("missing-artifact-root");
        var writer = new FileSystemBtmArtifactWriter(artifactRoot);

        var artifacts = writer.write(request(100_000));

        assertTrue(Files.isRegularFile(artifactRoot.resolve(artifacts.artifacts().getFirst().artifact().path())));
        assertTrue(Files.isRegularFile(artifactRoot.resolve(artifacts.artifacts().get(1).artifact().path())));
    }

    @Test
    void rejectsExistingArtifactWithDifferentBytes() throws Exception {
        Files.createDirectories(tempDir.resolve("btm"));
        Files.writeString(tempDir.resolve("btm/snapshot-1-job-1-rules.btm"), "different");

        var writer = new FileSystemBtmArtifactWriter(tempDir);

        assertThrows(BtmArtifactException.class, () -> writer.write(request(100_000)));
    }

    @Test
    void rejectsNonRegularExistingBtmTargetsBeforeAcceptingBytes() throws Exception {
        var writer = new FileSystemBtmArtifactWriter(tempDir);
        var artifacts = writer.write(request(100_000));
        var rulePath = tempDir.resolve(artifacts.artifacts().getFirst().artifact().path());
        Files.delete(rulePath);
        Files.createDirectory(rulePath);

        assertThrows(BtmArtifactException.class, () -> writer.write(request(100_000)));

        var manifestRoot = tempDir.resolve("manifest-directory");
        var manifestWriter = new FileSystemBtmArtifactWriter(manifestRoot);
        var manifestArtifacts = manifestWriter.write(request(100_000));
        var manifestPath = manifestRoot.resolve(manifestArtifacts.artifacts().get(1).artifact().path());
        Files.delete(manifestPath);
        Files.createDirectory(manifestPath);

        assertThrows(BtmArtifactException.class, () -> manifestWriter.write(request(100_000)));
    }

    @Test
    void rejectsSymlinkedArtifactRootBeforeWritingBtmArtifacts() throws Exception {
        var outside = Files.createDirectory(tempDir.resolve("outside-root"));
        var symlinkRoot = tempDir.resolve("artifact-root-link");
        Files.createSymbolicLink(symlinkRoot, outside);
        var writer = new FileSystemBtmArtifactWriter(symlinkRoot);

        assertThrows(BtmArtifactException.class, () -> writer.write(request(100_000)));
        try (var files = Files.list(outside)) {
            assertEquals(0, files.count());
        }
    }

    @Test
    void rejectsSymlinkedBtmDirectoryBeforeWritingArtifacts() throws Exception {
        var outside = Files.createDirectory(tempDir.resolve("outside"));
        Files.createSymbolicLink(tempDir.resolve("btm"), outside);
        var writer = new FileSystemBtmArtifactWriter(tempDir);

        assertThrows(BtmArtifactException.class, () -> writer.write(request(100_000)));
        try (var files = Files.list(outside)) {
            assertEquals(0, files.count());
        }
    }

    @Test
    void rejectsUnsafeRelativePathsInWriterGuard() throws Exception {
        var relativePath = FileSystemBtmArtifactWriter.class.getDeclaredMethod("relativePath", String.class, String.class);
        relativePath.setAccessible(true);

        assertWriterPathRejected(relativePath, "");
        assertWriterPathRejected(relativePath, "/tmp/rules.btm");
        assertWriterPathRejected(relativePath, "file:rules.btm");
        assertWriterPathRejected(relativePath, "https://example.test/rules.btm");
        assertWriterPathRejected(relativePath, "C:/tmp/rules.btm");
        assertWriterPathRejected(relativePath, "btm/./rules.btm");
        assertWriterPathRejected(relativePath, "btm//rules.btm");
        assertWriterPathRejected(relativePath, "btm/../rules.btm");
    }

    @Test
    void rejectsFinalRuleSymlinkBeforeAcceptingExistingIdenticalBytes() throws Exception {
        var writer = new FileSystemBtmArtifactWriter(tempDir);
        var artifacts = writer.write(request(100_000));
        var rulePath = tempDir.resolve(artifacts.artifacts().getFirst().artifact().path());
        var outside = Files.writeString(tempDir.resolve("outside-rules.btm"), Files.readString(rulePath, StandardCharsets.UTF_8));
        var outsideContent = Files.readString(outside, StandardCharsets.UTF_8);
        Files.delete(rulePath);
        Files.createSymbolicLink(rulePath, outside);

        assertThrows(BtmArtifactException.class, () -> writer.write(request(100_000)));
        assertEquals(outsideContent, Files.readString(outside, StandardCharsets.UTF_8));
    }

    @Test
    void rejectsFinalManifestSymlinkBeforeAcceptingExistingIdenticalBytes() throws Exception {
        var writer = new FileSystemBtmArtifactWriter(tempDir);
        var artifacts = writer.write(request(100_000));
        var manifestPath = tempDir.resolve(artifacts.artifacts().get(1).artifact().path());
        var outside = Files.writeString(tempDir.resolve("outside-manifest.json"), Files.readString(manifestPath, StandardCharsets.UTF_8));
        var outsideContent = Files.readString(outside, StandardCharsets.UTF_8);
        Files.delete(manifestPath);
        Files.createSymbolicLink(manifestPath, outside);

        assertThrows(BtmArtifactException.class, () -> writer.write(request(100_000)));
        assertEquals(outsideContent, Files.readString(outside, StandardCharsets.UTF_8));
    }

    private static BtmArtifactWriteRequest request(long maxArtifactBytes) {
        return request(maxArtifactBytes, List.of(ProbeKind.METHOD_ENTRY));
    }

    private static BtmArtifactWriteRequest request(long maxArtifactBytes, List<ProbeKind> probeKinds) {
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
        var rules = java.util.stream.IntStream.range(0, probeKinds.size())
            .mapToObj(index -> new GeneratedRule(
                "btm-rule:test-" + index,
                new RuleTarget(
                    "target-" + index,
                    "fact-" + index,
                    "semantic-" + index,
                    "src/main/java/a/A.java",
                    "a.A",
                    "run",
                    "a.A#run()",
                    12 + index,
                    probeKinds.get(index),
                    "source-facts.json",
                    "semantic.json",
                    index,
                    AnalysisCompleteness.COMPLETE,
                    "internal"
                )
            ))
            .toList();
        return new BtmArtifactWriteRequest(
            metadata,
            new BtmGenerationPolicy(10, maxArtifactBytes, 60, "btm-rule-v1", false),
            rules,
            List.of(),
            new BtmGenerationSummary(probeKinds.size(), probeKinds.size(), 0, 1, 1, "btm-generation-service", "test", "btm-rule-v1"),
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
                probeKinds.size()
            )
        );
    }

    private static void assertWriterPathRejected(Method relativePath, String value) {
        var failure = assertThrows(
            InvocationTargetException.class,
            () -> relativePath.invoke(null, value, "BTM artifact path")
        );
        assertTrue(failure.getCause() instanceof BtmArtifactException);
    }

}
