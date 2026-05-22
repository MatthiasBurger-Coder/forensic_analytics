package de.burger.forensics.analytics.services.javaparseranalysis.adapter.out.filesystem;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.burger.forensics.analytics.services.javaparseranalysis.application.port.AstResultArtifactReaderPort;
import de.burger.forensics.analytics.services.javaparseranalysis.application.port.AstResultArtifactWriterPort;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.AnalysisArtifactCategory;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.ArtifactByteAccess;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.ArtifactByteCustody;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.ArtifactReference;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.JavaAstDiagnostic;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.JavaSourceFact;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.RequestMetadata;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.ScanSummary;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.SourceFactArtifactBytes;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.SourceFactArtifactBytesRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.sha256;

public final class FileSystemAstResultArtifactWriter implements AstResultArtifactWriterPort, AstResultArtifactReaderPort {
    private static final String PRODUCER_SERVICE = "java-parser-analysis-service";
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
        var document = AstArtifactDocument.from(metadata, sourceFacts, diagnostics, summary);
        var content = GSON.toJson(document);
        var bytes = content.getBytes(StandardCharsets.UTF_8);
        var relativePath = artifactPath(metadata);
        var target = resolve(relativePath.toString());
        try {
            createArtifactDirectories(relativePath.getParent());
            writeBytes(target, bytes);
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
                ? de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.AnalysisCompleteness.INCOMPLETE
                : de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.AnalysisCompleteness.COMPLETE,
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
        var relativePath = relativePath(request.retrievalReference(), "retrieval reference");
        var target = resolve(request.retrievalReference());
        try {
            rejectSymlinkSegments(relativePath);
            var attributes = regularFileAttributes(target);
            if (attributes == null) {
                throw new IllegalStateException("Source fact artifact is not available");
            }
            var size = attributes.size();
            if (size > request.maxBytes()) {
                throw new IllegalStateException("Source fact artifact exceeds requested byte limit");
            }
            if (size != request.expectedSizeBytes()) {
                throw new IllegalStateException("Source fact artifact size mismatch");
            }
            var bytes = readBytes(target);
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
                    document.diagnostics().stream().anyMatch(DiagnosticDocument::affectsCompleteness)
                        ? de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.AnalysisCompleteness.INCOMPLETE
                        : de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.AnalysisCompleteness.COMPLETE,
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
            "java-parser-analysis",
            safeName(metadata.sourceSnapshotId().value()) + "-" + safeName(metadata.analysisJobId().value()) + "-source-facts.json"
        );
    }

    private Path resolve(String artifactPath) {
        var relativePath = relativePath(artifactPath, "retrieval reference");
        var target = artifactRoot.resolve(relativePath).normalize();
        if (!target.startsWith(artifactRoot)) {
            throw new IllegalArgumentException("retrieval reference must stay inside Java AST artifact storage");
        }
        return target;
    }

