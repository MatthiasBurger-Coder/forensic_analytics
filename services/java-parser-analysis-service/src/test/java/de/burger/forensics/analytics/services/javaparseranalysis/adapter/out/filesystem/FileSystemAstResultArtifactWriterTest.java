package de.burger.forensics.analytics.services.javaparseranalysis.adapter.out.filesystem;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.AnalysisArtifactCategory;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.AnalysisJobId;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.DiagnosticSeverity;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.EvidenceKind;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.JavaAstDiagnostic;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.JavaSourceFact;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.RequestMetadata;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.ScanSummary;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.SourceLocation;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.SourceFactArtifactBytesRequest;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.SourceSnapshotId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
            "src/main/java",
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
        assertEquals("java-parser-analysis-service", artifact.producerService());
        assertEquals("application/vnd.forensic-analytics.java-ast-source-facts.v1+json", artifact.artifact().type());
        assertTrue(content.contains("\"signature\": \"a.A#run()\""));
        assertTrue(content.contains("\"code\": \"SYMBOL_RESOLUTION_NOT_CONFIGURED\""));
        assertEquals(64, artifact.artifact().sha256().length());
    }

    @Test
    void writesContractCompliantDeterministicSourceFactPayload() throws Exception {
        var writer = new FileSystemAstResultArtifactWriter(tempDir);
        var metadata = metadata();
        var fact = new JavaSourceFact(
            "fact-1",
            "java-method",
            "src/main/java",
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
        var summary = new ScanSummary(1, 1, 0, 0, 1, "JavaParser", "3.27.1");

        var artifact = writer.write(metadata, List.of(fact), List.of(diagnostic), summary);
        var firstPayload = Files.readString(tempDir.resolve(artifact.artifact().path()));
        var repeated = writer.write(metadata, List.of(fact), List.of(diagnostic), summary);
        var secondPayload = Files.readString(tempDir.resolve(repeated.artifact().path()));
        var document = JsonParser.parseString(firstPayload).getAsJsonObject();

        assertEquals(firstPayload, secondPayload);
        assertEquals(artifact.artifact().sha256(), repeated.artifact().sha256());
        assertTrue(Files.readString(contractSchema()).contains("application/vnd.forensic-analytics.java-ast-source-facts.v1+json"));
        assertEquals(
            Set.of("schemaVersion", "analysisRunId", "analysisJobId", "sourceSnapshotId", "summary", "sourceFacts", "diagnostics"),
            document.keySet()
        );
        assertEquals("java-ast-analysis-v1", document.get("schemaVersion").getAsString());
        assertEquals("run-1", document.get("analysisRunId").getAsString());
        assertEquals("job-1", document.get("analysisJobId").getAsString());
        assertEquals("snapshot-1", document.get("sourceSnapshotId").getAsString());
        assertEquals(1, document.getAsJsonObject("summary").get("sourceFactCount").getAsInt());
        var sourceFact = document.getAsJsonArray("sourceFacts").get(0).getAsJsonObject();
        assertEquals(Set.of("factId", "factType", "sourceRoot", "location", "signature", "summary", "evidenceKind"), sourceFact.keySet());
        assertEquals("java-method", sourceFact.get("factType").getAsString());
        assertEquals("src/main/java", sourceFact.get("sourceRoot").getAsString());
        assertEquals("STATIC_SOURCE_FACT", sourceFact.get("evidenceKind").getAsString());
        var location = sourceFact.getAsJsonObject("location");
        assertEquals("src/main/java/A.java", location.get("sourcePath").getAsString());
        assertEquals("a.A", location.get("fullyQualifiedClassName").getAsString());
        assertEquals("run", location.get("methodName").getAsString());
        assertEquals(4, location.get("lineNumber").getAsInt());
        var diagnosticDocument = document.getAsJsonArray("diagnostics").get(0).getAsJsonObject();
        assertEquals(
            Set.of("code", "message", "severity", "sourceSnapshotId", "sourcePath", "lineNumber", "columnNumber", "retryable", "affectsCompleteness"),
            diagnosticDocument.keySet()
        );
        assertEquals("WARNING", diagnosticDocument.get("severity").getAsString());
        assertEquals("snapshot-1", diagnosticDocument.get("sourceSnapshotId").getAsString());
        assertEquals("", diagnosticDocument.get("sourcePath").getAsString());
        assertEquals(0, diagnosticDocument.get("lineNumber").getAsInt());
        assertEquals(0, diagnosticDocument.get("columnNumber").getAsInt());
        assertTrue(!diagnosticDocument.get("retryable").getAsBoolean());
        assertTrue(diagnosticDocument.get("affectsCompleteness").getAsBoolean());
    }

    @Test
    void writesCompleteArtifactAndReportsWriteFailures() throws Exception {
        var writer = new FileSystemAstResultArtifactWriter(tempDir);
        var metadata = metadata();
        var artifact = writer.write(metadata, List.of(), List.of(), new ScanSummary(0, 0, 0, 0, 0, "JavaParser", "3.27.1"));

        assertEquals("java-parser-analysis/snapshot-1-job-1-source-facts.json", artifact.artifact().path());

        var fileRoot = Files.writeString(tempDir.resolve("not-a-directory"), "occupied");
        var failingWriter = new FileSystemAstResultArtifactWriter(fileRoot);
        assertThrows(
            UncheckedIOException.class,
            () -> failingWriter.write(metadata, List.of(), List.of(), new ScanSummary(0, 0, 0, 0, 0, "JavaParser", "3.27.1"))
        );
    }

    @Test
    void createsMissingArtifactRootBeforeWritingSourceFactBytes() {
        var artifactRoot = tempDir.resolve("missing-artifact-root");
        var writer = new FileSystemAstResultArtifactWriter(artifactRoot);

        var artifact = writer.write(metadata(), List.of(), List.of(), new ScanSummary(0, 0, 0, 0, 0, "JavaParser", "3.27.1"));

        assertTrue(Files.isRegularFile(artifactRoot.resolve(artifact.artifact().path())));
    }

    @Test
    void readsSourceFactBytesAndPreservesCompleteMetadata() {
        var writer = new FileSystemAstResultArtifactWriter(tempDir);
        var artifact = writer.write(metadata(), List.of(), List.of(), new ScanSummary(0, 0, 0, 0, 0, "JavaParser", "3.27.1"));

        var bytes = writer.read(request(artifact));

        assertEquals(AnalysisCompleteness.COMPLETE, bytes.artifact().completeness());
        assertEquals("java-parser-analysis-service", bytes.artifact().byteAccess().ownerService());
        assertEquals(Map.of("tenant", "demo"), bytes.safeAttributes());
        assertTrue(new String(bytes.content(), java.nio.charset.StandardCharsets.UTF_8).contains("\"sourceFacts\": []"));
    }

    @Test
    void rejectsUnavailableOversizedMismatchedAndWrongIdentityArtifacts() {
        var writer = new FileSystemAstResultArtifactWriter(tempDir);
        var artifact = writer.write(metadata(), List.of(), List.of(), new ScanSummary(0, 0, 0, 0, 0, "JavaParser", "3.27.1"));

        var unavailable = assertThrows(
            IllegalStateException.class,
            () -> writer.read(request("java-parser-analysis/missing-source-facts.json", "a".repeat(64), 0, 1, "java-ast-analysis-v1"))
        );
        assertEquals("Source fact artifact is not available", unavailable.getMessage());

        var oversized = assertThrows(
            IllegalStateException.class,
            () -> writer.read(request(
                artifact.artifact().path(),
                artifact.artifact().sha256(),
                artifact.artifact().sizeBytes(),
                artifact.artifact().sizeBytes() - 1,
                "java-ast-analysis-v1"
            ))
        );
        assertEquals("Source fact artifact exceeds requested byte limit", oversized.getMessage());

        var sizeMismatch = assertThrows(
            IllegalStateException.class,
            () -> writer.read(request(
                artifact.artifact().path(),
                artifact.artifact().sha256(),
                artifact.artifact().sizeBytes() + 1,
                artifact.artifact().sizeBytes() + 1,
                "java-ast-analysis-v1"
            ))
        );
        assertEquals("Source fact artifact size mismatch", sizeMismatch.getMessage());

        var checksumMismatch = assertThrows(
            IllegalStateException.class,
            () -> writer.read(request(
                artifact.artifact().path(),
                "b".repeat(64),
                artifact.artifact().sizeBytes(),
                artifact.artifact().sizeBytes(),
                "java-ast-analysis-v1"
            ))
        );
        assertEquals("Source fact artifact checksum mismatch", checksumMismatch.getMessage());

        var runIdentityMismatch = assertThrows(
            IllegalStateException.class,
            () -> writer.read(request(
                artifact.artifact().path(),
                artifact.artifact().sha256(),
                artifact.artifact().sizeBytes(),
                artifact.artifact().sizeBytes(),
                "java-ast-analysis-v1",
                new AnalysisRunId("run-2"),
                new AnalysisJobId("job-1"),
                new SourceSnapshotId("snapshot-1")
            ))
        );
        assertEquals("Source fact artifact identity mismatch", runIdentityMismatch.getMessage());

        var jobIdentityMismatch = assertThrows(
            IllegalStateException.class,
            () -> writer.read(request(
                artifact.artifact().path(),
                artifact.artifact().sha256(),
                artifact.artifact().sizeBytes(),
                artifact.artifact().sizeBytes(),
                "java-ast-analysis-v1",
                new AnalysisRunId("run-1"),
                new AnalysisJobId("job-2"),
                new SourceSnapshotId("snapshot-1")
            ))
        );
        assertEquals("Source fact artifact identity mismatch", jobIdentityMismatch.getMessage());

        var snapshotIdentityMismatch = assertThrows(
            IllegalStateException.class,
            () -> writer.read(request(
                artifact.artifact().path(),
                artifact.artifact().sha256(),
                artifact.artifact().sizeBytes(),
                artifact.artifact().sizeBytes(),
                "java-ast-analysis-v1",
                new AnalysisRunId("run-1"),
                new AnalysisJobId("job-1"),
                new SourceSnapshotId("snapshot-2")
            ))
        );
        assertEquals("Source fact artifact identity mismatch", snapshotIdentityMismatch.getMessage());

        var identityMismatch = assertThrows(
            IllegalStateException.class,
            () -> writer.read(request(
                artifact.artifact().path(),
                artifact.artifact().sha256(),
                artifact.artifact().sizeBytes(),
                artifact.artifact().sizeBytes(),
                "java-ast-analysis-v2"
            ))
        );
        assertEquals("Source fact artifact identity mismatch", identityMismatch.getMessage());
    }

    @Test
    void rejectsNonRegularSourceFactArtifactTargetsOnWriteAndRead() throws Exception {
        var writer = new FileSystemAstResultArtifactWriter(tempDir);
        var metadata = metadata();
        var artifact = writer.write(metadata, List.of(), List.of(), new ScanSummary(0, 0, 0, 0, 0, "JavaParser", "3.27.1"));
        var artifactPath = tempDir.resolve(artifact.artifact().path());
        Files.delete(artifactPath);
        Files.createDirectory(artifactPath);

        var readFailure = assertThrows(IllegalStateException.class, () -> writer.read(request(artifact)));
        assertEquals("Source fact artifact is not available", readFailure.getMessage());
        assertThrows(
            UncheckedIOException.class,
            () -> writer.write(metadata, List.of(), List.of(), new ScanSummary(0, 0, 0, 0, 0, "JavaParser", "3.27.1"))
        );
    }

    @Test
    void rejectsSymlinkedArtifactRootBeforeWritingSourceFactBytes() throws Exception {
        var outside = Files.createDirectory(tempDir.resolve("outside-root"));
        var symlinkRoot = tempDir.resolve("artifact-root-link");
        Files.createSymbolicLink(symlinkRoot, outside);
        var writer = new FileSystemAstResultArtifactWriter(symlinkRoot);

        assertThrows(
            UncheckedIOException.class,
            () -> writer.write(metadata(), List.of(), List.of(), new ScanSummary(0, 0, 0, 0, 0, "JavaParser", "3.27.1"))
        );
        try (var files = Files.list(outside)) {
            assertEquals(0, files.count());
        }
    }

    @Test
    void rejectsSymlinkedArtifactRootBeforeReadingSourceFactBytes() throws Exception {
        var realRoot = Files.createDirectory(tempDir.resolve("real-root"));
        var artifact = new FileSystemAstResultArtifactWriter(realRoot)
            .write(metadata(), List.of(), List.of(), new ScanSummary(0, 0, 0, 0, 0, "JavaParser", "3.27.1"));
        var symlinkRoot = tempDir.resolve("read-root-link");
        Files.createSymbolicLink(symlinkRoot, realRoot);
        var reader = new FileSystemAstResultArtifactWriter(symlinkRoot);

        assertThrows(UncheckedIOException.class, () -> reader.read(request(artifact)));
    }

    @Test
    void rejectsSymlinkedArtifactDirectoryOnWriteAndRead() throws Exception {
        var writer = new FileSystemAstResultArtifactWriter(tempDir);
        var outside = Files.createDirectory(tempDir.resolve("outside"));
        Files.createSymbolicLink(tempDir.resolve("java-parser-analysis"), outside);

        assertThrows(
            UncheckedIOException.class,
            () -> writer.write(metadata(), List.of(), List.of(), new ScanSummary(0, 0, 0, 0, 0, "JavaParser", "3.27.1"))
        );
        try (var files = Files.list(outside)) {
            assertEquals(0, files.count());
        }
    }

    @Test
    void rejectsSymlinkedArtifactDirectoryBeforeReadingSourceFactBytes() throws Exception {
        var root = Files.createDirectory(tempDir.resolve("read-segment"));
        var writer = new FileSystemAstResultArtifactWriter(root);
        var artifact = writer.write(metadata(), List.of(), List.of(), new ScanSummary(0, 0, 0, 0, 0, "JavaParser", "3.27.1"));
        var javaAstDirectory = root.resolve("java-parser-analysis");
        var movedDirectory = root.resolve("java-parser-analysis-real");
        Files.move(javaAstDirectory, movedDirectory);
        Files.createSymbolicLink(javaAstDirectory, movedDirectory);

        assertThrows(UncheckedIOException.class, () -> writer.read(request(artifact)));
    }

    @Test
    void rejectsFinalSourceFactSymlinkOnWriteAndRead() throws Exception {
        var writer = new FileSystemAstResultArtifactWriter(tempDir);
        var metadata = metadata();
        var artifact = writer.write(metadata, List.of(), List.of(), new ScanSummary(0, 0, 0, 0, 0, "JavaParser", "3.27.1"));
        var artifactPath = tempDir.resolve(artifact.artifact().path());
        var outside = Files.writeString(tempDir.resolve("outside-source-facts.json"), Files.readString(artifactPath));
        var outsideContent = Files.readString(outside, StandardCharsets.UTF_8);
        Files.delete(artifactPath);
        Files.createSymbolicLink(artifactPath, outside);

        assertThrows(UncheckedIOException.class, () -> writer.read(request(artifact)));
        assertThrows(
            UncheckedIOException.class,
            () -> writer.write(metadata, List.of(), List.of(), new ScanSummary(0, 0, 0, 0, 0, "JavaParser", "3.27.1"))
        );
        assertEquals(outsideContent, Files.readString(outside, StandardCharsets.UTF_8));
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
            "java-parser-analysis-service-test",
            Map.of("tenant", "demo")
        );
    }

    private static Path contractSchema() {
        var current = Path.of("").toAbsolutePath();
        while (current != null) {
            var schema = current.resolve("contracts/grpc/java-ast-source-facts-v1.schema.json");
            if (Files.isRegularFile(schema)) {
                return schema;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Java AST source fact schema contract not found");
    }

    private static SourceFactArtifactBytesRequest request(AnalysisArtifactReference artifact) {
        return request(
            artifact.artifact().path(),
            artifact.artifact().sha256(),
            artifact.artifact().sizeBytes(),
            artifact.artifact().sizeBytes(),
            artifact.schemaVersion()
        );
    }

    private static SourceFactArtifactBytesRequest request(
        String retrievalReference,
        String expectedSha256,
        long expectedSizeBytes,
        long maxBytes,
        String schemaVersion
    ) {
        return request(
            retrievalReference,
            expectedSha256,
            expectedSizeBytes,
            maxBytes,
            schemaVersion,
            new AnalysisRunId("run-1"),
            new AnalysisJobId("job-1"),
            new SourceSnapshotId("snapshot-1")
        );
    }

    private static SourceFactArtifactBytesRequest request(
        String retrievalReference,
        String expectedSha256,
        long expectedSizeBytes,
        long maxBytes,
        String schemaVersion,
        AnalysisRunId analysisRunId,
        AnalysisJobId analysisJobId,
        SourceSnapshotId sourceSnapshotId
    ) {
        return new SourceFactArtifactBytesRequest(
            "request-bytes",
            "correlation-1",
            analysisRunId,
            analysisJobId,
            sourceSnapshotId,
            retrievalReference,
            expectedSha256,
            expectedSizeBytes,
            maxBytes,
            schemaVersion,
            Map.of("tenant", "demo")
        );
    }
}
