package de.burger.forensics.analytics.services.javaastanalysis.adapter.out.filesystem;

import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalysisArtifactCategory;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalysisJobId;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.DiagnosticSeverity;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.EvidenceKind;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.JavaAstDiagnostic;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.JavaSourceFact;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.RequestMetadata;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.ScanSummary;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.SourceLocation;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.SourceSnapshotId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileSystemAstResultArtifactWriterTest {
    @TempDir
    Path tempDir;

    @Test
    void writesDeterministicSourceFactArtifactMetadataAndContent() throws Exception {
        var writer = new FileSystemAstResultArtifactWriter(tempDir);
        var metadata = metadata();
        var fact = new JavaSourceFact(
            "fact-1",
            "java-method",
            new SourceLocation("src/main/java/A.java", "a.A", "run", 4, 9),
            "a.A#run()",
            "AST method a.A#run()",
            EvidenceKind.STATIC_SOURCE_FACT
        );
        var diagnostic = new JavaAstDiagnostic(
            "SYMBOL_RESOLUTION_NOT_CONFIGURED",
            "symbol solving is not configured",
            DiagnosticSeverity.WARNING,
            metadata.sourceSnapshotId(),
            "",
            0,
            0,
            false,
            true
        );

        var artifact = writer.write(metadata, List.of(fact), List.of(diagnostic), new ScanSummary(1, 1, 0, 0, 1, "JavaParser", "3.27.1"));
        var content = Files.readString(tempDir.resolve(artifact.artifact().path()));

        assertEquals(AnalysisArtifactCategory.STATIC, artifact.category());
        assertEquals("java-ast-analysis-service", artifact.producerService());
        assertEquals("application/vnd.forensic-analytics.java-ast-source-facts.v1+json", artifact.artifact().type());
        assertTrue(content.contains("\"signature\": \"a.A#run()\""));
        assertTrue(content.contains("\"code\": \"SYMBOL_RESOLUTION_NOT_CONFIGURED\""));
        assertEquals(64, artifact.artifact().sha256().length());
    }

    @Test
    void writesCompleteArtifactAndReportsWriteFailures() throws Exception {
        var writer = new FileSystemAstResultArtifactWriter(tempDir);
        var metadata = metadata();
        var artifact = writer.write(metadata, List.of(), List.of(), new ScanSummary(0, 0, 0, 0, 0, "JavaParser", "3.27.1"));

        assertEquals("java-ast/snapshot-1-job-1-source-facts.json", artifact.artifact().path());

        var fileRoot = Files.writeString(tempDir.resolve("not-a-directory"), "occupied");
        var failingWriter = new FileSystemAstResultArtifactWriter(fileRoot);
        assertThrows(
            UncheckedIOException.class,
            () -> failingWriter.write(metadata, List.of(), List.of(), new ScanSummary(0, 0, 0, 0, 0, "JavaParser", "3.27.1"))
        );
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
