package de.burger.forensics.analytics.services.javaastanalysis.adapter.out.filesystem;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.burger.forensics.analytics.services.javaastanalysis.application.port.AstResultArtifactReaderPort;
import de.burger.forensics.analytics.services.javaastanalysis.application.port.AstResultArtifactWriterPort;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalysisArtifactCategory;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.ArtifactByteAccess;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.ArtifactByteCustody;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.ArtifactReference;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.JavaAstDiagnostic;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.JavaSourceFact;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.RequestMetadata;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.ScanSummary;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.SourceFactArtifactBytes;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.SourceFactArtifactBytesRequest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.sha256;

public final class FileSystemAstResultArtifactWriter implements AstResultArtifactWriterPort, AstResultArtifactReaderPort {
    private static final String PRODUCER_SERVICE = "java-ast-analysis-service";
    public static final String BYTE_RETRIEVAL_CONTRACT =
        "java-ast-analysis.v1.JavaAstAnalysisService.GetSourceFactArtifactBytes";
    private static final String ARTIFACT_TYPE = "application/vnd.forensic-analytics.java-ast-source-facts.v1+json";
    private static final Gson GSON = new GsonBuilder()
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .create();

    private final Path artifactRoot;

    public FileSystemAstResultArtifactWriter(Path artifactRoot) {
        this.artifactRoot = Objects.requireNonNull(artifactRoot, "artifact root must not be null")
            .toAbsolutePath()
            .normalize();
    }

    @Override
    public AnalysisArtifactReference write(
        RequestMetadata metadata,
        List<JavaSourceFact> sourceFacts,
        List<JavaAstDiagnostic> diagnostics,
        ScanSummary summary
    ) {
        var document = new AstArtifactDocument(
            metadata.schemaVersion(),
            metadata.analysisRunId().value(),
            metadata.analysisJobId().value(),
            metadata.sourceSnapshotId().value(),
            summary,
            List.copyOf(sourceFacts),
            List.copyOf(diagnostics)
        );
        var content = GSON.toJson(document);
        var bytes = content.getBytes(StandardCharsets.UTF_8);
        var relativePath = artifactPath(metadata);
        var target = artifactRoot.resolve(relativePath).normalize();
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
        } catch (IOException error) {
            throw new UncheckedIOException("Failed to write Java AST source fact artifact.", error);
        }
        var publicArtifactPath = relativePath.toString().replace('\\', '/');
        return new AnalysisArtifactReference(
            new ArtifactReference(publicArtifactPath, ARTIFACT_TYPE, sha256(bytes), bytes.length),
            AnalysisArtifactCategory.STATIC,
            PRODUCER_SERVICE,
            metadata.schemaVersion(),
            diagnostics.stream().anyMatch(JavaAstDiagnostic::affectsCompleteness)
                ? de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalysisCompleteness.INCOMPLETE
                : de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalysisCompleteness.COMPLETE,
            new ArtifactByteAccess(
                PRODUCER_SERVICE,
                BYTE_RETRIEVAL_CONTRACT,
                publicArtifactPath,
                ArtifactByteCustody.PRODUCER_RETAINED
            )
        );
    }

    @Override
    public SourceFactArtifactBytes read(SourceFactArtifactBytesRequest request) {
        var relativePath = Path.of(request.retrievalReference());
        var target = artifactRoot.resolve(relativePath).normalize();
        if (!target.startsWith(artifactRoot)) {
            throw new IllegalArgumentException("retrieval reference must stay inside Java AST artifact storage");
        }
        try {
            if (!Files.isRegularFile(target)) {
                throw new IllegalStateException("Source fact artifact is not available");
            }
            var size = Files.size(target);
            if (size > request.maxBytes()) {
                throw new IllegalStateException("Source fact artifact exceeds requested byte limit");
            }
            if (size != request.expectedSizeBytes()) {
                throw new IllegalStateException("Source fact artifact size mismatch");
            }
            var bytes = Files.readAllBytes(target);
            var checksum = sha256(bytes);
            if (!checksum.equals(request.expectedSha256())) {
                throw new IllegalStateException("Source fact artifact checksum mismatch");
            }
            var document = GSON.fromJson(new String(bytes, StandardCharsets.UTF_8), AstArtifactDocument.class);
            requireArtifactIdentity(request, document);
            return new SourceFactArtifactBytes(
                new AnalysisArtifactReference(
                    new ArtifactReference(request.retrievalReference(), ARTIFACT_TYPE, checksum, bytes.length),
                    AnalysisArtifactCategory.STATIC,
                    PRODUCER_SERVICE,
                    document.schemaVersion(),
                    document.diagnostics().stream().anyMatch(JavaAstDiagnostic::affectsCompleteness)
                        ? de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalysisCompleteness.INCOMPLETE
                        : de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalysisCompleteness.COMPLETE,
                    new ArtifactByteAccess(
                        PRODUCER_SERVICE,
                        BYTE_RETRIEVAL_CONTRACT,
                        request.retrievalReference(),
                        ArtifactByteCustody.PRODUCER_RETAINED
                    )
                ),
                bytes,
                request.safeAttributes()
            );
        } catch (IOException error) {
            throw new UncheckedIOException("Failed to read Java AST source fact artifact.", error);
        }
    }

    private static void requireArtifactIdentity(SourceFactArtifactBytesRequest request, AstArtifactDocument document) {
        if (document == null
            || !document.analysisRunId().equals(request.analysisRunId().value())
            || !document.analysisJobId().equals(request.analysisJobId().value())
            || !document.sourceSnapshotId().equals(request.sourceSnapshotId().value())
            || !document.schemaVersion().equals(request.schemaVersion())) {
            throw new IllegalStateException("Source fact artifact identity mismatch");
        }
    }

    private static Path artifactPath(RequestMetadata metadata) {
        return Path.of(
            "java-ast",
            safeName(metadata.sourceSnapshotId().value()) + "-" + safeName(metadata.analysisJobId().value()) + "-source-facts.json"
        );
    }

    private static String safeName(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private record AstArtifactDocument(
        String schemaVersion,
        String analysisRunId,
        String analysisJobId,
        String sourceSnapshotId,
        ScanSummary summary,
        List<JavaSourceFact> sourceFacts,
        List<JavaAstDiagnostic> diagnostics
    ) {
    }
}
