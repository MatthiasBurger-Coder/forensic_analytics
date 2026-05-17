package de.burger.forensics.analytics.services.javaastanalysis.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public final class JavaAstAnalysisDomain {
    private static final Pattern WINDOWS_DRIVE_PATH = Pattern.compile("^[A-Za-z]:.*");
    private static final Set<String> SENSITIVE_ATTRIBUTE_NAMES = Set.of(
        "authorization",
        "credential",
        "credentials",
        "password",
        "secret",
        "token"
    );

    private JavaAstAnalysisDomain() {
    }

    public static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    public static String requireRelativePath(String value, String name) {
        var text = requireText(value, name).replace('\\', '/');
        if (text.startsWith("/")
            || text.startsWith("file:")
            || text.contains(":")
            || text.contains("//")
            || WINDOWS_DRIVE_PATH.matcher(text).matches()
            || Arrays.asList(text.split("/")).contains("..")) {
            throw new IllegalArgumentException(name + " must be a safe relative path");
        }
        return text;
    }

    public static Map<String, String> safeAttributes(Map<String, String> attributes) {
        var copy = Map.copyOf(Objects.requireNonNull(attributes, "safe attributes must not be null"));
        copy.forEach(JavaAstAnalysisDomain::requireSafeAttribute);
        return copy;
    }

    public static String sha256(String content) {
        return sha256(content.getBytes(StandardCharsets.UTF_8));
    }

    public static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
    }

    public static String stableId(String... parts) {
        return "java-source-fact:" + sha256(String.join("|", parts));
    }

    private static void requireSafeAttribute(String key, String value) {
        requireText(key, "safe attribute key");
        requireText(value, "safe attribute value");
        var normalizedKey = key.toLowerCase(Locale.ROOT);
        if (SENSITIVE_ATTRIBUTE_NAMES.stream().anyMatch(normalizedKey::contains)
            || value.startsWith("file:")
            || value.contains("://")
            || value.contains("\\")) {
            throw new IllegalArgumentException("safe attributes must not contain secrets or local paths");
        }
    }

    public record RequestMetadata(
        String requestId,
        String idempotencyKey,
        String schemaVersion,
        String correlationId,
        AnalysisRunId analysisRunId,
        AnalysisJobId analysisJobId,
        SourceSnapshotId sourceSnapshotId,
        String workerVersion,
        Map<String, String> safeAttributes
    ) {
        public RequestMetadata {
            requestId = requireText(requestId, "request id");
            idempotencyKey = requireText(idempotencyKey, "idempotency key");
            schemaVersion = requireText(schemaVersion, "schema version");
            correlationId = requireText(correlationId, "correlation id");
            analysisRunId = Objects.requireNonNull(analysisRunId, "analysis run id must not be null");
            analysisJobId = Objects.requireNonNull(analysisJobId, "analysis job id must not be null");
            sourceSnapshotId = Objects.requireNonNull(sourceSnapshotId, "source snapshot id must not be null");
            workerVersion = requireText(workerVersion, "worker version");
            safeAttributes = JavaAstAnalysisDomain.safeAttributes(safeAttributes);
        }
    }

    public record AnalysisRunId(String value) {
        public AnalysisRunId {
            value = requireText(value, "analysis run id");
        }
    }

    public record AnalysisJobId(String value) {
        public AnalysisJobId {
            value = requireText(value, "analysis job id");
        }
    }

    public record SourceSnapshotId(String value) {
        public SourceSnapshotId {
            value = requireText(value, "source snapshot id");
        }
    }

    public record SourceRoot(String relativePath, String language) {
        public SourceRoot {
            relativePath = requireRelativePath(relativePath, "source root");
            language = requireText(language, "source root language").toLowerCase(Locale.ROOT);
        }
    }

    public record JavaSourceFile(
        String sourceRoot,
        String relativePath,
        String contentUtf8,
        String sha256,
        long sizeBytes
    ) {
        public JavaSourceFile {
            sourceRoot = requireRelativePath(sourceRoot, "source root");
            relativePath = requireRelativePath(relativePath, "source file path");
            contentUtf8 = Objects.requireNonNull(contentUtf8, "source content must not be null");
            sha256 = sha256 == null ? "" : sha256;
            if (!sha256.isBlank() && !sha256.matches("[0-9a-fA-F]{64}")) {
                throw new IllegalArgumentException("source file checksum must be a SHA-256 hex value");
            }
            if (sizeBytes < 0) {
                throw new IllegalArgumentException("source file size must not be negative");
            }
        }

        public String sourcePath() {
            return ".".equals(sourceRoot) ? relativePath : sourceRoot + "/" + relativePath;
        }

        public long actualSizeBytes() {
            return contentUtf8.getBytes(StandardCharsets.UTF_8).length;
        }
    }

    public record ScanPolicy(
        int maxFiles,
        long maxSourceBytes,
        long timeoutSeconds,
        boolean emitSymbolResolutionDiagnostics
    ) {
        public ScanPolicy {
            if (maxFiles < 1 || maxFiles > 100_000) {
                throw new IllegalArgumentException("max files must be between 1 and 100000");
            }
            if (maxSourceBytes < 1 || maxSourceBytes > 1_073_741_824L) {
                throw new IllegalArgumentException("max source bytes must be between 1 and 1073741824");
            }
            if (timeoutSeconds < 1 || timeoutSeconds > 86_400) {
                throw new IllegalArgumentException("timeout seconds must be between 1 and 86400");
            }
        }
    }

    public record AnalyzeSourceSnapshotCommand(
        RequestMetadata metadata,
        ScanPolicy scanPolicy,
        List<SourceRoot> sourceRoots,
        List<JavaSourceFile> sourceFiles
    ) {
        public AnalyzeSourceSnapshotCommand {
            metadata = Objects.requireNonNull(metadata, "metadata must not be null");
            scanPolicy = Objects.requireNonNull(scanPolicy, "scan policy must not be null");
            sourceRoots = sortedRoots(sourceRoots);
            sourceFiles = sortedFiles(sourceFiles);
            if (sourceRoots.isEmpty()) {
                throw new IllegalArgumentException("source roots must not be empty");
            }
            if (sourceFiles.isEmpty()) {
                throw new IllegalArgumentException("source files must not be empty");
            }
        }

        private static List<SourceRoot> sortedRoots(List<SourceRoot> sourceRoots) {
            return List.copyOf(Objects.requireNonNull(sourceRoots, "source roots must not be null")).stream()
                .sorted(Comparator.comparing(SourceRoot::relativePath))
                .toList();
        }

        private static List<JavaSourceFile> sortedFiles(List<JavaSourceFile> sourceFiles) {
            return List.copyOf(Objects.requireNonNull(sourceFiles, "source files must not be null")).stream()
                .sorted(Comparator.comparing(JavaSourceFile::sourcePath))
                .toList();
        }
    }

    public record SourceLocation(
        String sourcePath,
        String fullyQualifiedClassName,
        String methodName,
        int lineNumber,
        int columnNumber
    ) {
        public SourceLocation {
            sourcePath = requireRelativePath(sourcePath, "source path");
            fullyQualifiedClassName = requireText(fullyQualifiedClassName, "fully qualified class name");
            methodName = requireText(methodName, "method name");
            if (lineNumber < 1) {
                throw new IllegalArgumentException("line number must be positive");
            }
            if (columnNumber < 1) {
                throw new IllegalArgumentException("column number must be positive");
            }
        }
    }

    public record JavaSourceFact(
        String factId,
        String factType,
        SourceLocation location,
        String signature,
        String summary,
        EvidenceKind evidenceKind
    ) {
        public JavaSourceFact {
            factId = requireText(factId, "fact id");
            factType = requireText(factType, "fact type");
            location = Objects.requireNonNull(location, "source location must not be null");
            signature = requireText(signature, "signature");
            summary = requireText(summary, "summary");
            evidenceKind = Objects.requireNonNull(evidenceKind, "evidence kind must not be null");
            if (evidenceKind != EvidenceKind.STATIC_SOURCE_FACT) {
                throw new IllegalArgumentException("Java AST facts must be static source facts");
            }
        }
    }

    public record JavaAstDiagnostic(
        String code,
        String message,
        DiagnosticSeverity severity,
        SourceSnapshotId sourceSnapshotId,
        String sourcePath,
        int lineNumber,
        int columnNumber,
        boolean retryable,
        boolean affectsCompleteness
    ) {
        public JavaAstDiagnostic {
            code = requireText(code, "diagnostic code");
            message = sanitizeMessage(message);
            severity = Objects.requireNonNull(severity, "diagnostic severity must not be null");
            sourceSnapshotId = Objects.requireNonNull(sourceSnapshotId, "source snapshot id must not be null");
            sourcePath = sourcePath == null || sourcePath.isBlank() ? "" : requireRelativePath(sourcePath, "diagnostic source path");
            if (lineNumber < 0) {
                throw new IllegalArgumentException("diagnostic line number must not be negative");
            }
            if (columnNumber < 0) {
                throw new IllegalArgumentException("diagnostic column number must not be negative");
            }
        }

        public static JavaAstDiagnostic info(SourceSnapshotId sourceSnapshotId, String code, String message) {
            return new JavaAstDiagnostic(code, message, DiagnosticSeverity.INFO, sourceSnapshotId, "", 0, 0, false, false);
        }

        public static JavaAstDiagnostic warning(
            SourceSnapshotId sourceSnapshotId,
            String code,
            String message,
            String sourcePath,
            int lineNumber,
            int columnNumber,
            boolean affectsCompleteness
        ) {
            return new JavaAstDiagnostic(
                code,
                message,
                DiagnosticSeverity.WARNING,
                sourceSnapshotId,
                sourcePath,
                lineNumber,
                columnNumber,
                false,
                affectsCompleteness
            );
        }

        public static JavaAstDiagnostic error(
            SourceSnapshotId sourceSnapshotId,
            String code,
            String message,
            String sourcePath,
            int lineNumber,
            int columnNumber
        ) {
            return new JavaAstDiagnostic(
                code,
                message,
                DiagnosticSeverity.ERROR,
                sourceSnapshotId,
                sourcePath,
                lineNumber,
                columnNumber,
                false,
                true
            );
        }

        private static String sanitizeMessage(String value) {
            var text = requireText(value, "diagnostic message")
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\\', '/');
            if (text.startsWith("file:") || WINDOWS_DRIVE_PATH.matcher(text).matches()) {
                return "diagnostic details redacted";
            }
            return text;
        }
    }

    public record ScanSummary(
        int receivedFileCount,
        int parsedFileCount,
        int skippedFileCount,
        int parseErrorCount,
        int sourceFactCount,
        String parser,
        String parserVersion
    ) {
        public ScanSummary {
            if (receivedFileCount < 0 || parsedFileCount < 0 || skippedFileCount < 0
                || parseErrorCount < 0 || sourceFactCount < 0) {
                throw new IllegalArgumentException("scan counts must not be negative");
            }
            parser = requireText(parser, "parser");
            parserVersion = requireText(parserVersion, "parser version");
        }
    }

    public record JavaAstScanResult(
        List<JavaSourceFact> sourceFacts,
        List<JavaAstDiagnostic> diagnostics,
        ScanSummary summary
    ) {
        public JavaAstScanResult {
            sourceFacts = List.copyOf(Objects.requireNonNull(sourceFacts, "source facts must not be null"));
            diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics must not be null"));
            summary = Objects.requireNonNull(summary, "scan summary must not be null");
        }

        public AnalysisCompleteness completeness() {
            return diagnostics.stream().anyMatch(JavaAstDiagnostic::affectsCompleteness)
                ? AnalysisCompleteness.INCOMPLETE
                : AnalysisCompleteness.COMPLETE;
        }
    }

    public record ArtifactReference(String path, String type, String sha256, long sizeBytes) {
        public ArtifactReference {
            path = requireRelativePath(path, "artifact path");
            type = requireText(type, "artifact type");
            sha256 = requireText(sha256, "artifact checksum");
            if (!sha256.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("artifact checksum must be a SHA-256 hex value");
            }
            if (sizeBytes < 0) {
                throw new IllegalArgumentException("artifact size must not be negative");
            }
        }
    }

    public record AnalysisArtifactReference(
        ArtifactReference artifact,
        AnalysisArtifactCategory category,
        String producerService,
        String schemaVersion,
        AnalysisCompleteness completeness
    ) {
        public AnalysisArtifactReference {
            artifact = Objects.requireNonNull(artifact, "artifact must not be null");
            category = Objects.requireNonNull(category, "artifact category must not be null");
            producerService = requireText(producerService, "producer service");
            schemaVersion = requireText(schemaVersion, "schema version");
            completeness = Objects.requireNonNull(completeness, "completeness must not be null");
        }
    }

    public record AnalyzeSourceSnapshotResult(
        RequestMetadata metadata,
        AnalysisCompleteness completeness,
        AnalysisArtifactReference sourceFactArtifact,
        ScanSummary summary,
        List<JavaAstDiagnostic> diagnostics
    ) {
        public AnalyzeSourceSnapshotResult {
            metadata = Objects.requireNonNull(metadata, "metadata must not be null");
            completeness = Objects.requireNonNull(completeness, "completeness must not be null");
            sourceFactArtifact = Objects.requireNonNull(sourceFactArtifact, "source fact artifact must not be null");
            summary = Objects.requireNonNull(summary, "summary must not be null");
            diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics must not be null"));
        }
    }

    public enum AnalysisCompleteness {
        COMPLETE,
        INCOMPLETE,
        UNKNOWN
    }

    public enum AnalysisArtifactCategory {
        STATIC
    }

    public enum DiagnosticSeverity {
        INFO,
        WARNING,
        ERROR
    }

    public enum EvidenceKind {
        STATIC_SOURCE_FACT
    }
}
