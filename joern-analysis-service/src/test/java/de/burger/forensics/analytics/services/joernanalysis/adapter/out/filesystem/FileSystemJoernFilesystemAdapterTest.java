package de.burger.forensics.analytics.services.joernanalysis.adapter.out.filesystem;

import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalysisArtifactCategory;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalysisJobId;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalyzeJoernCpgCommand;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.ArtifactByteAccess;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.ArtifactByteCustody;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.ArtifactReference;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.JoernCpgDiagnostic;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.JoernCpgPolicy;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.JoernRuntimeResult;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.RequestMetadata;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.SemanticArtifactBytesRequest;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.SourceRoot;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.SourceSnapshotId;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.SourceWorkspace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.PRODUCER_SERVICE;
import static de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.SEMANTIC_ARTIFACT_SCHEMA_VERSION;
import static de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.sha256;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileSystemJoernFilesystemAdapterTest {
    @TempDir
    Path tempDir;

    @Test
    void resolvesServiceOwnedWorkspaceAndRejectsInvalidSourceRoots() throws IOException {
        var workspaceRoot = tempDir.resolve("workspaces");
        var workspace = workspaceRoot.resolve("joern-workspace-snapshot-1");
        var sourceRoot = workspace.resolve("src/main/java");
        write(sourceRoot.resolve("App.java"), "class App {}\n");
        var adapter = new FileSystemJoernWorkspaceAdapter(workspaceRoot);

        var resolved = adapter.resolve(command(true, false, false));

        assertEquals(workspace.toAbsolutePath().normalize(), resolved.workspacePath());
        assertEquals(List.of(sourceRoot.toAbsolutePath().normalize()), resolved.sourceRootPaths());
        assertTrue(resolved.workspaceBytes() > 0);
        assertThrows(IllegalArgumentException.class, () -> adapter.resolve(commandWithSourceRoot("src/main/java", "kotlin")));
        assertThrows(IllegalArgumentException.class, () -> adapter.resolve(commandWithSourceRoot("missing", "java")));
    }

    @Test
    void collectsProducedMissingAndProvenanceArtifactsWithBoundedReads() throws IOException {
        var collector = new FileSystemJoernArtifactCollector(tempDir.resolve("artifacts"));
        var command = command(true, true, true);
        var directory = tempDir.resolve("artifacts").resolve(artifactDirectory());
        write(directory.resolve(FileSystemJoernArtifactCollector.CPG), "cpg-bytes");
        write(directory.resolve(FileSystemJoernArtifactCollector.DATAFLOW), "{\"edges\":[]}");
        var runtime = new JoernRuntimeResult(
            "joern 1.2.3",
            image(),
            artifactDirectory(),
            List.of(JoernCpgDiagnostic.info(new SourceSnapshotId("snapshot-1"), "JOERN_QUERY_STARTED", "query bundle executed"))
        );

        var collected = collector.collect(command, runtime);
        var cpg = artifact(collected.artifacts(), FileSystemJoernArtifactCollector.CPG);
        var provenance = artifact(collected.artifacts(), FileSystemJoernArtifactCollector.PROVENANCE);

        assertEquals(3, collected.missingArtifactCount());
        assertEquals(3, collected.diagnostics().size());
        assertEquals("application/vnd.forensic-analytics.joern-cpg.v1+binary", cpg.artifact().type());
        assertEquals("application/vnd.forensic-analytics.joern-provenance.v1+json", provenance.artifact().type());
        assertEquals("cpg-bytes", new String(collector.read(bytesRequest(cpg, 100)).content(), StandardCharsets.UTF_8));
        assertEquals(AnalysisCompleteness.INCOMPLETE, collector.read(bytesRequest(provenance, 10_000)).artifact().completeness());
    }

    @Test
    void writesUnavailableProvenanceAndRejectsUnsafeArtifactReads() throws IOException {
        var collector = new FileSystemJoernArtifactCollector(tempDir.resolve("artifacts"));
        var command = command(false, false, false);
        var directory = tempDir.resolve("artifacts").resolve(artifactDirectory());
        write(directory.resolve(FileSystemJoernArtifactCollector.CPG), "cpg-bytes");
        var cpg = new AnalysisArtifactReference(
            new ArtifactReference(
                artifactDirectory() + "/" + FileSystemJoernArtifactCollector.CPG,
                "application/vnd.forensic-analytics.joern-cpg.v1+binary",
                sha256("cpg-bytes"),
                "cpg-bytes".getBytes(StandardCharsets.UTF_8).length
            ),
            AnalysisArtifactCategory.STATIC,
            PRODUCER_SERVICE,
            SEMANTIC_ARTIFACT_SCHEMA_VERSION,
            AnalysisCompleteness.COMPLETE,
            new ArtifactByteAccess(
                PRODUCER_SERVICE,
                FileSystemJoernArtifactCollector.BYTE_RETRIEVAL_CONTRACT,
                artifactDirectory() + "/" + FileSystemJoernArtifactCollector.CPG,
                ArtifactByteCustody.PRODUCER_RETAINED
            )
        );

        var unavailable = collector.collectUnavailable(
            command,
            new FileSystemJoernWorkspaceAdapter(tempDir.resolve("workspaces")).resolve(materializedWorkspace()),
            JoernCpgDiagnostic.error(new SourceSnapshotId("snapshot-1"), "JOERN_RUNTIME_UNAVAILABLE", "runtime unavailable", true)
        );

        assertEquals(1, unavailable.missingArtifactCount());
        assertEquals(AnalysisCompleteness.UNKNOWN, collector.read(bytesRequest(unavailable.artifacts().getFirst(), 10_000)).artifact().completeness());
        assertThrows(IllegalArgumentException.class, () -> collector.read(bytesRequest(cpg, 100, "wrong-schema", cpg.artifact().sha256(), cpg.artifact().sizeBytes())));
        assertThrows(IllegalStateException.class, () -> collector.read(bytesRequest(cpg, 100, SEMANTIC_ARTIFACT_SCHEMA_VERSION, cpg.artifact().sha256(), 1)));
        assertThrows(IllegalStateException.class, () -> collector.read(bytesRequest(cpg, 1)));
        assertThrows(IllegalStateException.class, () -> collector.read(bytesRequest(cpg, 100, SEMANTIC_ARTIFACT_SCHEMA_VERSION, "b".repeat(64), cpg.artifact().sizeBytes())));
        assertThrows(IllegalStateException.class, () -> collector.read(bytesRequest(
            new AnalysisArtifactReference(
                new ArtifactReference("joern-cpg/other/cpg.bin.zip", cpg.artifact().type(), cpg.artifact().sha256(), cpg.artifact().sizeBytes()),
                cpg.category(),
                cpg.producerService(),
                cpg.schemaVersion(),
                cpg.completeness(),
                cpg.byteAccess()
            ),
            100
        )));
    }

    private AnalyzeJoernCpgCommand materializedWorkspace() throws IOException {
        var workspaceRoot = tempDir.resolve("workspaces");
        var sourceRoot = workspaceRoot.resolve("joern-workspace-snapshot-1/src/main/java");
        write(sourceRoot.resolve("App.java"), "class App {}\n");
        return command(true, false, false);
    }

    private static AnalysisArtifactReference artifact(List<AnalysisArtifactReference> artifacts, String fileName) {
        return artifacts.stream()
            .filter(reference -> reference.artifact().path().endsWith("/" + fileName))
            .findFirst()
            .orElseThrow();
    }

    private static SemanticArtifactBytesRequest bytesRequest(AnalysisArtifactReference reference, long maxBytes) {
        return bytesRequest(reference, maxBytes, reference.schemaVersion(), reference.artifact().sha256(), reference.artifact().sizeBytes());
    }

    private static SemanticArtifactBytesRequest bytesRequest(
        AnalysisArtifactReference reference,
        long maxBytes,
        String schemaVersion,
        String expectedSha256,
        long expectedSizeBytes
    ) {
        return new SemanticArtifactBytesRequest(
            "request-read",
            "correlation-1",
            new AnalysisRunId("run-1"),
            new AnalysisJobId("job-1"),
            new SourceSnapshotId("snapshot-1"),
            reference.artifact().path(),
            expectedSha256,
            expectedSizeBytes,
            maxBytes,
            schemaVersion,
            Map.of("tenant", "demo")
        );
    }

    private static AnalyzeJoernCpgCommand command(boolean requireCallgraph, boolean requireControlflow, boolean requireDataflow) {
        return commandWithSourceRoot("src/main/java", "java", requireCallgraph, requireControlflow, requireDataflow);
    }

    private static AnalyzeJoernCpgCommand commandWithSourceRoot(String relativePath, String language) {
        return commandWithSourceRoot(relativePath, language, true, false, false);
    }

    private static AnalyzeJoernCpgCommand commandWithSourceRoot(
        String relativePath,
        String language,
        boolean requireCallgraph,
        boolean requireControlflow,
        boolean requireDataflow
    ) {
        return new AnalyzeJoernCpgCommand(
            new RequestMetadata(
                "request-1",
                "idempotency-1",
                "joern-cpg-analysis-v1",
                "correlation-1",
                new AnalysisRunId("run-1"),
                new AnalysisJobId("job-1"),
                new SourceSnapshotId("snapshot-1"),
                "joern-analysis-service-test",
                Map.of("tenant", "demo")
            ),
            new JoernCpgPolicy(
                2,
                1_000,
                10_000,
                60,
                image(),
                "queries-v1",
                requireCallgraph,
                requireControlflow,
                requireDataflow
            ),
            new SourceWorkspace(
                "joern-workspace-snapshot-1",
                List.of(new SourceRoot(relativePath, language)),
                List.of(reference("input.json"))
            )
        );
    }

    private static AnalysisArtifactReference reference(String path) {
        return new AnalysisArtifactReference(
            new ArtifactReference(path, "application/json", "a".repeat(64), 3),
            AnalysisArtifactCategory.STATIC,
            PRODUCER_SERVICE,
            SEMANTIC_ARTIFACT_SCHEMA_VERSION,
            AnalysisCompleteness.COMPLETE,
            new ArtifactByteAccess(
                PRODUCER_SERVICE,
                FileSystemJoernArtifactCollector.BYTE_RETRIEVAL_CONTRACT,
                path,
                ArtifactByteCustody.PRODUCER_RETAINED
            )
        );
    }

    private static String artifactDirectory() {
        return "joern-cpg/" + sha256("run-1|job-1|snapshot-1").substring(0, 24);
    }

    private static String image() {
        return "ghcr.io/joernio/joern@sha256:" + "a".repeat(64);
    }

    private static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }
}
