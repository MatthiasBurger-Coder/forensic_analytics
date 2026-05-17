package de.burger.forensics.analytics.services.joerncpganalysis.adapter.out.filesystem;

import com.google.gson.JsonParser;
import de.burger.forensics.analytics.services.joerncpganalysis.application.JoernCpgArtifactException;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisJobId;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalyzeJoernCpgCommand;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.JoernCpgPolicy;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.JoernRuntimeResult;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.RequestMetadata;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.SourceRoot;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.SourceSnapshotId;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.SourceWorkspace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static de.burger.forensics.analytics.services.joerncpganalysis.adapter.out.filesystem.FileSystemJoernArtifactCollector.CALLGRAPH;
import static de.burger.forensics.analytics.services.joerncpganalysis.adapter.out.filesystem.FileSystemJoernArtifactCollector.CONTROLFLOW;
import static de.burger.forensics.analytics.services.joerncpganalysis.adapter.out.filesystem.FileSystemJoernArtifactCollector.CPG;
import static de.burger.forensics.analytics.services.joerncpganalysis.adapter.out.filesystem.FileSystemJoernArtifactCollector.DATAFLOW;
import static de.burger.forensics.analytics.services.joerncpganalysis.adapter.out.filesystem.FileSystemJoernArtifactCollector.PROVENANCE;
import static de.burger.forensics.analytics.services.joerncpganalysis.adapter.out.filesystem.FileSystemJoernArtifactCollector.SLICES;
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
                "joern-cpg-analysis-service-test",
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
            new SourceWorkspace("workspace-1", List.of(new SourceRoot("src/main/java", "java")), List.of())
        );
    }

    private static String image() {
        return "ghcr.io/joernio/joern@sha256:" + "a".repeat(64);
    }
}
