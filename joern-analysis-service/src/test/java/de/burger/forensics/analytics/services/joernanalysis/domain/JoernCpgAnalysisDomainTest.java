package de.burger.forensics.analytics.services.joernanalysis.domain;

import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalysisArtifactCategory;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalysisJobId;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalyzeJoernCpgCommand;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalyzeJoernCpgResult;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.ArtifactByteAccess;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.ArtifactByteCustody;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.ArtifactReference;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.DiagnosticSeverity;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.JoernArtifactCollectionResult;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.JoernCpgDiagnostic;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.JoernCpgPolicy;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.JoernCpgSummary;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.JoernMaterializationPolicy;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.MaterializationMetadata;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.MaterializedPackageDescriptor;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.MaterializeJoernWorkspaceCommand;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.PackageAvailability;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.RequestMetadata;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.SourceRoot;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.SourceSnapshotId;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.SourceWorkspace;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.completeness;
import static de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.requireRelativePath;
import static de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.requireSha256;
import static de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.requireText;
import static de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.sha256;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JoernCpgAnalysisDomainTest {
    @Test
    void validatesSafePublicPathsAttributesAndPinnedImageReferences() {
        assertThrows(IllegalArgumentException.class, () -> new SourceRoot("../src", "java"));
        assertThrows(IllegalArgumentException.class, () -> new SourceRoot("/workspace/src", "java"));
        assertThrows(IllegalArgumentException.class, () -> new SourceWorkspace("workspace/1", List.of(new SourceRoot("src", "java")), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new SourceWorkspace("workspace\\1", List.of(new SourceRoot("src", "java")), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new SourceWorkspace("workspace-1", List.of(new SourceRoot("src", "java")), List.of()));
        assertThrows(IllegalArgumentException.class, () -> metadata(Map.of("api_token", "secret")));
        assertThrows(IllegalArgumentException.class, () -> metadata(Map.of("tenant", "file:/private/path")));
        assertThrows(IllegalArgumentException.class, () -> metadata(Map.of("tenant", "https://example.test/workspace")));
        assertThrows(IllegalArgumentException.class, () -> metadata(Map.of("tenant", "//server/share")));
        assertThrows(IllegalArgumentException.class, () -> metadata(Map.of("tenant", "/mnt/private/workspace")));
        assertThrows(IllegalArgumentException.class, () -> metadata(Map.of("tenant", "/home/user/workspace")));
        assertThrows(IllegalArgumentException.class, () -> metadata(Map.of("tenant", "/var/tmp/workspace")));
        assertThrows(IllegalArgumentException.class, () -> metadata(Map.of("tenant", "/tmp/workspace")));
        assertThrows(IllegalArgumentException.class, () -> metadata(Map.of("tenant", "/root/workspace")));
        assertThrows(IllegalArgumentException.class, () -> metadata(Map.of("tenant", "C:/tmp/workspace")));
        assertThrows(IllegalArgumentException.class, () -> byteAccess("/private/artifact"));
        assertThrows(IllegalArgumentException.class, () -> byteAccess("FILE:/tmp/artifact"));
        assertThrows(IllegalArgumentException.class, () -> byteAccess("https://example.test/artifact"));
        assertThrows(IllegalArgumentException.class, () -> byteAccess("C:/tmp/artifact"));
        assertThrows(IllegalArgumentException.class, () -> byteAccess("artifacts/./cpg.bin.zip"));
        assertThrows(IllegalArgumentException.class, () -> byteAccess("artifacts//cpg.bin.zip"));
        assertThrows(IllegalArgumentException.class, () -> byteAccess("artifacts/../cpg.bin.zip"));
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
            "joern-workspace-1",
            List.of(new SourceRoot("z", "java"), new SourceRoot("a", "JAVA")),
            List.of(reference("input.json"))
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
        assertThrows(IllegalArgumentException.class, () -> new SourceWorkspace("joern-workspace-1", List.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new SourceWorkspace(
            "joern-workspace-1",
            List.of(new SourceRoot("src", "java")),
            List.of()
        ));
        assertThrows(IllegalArgumentException.class, () -> new SourceWorkspace(".", List.of(new SourceRoot("src", "java")), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new SourceWorkspace("..", List.of(new SourceRoot("src", "java")), List.of()));
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
            new SourceWorkspace(
                "joern-workspace-snapshot-1",
                List.of(new SourceRoot("a", "java"), new SourceRoot("b", "java")),
                List.of(reference("input.json"))
            )
        ));
        assertThrows(IllegalArgumentException.class, () -> new AnalyzeJoernCpgCommand(
            metadata(Map.of("tenant", "demo")),
            policy(2),
            new SourceWorkspace(
                "joern-workspace-other-snapshot",
                List.of(new SourceRoot("src", "java")),
                List.of(reference("input.json"))
            )
        ));
    }

    @Test
    void sortsArtifactReferencesAndSanitizesDiagnostics() {
        var inputArtifacts = List.of(reference("z.json"), reference("a.json"));
        var workspace = new SourceWorkspace("joern-workspace-1", List.of(new SourceRoot("src", "java")), inputArtifacts);
        var diagnostics = List.of(
            JoernCpgDiagnostic.error(new SourceSnapshotId("snapshot-1"), "JOERN_RUNTIME_UNAVAILABLE", "C:\\secret\\trace.log", true),
            JoernCpgDiagnostic.info(new SourceSnapshotId("snapshot-1"), "JOERN_OPTIONAL_QUERY_SKIPPED", "line one\nline two")
        );
        var result = new AnalyzeJoernCpgResult(
            metadata(Map.of("tenant", "demo")),
            AnalysisCompleteness.COMPLETE,
            inputArtifacts,
            new JoernCpgSummary(1, 2, 0, "joern", image(), "queries-v1", "joern-analysis-service", "schema"),
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
        assertEquals("diagnostic details redacted", JoernCpgDiagnostic.info(
            new SourceSnapshotId("snapshot-1"),
            "SOURCE_SNIPPET",
            "public class App {}"
        ).message());
        assertEquals("diagnostic details redacted", JoernCpgDiagnostic.info(
            new SourceSnapshotId("snapshot-1"),
            "SOURCE_SNIPPET",
            "private class App {}"
        ).message());
        assertEquals("diagnostic details redacted", JoernCpgDiagnostic.info(
            new SourceSnapshotId("snapshot-1"),
            "SOURCE_SNIPPET",
            "protected class App {}"
        ).message());
        assertEquals("diagnostic details redacted", JoernCpgDiagnostic.info(
            new SourceSnapshotId("snapshot-1"),
            "SOURCE_SNIPPET",
            "import java.util.List;"
        ).message());
        assertEquals("diagnostic details redacted", JoernCpgDiagnostic.info(
            new SourceSnapshotId("snapshot-1"),
            "SOURCE_SNIPPET",
            "package demo;"
        ).message());
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
        assertThrows(IllegalArgumentException.class, () -> requireRelativePath("FILE:/tmp/source", "path"));
        assertThrows(IllegalArgumentException.class, () -> requireRelativePath("src//main", "path"));
        assertThrows(IllegalArgumentException.class, () -> requireRelativePath("src/../main", "path"));
        assertThrows(IllegalArgumentException.class, () -> requireRelativePath("src/./main", "path"));
        assertThrows(IllegalArgumentException.class, () -> requireSha256("not-a-digest", "sha"));
        assertThrows(IllegalArgumentException.class, () -> requireText(" ", "text"));
    }

    @Test
    void validatesJoernMaterializationPoliciesAndPackageReadiness() {
        var sourcePackage = packageDescriptor("source package", PackageAvailability.AVAILABLE, AnalysisCompleteness.COMPLETE, 10);
        var buildPackage = packageDescriptor("build-output package", PackageAvailability.AVAILABLE, AnalysisCompleteness.COMPLETE, 10);

        var command = new MaterializeJoernWorkspaceCommand(
            materializationMetadata(),
            List.of(new SourceRoot("src/main/java", "java")),
            sourcePackage,
            buildPackage,
            materializationPolicy()
        );

        assertEquals(List.of("src/main/java"), command.sourceRoots().stream().map(SourceRoot::relativePath).toList());
        assertThrows(IllegalArgumentException.class, () -> new JoernMaterializationPolicy(0, 100, 100, 1, true, true, true, true));
        assertThrows(IllegalArgumentException.class, () -> new JoernMaterializationPolicy(1, 0, 100, 1, true, true, true, true));
        assertThrows(IllegalArgumentException.class, () -> new JoernMaterializationPolicy(1, 100, 0, 1, true, true, true, true));
        assertThrows(IllegalArgumentException.class, () -> new JoernMaterializationPolicy(1, 100, 100, 0, true, true, true, true));
        assertThrows(IllegalArgumentException.class, () -> new JoernMaterializationPolicy(1, 100, 100, 1, false, true, true, true));
        assertThrows(IllegalArgumentException.class, () -> new JoernMaterializationPolicy(1, 100, 100, 1, true, false, true, true));
        assertThrows(IllegalArgumentException.class, () -> new JoernMaterializationPolicy(1, 100, 100, 1, true, true, false, true));
        assertThrows(IllegalArgumentException.class, () -> new JoernMaterializationPolicy(1, 100, 100, 1, true, true, true, false));
        assertThrows(IllegalArgumentException.class, () -> packageDescriptor("source package", PackageAvailability.PENDING, AnalysisCompleteness.COMPLETE, 10));
        assertThrows(IllegalArgumentException.class, () -> packageDescriptor("source package", PackageAvailability.AVAILABLE, AnalysisCompleteness.INCOMPLETE, 10));
        assertThrows(IllegalArgumentException.class, () -> packageDescriptor("source package", PackageAvailability.AVAILABLE, AnalysisCompleteness.COMPLETE, 0));
        assertThrows(IllegalArgumentException.class, () -> new MaterializeJoernWorkspaceCommand(
            materializationMetadata(),
            List.of(new SourceRoot("src", "java"), new SourceRoot("generated", "java")),
            sourcePackage,
            buildPackage,
            new JoernMaterializationPolicy(1, 100, 100, 1, true, true, true, true)
        ));
        assertThrows(IllegalArgumentException.class, () -> new MaterializeJoernWorkspaceCommand(
            materializationMetadata(),
            List.of(new SourceRoot("src", "java")),
            sourcePackage,
            packageDescriptor("build-output package", PackageAvailability.AVAILABLE, AnalysisCompleteness.COMPLETE, 101),
            new JoernMaterializationPolicy(1, 100, 100, 1, true, true, true, true)
        ));
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
            "joern-analysis-service-test",
            attributes
        );
    }

    private static MaterializationMetadata materializationMetadata() {
        return new MaterializationMetadata(
            "request-1",
            "idempotency-1",
            "joern-materialization-v1",
            "correlation-1",
            new AnalysisRunId("run-1"),
            new AnalysisJobId("job-1"),
            new SourceSnapshotId("snapshot-1"),
            Map.of("tenant", "demo")
        );
    }

    private static JoernMaterializationPolicy materializationPolicy() {
        return new JoernMaterializationPolicy(2, 1_000, 100, 10, true, true, true, true);
    }

    private static MaterializedPackageDescriptor packageDescriptor(
        String packageName,
        PackageAvailability availability,
        AnalysisCompleteness completeness,
        long sizeBytes
    ) {
        return new MaterializedPackageDescriptor(
            packageName,
            availability,
            new ArtifactReference(packageName.replace(' ', '-') + "-manifest.json", "application/json", "b".repeat(64), 1),
            new ArtifactReference(packageName.replace(' ', '-') + ".zip", "application/zip", "a".repeat(64), sizeBytes),
            "repository-analysis-service",
            "package-v1",
            completeness,
            new ArtifactByteAccess(
                "repository-analysis-service",
                "repository-analysis.v1.GetRepositoryPreparation",
                "source-snapshot/snapshot-1/" + packageName.replace(' ', '-') + ".zip",
                ArtifactByteCustody.PRODUCER_RETAINED
            )
        );
    }

    private static JoernCpgPolicy policy(int maxSourceRoots) {
        return new JoernCpgPolicy(maxSourceRoots, 1_000, 1_000, 60, image(), "queries-v1", true, true, true);
    }

    private static AnalysisArtifactReference reference(String path) {
        return new AnalysisArtifactReference(
            new ArtifactReference(path, "application/json", "a".repeat(64), 1),
            AnalysisArtifactCategory.STATIC,
            "joern-analysis-service",
            "schema",
            AnalysisCompleteness.COMPLETE,
            new ArtifactByteAccess(
                "joern-analysis-service",
                "joern-cpg-analysis.v1.JoernCpgAnalysisService.GetSemanticArtifactBytes",
                path,
                ArtifactByteCustody.PRODUCER_RETAINED
            )
        );
    }

    private static ArtifactByteAccess byteAccess(String retrievalReference) {
        return new ArtifactByteAccess(
            "joern-analysis-service",
            "joern-cpg-analysis.v1.JoernCpgAnalysisService.GetSemanticArtifactBytes",
            retrievalReference,
            ArtifactByteCustody.PRODUCER_RETAINED
        );
    }

    private static String image() {
        return "ghcr.io/joernio/joern@sha256:" + "a".repeat(64);
    }
}
