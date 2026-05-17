package de.burger.forensics.analytics.services.joerncpganalysis.domain;

import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisArtifactCategory;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisJobId;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalyzeJoernCpgCommand;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalyzeJoernCpgResult;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.ArtifactReference;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.DiagnosticSeverity;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.JoernArtifactCollectionResult;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.JoernCpgDiagnostic;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.JoernCpgPolicy;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.JoernCpgSummary;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.RequestMetadata;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.SourceRoot;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.SourceSnapshotId;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.SourceWorkspace;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.completeness;
import static de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.requireRelativePath;
import static de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.requireSha256;
import static de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.requireText;
import static de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.sha256;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JoernCpgAnalysisDomainTest {
    @Test
    void validatesSafePublicPathsAttributesAndPinnedImageReferences() {
        assertThrows(IllegalArgumentException.class, () -> new SourceRoot("../src", "java"));
        assertThrows(IllegalArgumentException.class, () -> new SourceRoot("/workspace/src", "java"));
        assertThrows(IllegalArgumentException.class, () -> new SourceWorkspace("workspace/1", List.of(new SourceRoot("src", "java")), List.of()));
        assertThrows(IllegalArgumentException.class, () -> metadata(Map.of("api_token", "secret")));
        assertThrows(IllegalArgumentException.class, () -> metadata(Map.of("tenant", "file:/private/path")));
        assertThrows(IllegalArgumentException.class, () -> new JoernCpgPolicy(
            1,
            100,
            100,
            60,
            "ghcr.io/joernio/joern:latest",
            "queries-v1",
            true,
            true,
            true
        ));
    }

    @Test
    void sortsSourceRootsAndKeepsIncompleteEvidenceExplicit() {
        var workspace = new SourceWorkspace(
            "workspace-1",
            List.of(new SourceRoot("z", "java"), new SourceRoot("a", "JAVA")),
            List.of()
        );
        var diagnostics = List.of(JoernCpgDiagnostic.warning(
            new SourceSnapshotId("snapshot-1"),
            "JOERN_ARTIFACT_MISSING",
            "missing static semantic artifact",
            "callgraph.json",
            true
        ));

        assertEquals(List.of("a", "z"), workspace.sourceRoots().stream().map(SourceRoot::relativePath).toList());
        assertEquals(AnalysisCompleteness.INCOMPLETE, completeness(diagnostics));
        assertEquals(DiagnosticSeverity.WARNING, diagnostics.getFirst().severity());
    }

    @Test
    void rejectsInvalidPolicyCommandArtifactAndSummaryBoundaries() {
        assertThrows(IllegalArgumentException.class, () -> new SourceWorkspace("workspace-1", List.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new SourceWorkspace(".", List.of(new SourceRoot("src", "java")), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new SourceWorkspace("workspace:1", List.of(new SourceRoot("src", "java")), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new JoernCpgPolicy(0, 100, 100, 60, image(), "queries-v1", false, false, false));
        assertThrows(IllegalArgumentException.class, () -> new JoernCpgPolicy(1, 0, 100, 60, image(), "queries-v1", false, false, false));
        assertThrows(IllegalArgumentException.class, () -> new JoernCpgPolicy(1, 100, 0, 60, image(), "queries-v1", false, false, false));
        assertThrows(IllegalArgumentException.class, () -> new JoernCpgPolicy(1, 100, 100, 0, image(), "queries-v1", false, false, false));
        assertThrows(IllegalArgumentException.class, () -> new JoernCpgPolicy(1, 100, 100, 86_401, image(), "queries-v1", false, false, false));
        assertThrows(IllegalArgumentException.class, () -> new ArtifactReference("artifact.json", "application/json", "x", 1));
        assertThrows(IllegalArgumentException.class, () -> new ArtifactReference("artifact.json", "application/json", "a".repeat(64), -1));
        assertThrows(IllegalArgumentException.class, () -> new JoernArtifactCollectionResult(List.of(), -1, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new JoernCpgSummary(-1, 0, 0, "joern", image(), "queries-v1", "service", "schema"));
        assertThrows(IllegalArgumentException.class, () -> new JoernCpgSummary(0, -1, 0, "joern", image(), "queries-v1", "service", "schema"));
        assertThrows(IllegalArgumentException.class, () -> new JoernCpgSummary(0, 0, -1, "joern", image(), "queries-v1", "service", "schema"));
        assertThrows(IllegalArgumentException.class, () -> new AnalyzeJoernCpgCommand(
            metadata(Map.of("tenant", "demo")),
            policy(1),
            new SourceWorkspace("workspace-1", List.of(new SourceRoot("a", "java"), new SourceRoot("b", "java")), List.of())
        ));
    }

    @Test
    void sortsArtifactReferencesAndSanitizesDiagnostics() {
        var inputArtifacts = List.of(reference("z.json"), reference("a.json"));
        var workspace = new SourceWorkspace("workspace-1", List.of(new SourceRoot("src", "java")), inputArtifacts);
        var diagnostics = List.of(
            JoernCpgDiagnostic.error(new SourceSnapshotId("snapshot-1"), "JOERN_RUNTIME_UNAVAILABLE", "C:\\secret\\trace.log", true),
            JoernCpgDiagnostic.info(new SourceSnapshotId("snapshot-1"), "JOERN_OPTIONAL_QUERY_SKIPPED", "line one\nline two")
        );
        var result = new AnalyzeJoernCpgResult(
            metadata(Map.of("tenant", "demo")),
            AnalysisCompleteness.COMPLETE,
            inputArtifacts,
            new JoernCpgSummary(1, 2, 0, "joern", image(), "queries-v1", "joern-cpg-analysis-service", "schema"),
            diagnostics
        );

        assertEquals(List.of("a.json", "z.json"), workspace.inputArtifacts().stream().map(artifact -> artifact.artifact().path()).toList());
        assertEquals(List.of("a.json", "z.json"), result.semanticArtifacts().stream().map(artifact -> artifact.artifact().path()).toList());
        assertEquals(List.of("JOERN_OPTIONAL_QUERY_SKIPPED", "JOERN_RUNTIME_UNAVAILABLE"), result.diagnostics().stream()
            .map(JoernCpgDiagnostic::code)
            .toList());
        assertEquals("diagnostic details redacted", result.diagnostics().get(1).message());
        assertEquals(DiagnosticSeverity.INFO, result.diagnostics().getFirst().severity());
        assertTrue(result.diagnostics().getFirst().message().contains("line one line two"));
    }

    @Test
    void validatesTextRelativePathAndDigestHelpers() {
        assertEquals("src/main/java", requireRelativePath("src\\main\\java", "path"));
        assertEquals(64, sha256("fixture").length());
        assertEquals("a".repeat(64), requireSha256("A".repeat(64), "sha"));
        assertEquals("value", requireText(" value ", "text"));

        assertThrows(IllegalArgumentException.class, () -> requireRelativePath("file:/tmp/source", "path"));
        assertThrows(IllegalArgumentException.class, () -> requireRelativePath("https://example.test/source", "path"));
        assertThrows(IllegalArgumentException.class, () -> requireRelativePath("C:/tmp/source", "path"));
        assertThrows(IllegalArgumentException.class, () -> requireRelativePath("src//main", "path"));
        assertThrows(IllegalArgumentException.class, () -> requireRelativePath("src/../main", "path"));
        assertThrows(IllegalArgumentException.class, () -> requireSha256("not-a-digest", "sha"));
        assertThrows(IllegalArgumentException.class, () -> requireText(" ", "text"));
    }

    private static RequestMetadata metadata(Map<String, String> attributes) {
        return new RequestMetadata(
            "request-1",
            "idempotency-1",
            "joern-cpg-analysis-v1",
            "correlation-1",
            new AnalysisRunId("run-1"),
            new AnalysisJobId("job-1"),
            new SourceSnapshotId("snapshot-1"),
            "joern-cpg-analysis-service-test",
            attributes
        );
    }

    private static JoernCpgPolicy policy(int maxSourceRoots) {
        return new JoernCpgPolicy(maxSourceRoots, 1_000, 1_000, 60, image(), "queries-v1", true, true, true);
    }

    private static AnalysisArtifactReference reference(String path) {
        return new AnalysisArtifactReference(
            new ArtifactReference(path, "application/json", "a".repeat(64), 1),
            AnalysisArtifactCategory.STATIC,
            "joern-cpg-analysis-service",
            "schema",
            AnalysisCompleteness.COMPLETE
        );
    }

    private static String image() {
        return "ghcr.io/joernio/joern@sha256:" + "a".repeat(64);
    }
}
