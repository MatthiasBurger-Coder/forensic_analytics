package de.burger.forensics.analytics.services.javaastanalysis.application;

import de.burger.forensics.analytics.services.javaastanalysis.application.port.AstResultArtifactWriterPort;
import de.burger.forensics.analytics.services.javaastanalysis.application.port.JavaSourceScannerPort;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalysisArtifactCategory;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalysisJobId;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalyzeSourceSnapshotCommand;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.ArtifactReference;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.JavaAstDiagnostic;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.JavaAstScanResult;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.JavaSourceFact;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.JavaSourceFile;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.RequestMetadata;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.ScanPolicy;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.ScanSummary;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.SourceRoot;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.SourceSnapshotId;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.sha256;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaAstAnalysisApplicationServiceTest {
    @Test
    void scansAndWritesArtifactResult() {
        var service = new JavaAstAnalysisApplicationService(new FakeScanner(), new FakeWriter());

        var result = service.analyze(command(file("src/main/java", "A.java", "class A { void run() {} }"), 10, 10_000));

        assertEquals(AnalysisCompleteness.COMPLETE, result.completeness());
        assertEquals("java-ast/source-facts.json", result.sourceFactArtifact().artifact().path());
        assertEquals(1, result.summary().sourceFactCount());
    }

    @Test
    void rejectsPolicyAndChecksumMismatchesBeforeScanning() {
        var service = new JavaAstAnalysisApplicationService(new FakeScanner(), new FakeWriter());
        var tooManyFiles = new AnalyzeSourceSnapshotCommand(
            metadata(),
            new ScanPolicy(1, 10_000, 60, false),
            List.of(new SourceRoot("src/main/java", "java")),
            List.of(
                file("src/main/java", "A.java", "class A {}"),
                file("src/main/java", "B.java", "class B {}")
            )
        );
        var checksumMismatch = command(
            new JavaSourceFile("src/main/java", "A.java", "class A {}", "0".repeat(64), 10),
            10,
            10_000
        );
        var tooManyBytes = command(file("src/main/java", "A.java", "class A {}"), 10, 1);
        var underReportedBytes = command(new JavaSourceFile("src/main/java", "A.java", "class A {}", sha256("class A {}"), 0), 10, 1);
        var blankChecksum = command(new JavaSourceFile("src/main/java", "A.java", "class A {}", "", 10), 10, 10_000);

        assertThrows(IllegalArgumentException.class, () -> service.analyze(tooManyFiles));
        assertThrows(IllegalArgumentException.class, () -> service.analyze(checksumMismatch));
        assertThrows(IllegalArgumentException.class, () -> service.analyze(tooManyBytes));
        assertThrows(IllegalArgumentException.class, () -> service.analyze(underReportedBytes));
        assertEquals(AnalysisCompleteness.COMPLETE, service.analyze(blankChecksum).completeness());
    }

    @Test
    void enforcesScanTimeoutAgainstScannerExecution() {
        var service = new JavaAstAnalysisApplicationService(command -> {
            try {
                Thread.sleep(2_000);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
            }
            return new JavaAstScanResult(List.of(), List.of(), new ScanSummary(1, 1, 0, 0, 0, "JavaParser", "3.27.1"));
        }, new FakeWriter());

        var started = System.nanoTime();
        var timeout = assertThrows(JavaAstAnalysisTimeoutException.class, () -> service.analyze(command(
            file("src/main/java", "A.java", "class A {}"),
            10,
            10_000,
            1
        )));
        var elapsedMillis = (System.nanoTime() - started) / 1_000_000;

        assertEquals("Java AST analysis timed out after 1 seconds.", timeout.getMessage());
        assertTrue(elapsedMillis < 1_900);
    }

    private static AnalyzeSourceSnapshotCommand command(JavaSourceFile file, int maxFiles, long maxBytes) {
        return command(file, maxFiles, maxBytes, 60);
    }

    private static AnalyzeSourceSnapshotCommand command(JavaSourceFile file, int maxFiles, long maxBytes, long timeoutSeconds) {
        return new AnalyzeSourceSnapshotCommand(
            metadata(),
            new ScanPolicy(maxFiles, maxBytes, timeoutSeconds, false),
            List.of(new SourceRoot("src/main/java", "java")),
            List.of(file)
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

    private static JavaSourceFile file(String root, String path, String content) {
        return new JavaSourceFile(root, path, content, sha256(content), content.getBytes(StandardCharsets.UTF_8).length);
    }

    private static final class FakeScanner implements JavaSourceScannerPort {
        @Override
        public JavaAstScanResult scan(AnalyzeSourceSnapshotCommand command) {
            return new JavaAstScanResult(
                List.of(),
                List.of(),
                new ScanSummary(command.sourceFiles().size(), command.sourceFiles().size(), 0, 0, 1, "JavaParser", "3.27.1")
            );
        }
    }

    private static final class FakeWriter implements AstResultArtifactWriterPort {
        @Override
        public AnalysisArtifactReference write(
            RequestMetadata metadata,
            List<JavaSourceFact> sourceFacts,
            List<JavaAstDiagnostic> diagnostics,
            ScanSummary summary
        ) {
            return new AnalysisArtifactReference(
                new ArtifactReference("java-ast/source-facts.json", "application/json", "a".repeat(64), 10),
                AnalysisArtifactCategory.STATIC,
                "java-ast-analysis-service",
                metadata.schemaVersion(),
                AnalysisCompleteness.COMPLETE
            );
        }
    }
}
