package de.burger.forensics.analytics.services.joernanalysis.adapter.out.filesystem;

import com.google.gson.JsonParser;
import de.burger.forensics.analytics.services.joernanalysis.application.JoernCpgArtifactException;
import de.burger.forensics.analytics.services.joernanalysis.application.port.ResolvedJoernWorkspace;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalysisArtifactCategory;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalysisArtifactReference;
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
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.SourceRoot;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.SourceSnapshotId;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.SourceWorkspace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static de.burger.forensics.analytics.services.joernanalysis.adapter.out.filesystem.FileSystemJoernArtifactCollector.CALLGRAPH;
import static de.burger.forensics.analytics.services.joernanalysis.adapter.out.filesystem.FileSystemJoernArtifactCollector.CONTROLFLOW;
import static de.burger.forensics.analytics.services.joernanalysis.adapter.out.filesystem.FileSystemJoernArtifactCollector.CPG;
import static de.burger.forensics.analytics.services.joernanalysis.adapter.out.filesystem.FileSystemJoernArtifactCollector.DATAFLOW;
import static de.burger.forensics.analytics.services.joernanalysis.adapter.out.filesystem.FileSystemJoernArtifactCollector.PROVENANCE;
import static de.burger.forensics.analytics.services.joernanalysis.adapter.out.filesystem.FileSystemJoernArtifactCollector.SLICES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileSystemJoernArtifactCollectorTest {
    @TempDir
    Path tempDir;

    @Test
    void collectsStaticSemanticArtifactsAndWritesProvenance() throws Exception {
        var artifactDirectory = artifactDirectory();
        Files.writeString(artifactDirectory.resolve(CPG), "cpg");
        Files.writeString(artifactDirectory.resolve(CALLGRAPH), "{\"calls\":[]}");
        Files.writeString(artifactDirectory.resolve(CONTROLFLOW), "{\"cfg\":[]}");
        Files.writeString(artifactDirectory.resolve(DATAFLOW), "{\"dfg\":[]}");
        Files.writeString(artifactDirectory.resolve(SLICES), "{\"slices\":[]}");

        var result = new FileSystemJoernArtifactCollector(tempDir).collect(
            command(true, true, true, 1_000_000),
            runtimeResult()
        );

        assertEquals(0, result.missingArtifactCount());
        assertEquals(List.of(
            "joern-cpg/run-1/callgraph.json",
            "joern-cpg/run-1/controlflow.json",
            "joern-cpg/run-1/cpg.bin.zip",
            "joern-cpg/run-1/dataflow.json",
            "joern-cpg/run-1/joern-provenance.json",
            "joern-cpg/run-1/slices.json"
        ), result.artifacts().stream().map(reference -> reference.artifact().path()).toList());
        assertEquals(List.of(), result.diagnostics());
        assertTrue(result.artifacts().stream().allMatch(reference -> reference.category().name().equals("STATIC")));

        var provenance = JsonParser.parseString(Files.readString(artifactDirectory.resolve(PROVENANCE))).getAsJsonObject();
        assertEquals("run-1", provenance.get("analysisRunId").getAsString());
        assertEquals("snapshot-1", provenance.get("sourceSnapshotId").getAsString());
        assertEquals("COMPLETE", provenance.get("completeness").getAsString());
    }

    @Test
    void exposesMissingRequiredArtifactsAsCompletenessDiagnostics() throws Exception {
        var artifactDirectory = artifactDirectory();
        Files.writeString(artifactDirectory.resolve(CPG), "cpg");

        var result = new FileSystemJoernArtifactCollector(tempDir).collect(
            command(true, true, true, 1_000_000),
            runtimeResult()
        );

        assertEquals(4, result.missingArtifactCount());
        assertEquals(List.of(
            "joern-cpg/run-1/cpg.bin.zip",
            "joern-cpg/run-1/joern-provenance.json"
        ), result.artifacts().stream().map(reference -> reference.artifact().path()).toList());
        assertEquals(List.of(CALLGRAPH, CONTROLFLOW, DATAFLOW, SLICES), result.diagnostics().stream()
            .map(diagnostic -> diagnostic.artifactPath())
            .toList());
        assertEquals(AnalysisCompleteness.INCOMPLETE, result.artifacts().stream()
            .filter(reference -> reference.artifact().path().endsWith(PROVENANCE))
            .findFirst()
            .orElseThrow()
            .completeness());
    }

    @Test
    void rejectsArtifactsThatExceedPolicyLimit() throws Exception {
        var artifactDirectory = artifactDirectory();
        Files.writeString(artifactDirectory.resolve(CPG), "larger-than-limit");

        var collector = new FileSystemJoernArtifactCollector(tempDir);

        assertThrows(JoernCpgArtifactException.class, () -> collector.collect(
            command(false, false, false, 3),
            runtimeResult()
        ));
    }

    @Test
    void rejectsSymlinkedExpectedArtifacts() throws Exception {
        var artifactDirectory = artifactDirectory();
        Files.writeString(tempDir.resolve("outside-cpg.bin.zip"), "cpg");
        try {
            Files.createSymbolicLink(artifactDirectory.resolve(CPG), tempDir.resolve("outside-cpg.bin.zip"));
        } catch (UnsupportedOperationException ignored) {
            return;
        }

        var collector = new FileSystemJoernArtifactCollector(tempDir);

        assertThrows(JoernCpgArtifactException.class, () -> collector.collect(
            command(false, false, false, 1_000_000),
            runtimeResult()
        ));
    }

    @Test
    void rejectsSymlinkedProvenanceArtifactWithoutOverwritingTarget() throws Exception {
        var artifactDirectory = artifactDirectory();
        Files.writeString(artifactDirectory.resolve(CPG), "cpg");
        var outside = tempDir.resolve("outside-provenance.json");
        Files.writeString(outside, "outside");
        try {
            Files.createSymbolicLink(artifactDirectory.resolve(PROVENANCE), outside);
        } catch (UnsupportedOperationException | java.io.IOException ignored) {
            return;
        }

        var collector = new FileSystemJoernArtifactCollector(tempDir);

        assertThrows(JoernCpgArtifactException.class, () -> collector.collect(
            command(false, false, false, 1_000_000),
            runtimeResult()
        ));
        assertEquals("outside", Files.readString(outside));
    }

    @Test
    void rejectsSymlinkedArtifactParentDirectories() throws Exception {
        var outside = tempDir.resolve("outside-artifacts");
        Files.createDirectories(outside);
        try {
            Files.createSymbolicLink(tempDir.resolve("joern-cpg"), outside);
        } catch (UnsupportedOperationException | java.io.IOException ignored) {
            return;
        }

        var collector = new FileSystemJoernArtifactCollector(tempDir);

        assertThrows(JoernCpgArtifactException.class, () -> collector.collect(
            command(false, false, false, 1_000_000),
            runtimeResult()
        ));
    }

    @Test
    void writesUnavailableProvenanceArtifactAsUnknownCompleteness() throws Exception {
        var collector = new FileSystemJoernArtifactCollector(tempDir);

        var result = collector.collectUnavailable(
            command(true, true, true, 1_000_000),
            workspace(),
            JoernCpgDiagnostic.error(new SourceSnapshotId("snapshot-1"), "JOERN_RUNTIME_UNAVAILABLE", "Joern unavailable.", true)
        );

        assertEquals(1, result.artifacts().size());
        assertEquals(5, result.missingArtifactCount());
        assertEquals(List.of("JOERN_RUNTIME_UNAVAILABLE"), result.diagnostics().stream()
            .map(JoernCpgDiagnostic::code)
            .toList());
        assertTrue(result.artifacts().getFirst().artifact().path().endsWith(PROVENANCE));
        assertEquals(AnalysisCompleteness.UNKNOWN, result.artifacts().getFirst().completeness());

        var provenance = JsonParser.parseString(Files.readString(tempDir.resolve(result.artifacts().getFirst().artifact().path()))).getAsJsonObject();
        assertEquals("UNKNOWN", provenance.get("completeness").getAsString());
        assertEquals("JOERN_RUNTIME_UNAVAILABLE", provenance.getAsJsonArray("diagnostics").get(0).getAsString());
    }

    @Test
    void rejectsSymlinkedUnavailableProvenanceArtifactWithoutOverwritingTarget() throws Exception {
        var command = command(true, true, true, 1_000_000);
        var artifactDirectory = tempDir.resolve("joern-cpg").resolve(
            de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.sha256(
                "run-1|job-1|snapshot-1"
            ).substring(0, 24)
        );
        Files.createDirectories(artifactDirectory);
        var outside = tempDir.resolve("outside-unavailable-provenance.json");
        Files.writeString(outside, "outside");
        try {
            Files.createSymbolicLink(artifactDirectory.resolve(PROVENANCE), outside);
        } catch (UnsupportedOperationException | java.io.IOException ignored) {
            return;
        }

        var collector = new FileSystemJoernArtifactCollector(tempDir);

        assertThrows(JoernCpgArtifactException.class, () -> collector.collectUnavailable(
            command,
            workspace(),
            JoernCpgDiagnostic.error(new SourceSnapshotId("snapshot-1"), "JOERN_RUNTIME_UNAVAILABLE", "Joern unavailable.", true)
        ));
        assertEquals("outside", Files.readString(outside));
    }

    @Test
    void includesProvenanceArtifactInArtifactBytePolicy() throws Exception {
        var artifactDirectory = artifactDirectory();
        Files.writeString(artifactDirectory.resolve(CPG), "cpg");

        var collector = new FileSystemJoernArtifactCollector(tempDir);

        assertThrows(JoernCpgArtifactException.class, () -> collector.collect(
            command(false, false, false, 10),
            runtimeResult()
        ));
    }

    private Path artifactDirectory() throws Exception {
        var directory = tempDir.resolve("joern-cpg/run-1");
        Files.createDirectories(directory);
        return directory;
    }

    private static JoernRuntimeResult runtimeResult() {
        return new JoernRuntimeResult("joern-test-1", image(), "joern-cpg/run-1", List.of());
    }

    private ResolvedJoernWorkspace workspace() {
        return new ResolvedJoernWorkspace(
            new SourceSnapshotId("snapshot-1"),
            "joern-workspace-snapshot-1",
            tempDir.resolve("workspace"),
            List.of(tempDir.resolve("workspace/src/main/java")),
            100
        );
    }

    private static AnalyzeJoernCpgCommand command(
        boolean callgraph,
        boolean controlflow,
        boolean dataflow,
        long maxArtifactBytes
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
                1_000_000,
                maxArtifactBytes,
                60,
                image(),
                "queries-v1",
                callgraph,
                controlflow,
                dataflow
            ),
            new SourceWorkspace("joern-workspace-snapshot-1", List.of(new SourceRoot("src/main/java", "java")), List.of(inputArtifact()))
        );
    }

    private static AnalysisArtifactReference inputArtifact() {
        return new AnalysisArtifactReference(
            new ArtifactReference("source-package.zip", "application/zip", "a".repeat(64), 1),
            AnalysisArtifactCategory.STATIC,
            "repository-analysis-service",
            "source-package-v1",
            AnalysisCompleteness.COMPLETE,
            new ArtifactByteAccess(
                "repository-analysis-service",
                "repository-analysis.v1.GetRepositoryPreparation",
                "source-snapshot/snapshot-1",
                ArtifactByteCustody.PRODUCER_RETAINED
            )
        );
    }

    private static String image() {
        return "ghcr.io/joernio/joern@sha256:" + "a".repeat(64);
    }
}