    private void createArtifactDirectories(Path relativeDirectory) throws IOException {
        ensureRootDirectory();
        if (relativeDirectory == null) {
            return;
        }
        var current = artifactRoot;
        for (var segment : relativeDirectory) {
            current = current.resolve(segment);
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                ensureDirectory(current);
            } else {
                Files.createDirectory(current);
            }
        }
    }

    private void ensureRootDirectory() throws IOException {
        if (!Files.exists(artifactRoot, LinkOption.NOFOLLOW_LINKS)) {
            Files.createDirectories(artifactRoot);
        }
        ensureDirectory(artifactRoot);
    }

    private static void ensureDirectory(Path path) throws IOException {
        if (Files.isSymbolicLink(path) || !Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Artifact directory must not be a symbolic link or non-directory.");
        }
    }

    private static BasicFileAttributes regularFileAttributes(Path target) throws IOException {
        if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            return null;
        }
        rejectSymlink(target);
        var attributes = Files.readAttributes(target, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        return attributes.isRegularFile() ? attributes : null;
    }

    private static void writeBytes(Path target, byte[] bytes) throws IOException {
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            rejectSymlink(target);
            var attributes = Files.readAttributes(target, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!attributes.isRegularFile()) {
                throw new IOException("Artifact target must be a regular file.");
            }
            try (var channel = Files.newByteChannel(
                target,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING,
                LinkOption.NOFOLLOW_LINKS
            )) {
                channel.write(ByteBuffer.wrap(bytes));
            }
            return;
        }
        try (var channel = Files.newByteChannel(
            target,
            StandardOpenOption.WRITE,
            StandardOpenOption.CREATE_NEW,
            LinkOption.NOFOLLOW_LINKS
        )) {
            channel.write(ByteBuffer.wrap(bytes));
        }
    }

    private static byte[] readBytes(Path target) throws IOException {
        try (var channel = Files.newByteChannel(target, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS);
             var output = new ByteArrayOutputStream()) {
            var buffer = ByteBuffer.allocate(8192);
            while (channel.read(buffer) != -1) {
                buffer.flip();
                output.write(buffer.array(), 0, buffer.limit());
                buffer.clear();
            }
            return output.toByteArray();
        }
    }

    private static void rejectSymlink(Path path) throws IOException {
        if (Files.isSymbolicLink(path)) {
            throw new IOException("Artifact path must not contain symbolic links.");
        }
    }

    private void rejectSymlinkSegments(Path relativePath) throws IOException {
        rejectSymlink(artifactRoot);
        var probe = artifactRoot;
        for (var segment : relativePath) {
            probe = probe.resolve(segment);
            rejectSymlink(probe);
        }
    }

    private static Path relativePath(String value, String fieldName) {
        var reference = Objects.requireNonNull(value, fieldName + " must not be null").replace('\\', '/');
        var lower = reference.toLowerCase(Locale.ROOT);
        if (reference.isBlank()
            || reference.startsWith("/")
            || lower.startsWith("file:")
            || reference.contains("://")
            || reference.matches("^[A-Za-z]:.*")) {
            throw new IllegalArgumentException(fieldName + " must be a safe relative path");
        }
        if (Arrays.stream(reference.split("/")).anyMatch(part -> part.isBlank() || part.equals(".") || part.equals(".."))) {
            throw new IllegalArgumentException(fieldName + " must not contain traversal, current-directory or blank path segments");
        }
        return Path.of(reference).normalize();
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
        List<SourceFactDocument> sourceFacts,
        List<DiagnosticDocument> diagnostics
    ) {
        static AstArtifactDocument from(
            RequestMetadata metadata,
            List<JavaSourceFact> sourceFacts,
            List<JavaAstDiagnostic> diagnostics,
            ScanSummary summary
        ) {
            return new AstArtifactDocument(
                metadata.schemaVersion(),
                metadata.analysisRunId().value(),
                metadata.analysisJobId().value(),
                metadata.sourceSnapshotId().value(),
                summary,
                sourceFacts.stream().map(SourceFactDocument::from).toList(),
                diagnostics.stream().map(DiagnosticDocument::from).toList()
            );
        }
    }

    private record SourceFactDocument(
        String factId,
        String factType,
        String sourceRoot,
        SourceLocationDocument location,
        String signature,
        String summary,
        String evidenceKind
    ) {
        static SourceFactDocument from(JavaSourceFact fact) {
            return new SourceFactDocument(
                fact.factId(),
                fact.factType(),
                fact.sourceRoot(),
                SourceLocationDocument.from(fact.location()),
                fact.signature(),
                fact.summary(),
                fact.evidenceKind().name()
            );
        }
    }

    private record SourceLocationDocument(
        String sourcePath,
        String fullyQualifiedClassName,
        String methodName,
        int lineNumber,
        int columnNumber
    ) {
        static SourceLocationDocument from(de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.SourceLocation location) {
            return new SourceLocationDocument(
                location.sourcePath(),
                location.fullyQualifiedClassName(),
                location.methodName(),
                location.lineNumber(),
                location.columnNumber()
            );
        }
    }

    private record DiagnosticDocument(
        String code,
        String message,
        String severity,
        String sourceSnapshotId,
        String sourcePath,
        int lineNumber,
        int columnNumber,
        boolean retryable,
        boolean affectsCompleteness
    ) {
        static DiagnosticDocument from(JavaAstDiagnostic diagnostic) {
            return new DiagnosticDocument(
                diagnostic.code(),
                diagnostic.message(),
                diagnostic.severity().name(),
                diagnostic.sourceSnapshotId().value(),
                diagnostic.sourcePath(),
                diagnostic.lineNumber(),
                diagnostic.columnNumber(),
                diagnostic.retryable(),
                diagnostic.affectsCompleteness()
            );
        }
    }
}
