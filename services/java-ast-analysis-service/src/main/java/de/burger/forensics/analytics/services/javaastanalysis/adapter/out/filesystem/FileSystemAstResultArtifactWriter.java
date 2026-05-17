package de.burger.forensics.analytics.services.javaastanalysis.adapter.out.filesystem;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.burger.forensics.analytics.services.javaastanalysis.application.port.AstResultArtifactWriterPort;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalysisArtifactCategory;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.ArtifactReference;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.JavaAstDiagnostic;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.JavaSourceFact;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.RequestMetadata;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.ScanSummary;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.sha256;

public final class FileSystemAstResultArtifactWriter implements AstResultArtifactWriterPort {
    private static final String PRODUCER_SERVICE = "java-ast-analysis-service";
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
        return new AnalysisArtifactReference(
            new ArtifactReference(relativePath.toString().replace('\\', '/'), ARTIFACT_TYPE, sha256(bytes), bytes.length),
            AnalysisArtifactCategory.STATIC,
            PRODUCER_SERVICE,
            metadata.schemaVersion(),
            diagnostics.stream().anyMatch(JavaAstDiagnostic::affectsCompleteness)
                ? de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalysisCompleteness.INCOMPLETE
                : de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalysisCompleteness.COMPLETE
        );
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
