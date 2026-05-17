package de.burger.forensics.analytics.services.javaastanalysis.domain;

import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalysisArtifactCategory;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalysisJobId;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalyzeSourceSnapshotCommand;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.ArtifactReference;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.DiagnosticSeverity;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.EvidenceKind;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.JavaAstDiagnostic;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.JavaAstScanResult;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.JavaSourceFact;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.JavaSourceFile;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.RequestMetadata;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.ScanPolicy;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.ScanSummary;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.SourceLocation;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.SourceRoot;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.SourceSnapshotId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.stableId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JavaAstAnalysisDomainTest {
    @Test
    void stableIdsAreDeterministicAndDistinguishSourceFacts() {
        var first = stableId("snapshot-1", "java-method", "src/main/java/A.java", "a.A#run()", "4");
        var same = stableId("snapshot-1", "java-method", "src/main/java/A.java", "a.A#run()", "4");
        var overload = stableId("snapshot-1", "java-method", "src/main/java/A.java", "a.A#run(String)", "4");

        assertEquals(first, same);
        assertNotEquals(first, overload);
    }

    @Test
    void rejectsUnsafePathsAndSensitiveAttributes() {
        assertThrows(IllegalArgumentException.class, () -> new SourceRoot("/workspace/src/main/java", "java"));
        assertThrows(IllegalArgumentException.class, () -> new SourceRoot("../src/main/java", "java"));
        assertThrows(IllegalArgumentException.class, () -> new SourceRoot("C:/repo/src/main/java", "java"));
        assertThrows(IllegalArgumentException.class, () -> new SourceRoot("file:/repo/src/main/java", "java"));
        assertThrows(IllegalArgumentException.class, () -> new SourceRoot("src//main/java", "java"));
        assertThrows(
            IllegalArgumentException.class,
            () -> JavaAstAnalysisDomain.safeAttributes(Map.of("accessToken", "secret"))
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> JavaAstAnalysisDomain.safeAttributes(Map.of("path", "https://example.com/private"))
        );
    }

    @Test
    void javaAstFactsMustRemainStaticSourceFacts() {
        var location = new SourceLocation("src/main/java/A.java", "a.A", "run", 4, 9);
        var fact = new JavaSourceFact("fact-1", "java-method", location, "a.A#run()", "AST method a.A#run()", EvidenceKind.STATIC_SOURCE_FACT);

        assertEquals(EvidenceKind.STATIC_SOURCE_FACT, fact.evidenceKind());
        assertThrows(NullPointerException.class, () -> new JavaSourceFact("fact-2", "java-method", location, "a.A#run()", "summary", null));
    }

    @Test
    void completenessReflectsCompletenessAffectingDiagnostics() {
        var complete = new JavaAstScanResult(
            List.of(),
            List.of(JavaAstDiagnostic.info(new SourceSnapshotId("snapshot-1"), "INFO", "informational diagnostic")),
            new ScanSummary(1, 1, 0, 0, 0, "JavaParser", "3.27.1")
        );
        var incomplete = new JavaAstScanResult(
            List.of(),
            List.of(JavaAstDiagnostic.warning(
                new SourceSnapshotId("snapshot-1"),
                "SYMBOL_RESOLUTION_NOT_CONFIGURED",
                "symbol solving is not configured",
                "",
                0,
                0,
                true
            )),
            new ScanSummary(1, 1, 0, 0, 0, "JavaParser", "3.27.1")
        );

        assertEquals(AnalysisCompleteness.COMPLETE, complete.completeness());
        assertEquals(AnalysisCompleteness.INCOMPLETE, incomplete.completeness());
    }

    @Test
    void validatesSourceFilesPoliciesCommandsAndArtifacts() {
        assertEquals("A.java", new JavaSourceFile(".", "A.java", "", "", 0).sourcePath());
        assertEquals(2, new JavaSourceFile(".", "A.java", "\u00fc", "", 0).actualSizeBytes());
        assertThrows(IllegalArgumentException.class, () -> new JavaSourceFile("src/main/java", "A.java", "class A {}", "bad", 10));
        assertThrows(IllegalArgumentException.class, () -> new JavaSourceFile("src/main/java", "A.java", "class A {}", "", -1));
        assertThrows(IllegalArgumentException.class, () -> new ScanPolicy(0, 1, 1, false));
        assertThrows(IllegalArgumentException.class, () -> new ScanPolicy(100_001, 1, 1, false));
        assertThrows(IllegalArgumentException.class, () -> new ScanPolicy(1, 0, 1, false));
        assertThrows(IllegalArgumentException.class, () -> new ScanPolicy(1, 1_073_741_825L, 1, false));
        assertThrows(IllegalArgumentException.class, () -> new ScanPolicy(1, 1, 0, false));
        assertThrows(IllegalArgumentException.class, () -> new ScanPolicy(1, 1, 86_401, false));
        assertThrows(
            IllegalArgumentException.class,
            () -> new AnalyzeSourceSnapshotCommand(metadata(), new ScanPolicy(1, 1, 1, false), List.of(), List.of(new JavaSourceFile(".", "A.java", "", "", 0)))
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new AnalyzeSourceSnapshotCommand(metadata(), new ScanPolicy(1, 1, 1, false), List.of(new SourceRoot(".", "java")), List.of())
        );
        assertThrows(IllegalArgumentException.class, () -> new ArtifactReference("artifact.json", "application/json", "bad", 1));
        assertThrows(IllegalArgumentException.class, () -> new ArtifactReference("artifact.json", "application/json", "a".repeat(64), -1));
        assertThrows(NullPointerException.class, () -> new AnalysisArtifactReference(null, AnalysisArtifactCategory.STATIC, "producer", "schema", AnalysisCompleteness.COMPLETE));
    }

    @Test
    void validatesLocationsDiagnosticsAndSummaries() {
        var snapshotId = new SourceSnapshotId("snapshot-1");

        assertThrows(IllegalArgumentException.class, () -> new SourceLocation("A.java", "a.A", "run", 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new SourceLocation("A.java", "a.A", "run", 1, 0));
        assertEquals("", JavaAstDiagnostic.info(snapshotId, "INFO", "ok").sourcePath());
        assertEquals("diagnostic details redacted", JavaAstDiagnostic.info(snapshotId, "INFO", "file:/tmp/secret").message());
        assertThrows(
            IllegalArgumentException.class,
            () -> new JavaAstDiagnostic("BAD", "bad", DiagnosticSeverity.ERROR, snapshotId, "A.java", -1, 0, false, true)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new JavaAstDiagnostic("BAD", "bad", DiagnosticSeverity.ERROR, snapshotId, "A.java", 0, -1, false, true)
        );
        assertThrows(IllegalArgumentException.class, () -> new ScanSummary(-1, 0, 0, 0, 0, "JavaParser", "3.27.1"));
    }

    private static RequestMetadata metadata() {
        return new RequestMetadata(
            "request-1",
            "idempotency-1",
            "java-ast-analysis-v1",
            "correlation-1",
            new AnalysisRunId("run-1"),
            new AnalysisJobId("job-1"),
            new SourceSnapshotId("snapshot-1"),
            "java-ast-analysis-service-test",
            Map.of("tenant", "demo")
        );
    }
}
