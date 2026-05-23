package de.burger.forensics.analytics.services.joernanalysis.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public final class JoernCpgAnalysisDomain {
    public static final String PRODUCER_SERVICE = "joern-analysis-service";
    public static final String SEMANTIC_ARTIFACT_SCHEMA_VERSION = "joern-cpg-semantic-artifacts-v1";
    private static final Pattern WINDOWS_DRIVE_PATH = Pattern.compile("^[A-Za-z]:[\\\\/].*");
    private static final Pattern SHA256 = Pattern.compile("^[0-9a-fA-F]{64}$");
    private static final Pattern SHA256_IMAGE = Pattern.compile(".+@sha256:[0-9a-fA-F]{64}");
    private static final List<String> SENSITIVE_KEYS = List.of(
        "secret",
        "password",
        "credential",
        "token",
        "apikey",
        "api_key"
    );

    private JoernCpgAnalysisDomain() {
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
            safeAttributes = safeAttributeMap(safeAttributes);
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
            value = requireIdentifier(value, "source snapshot id");
        }
    }

    public record SourceRoot(String relativePath, String language) {
        public SourceRoot {
            relativePath = requireRelativePath(relativePath, "source root");
            language = requireText(language, "source root language").toLowerCase(Locale.ROOT);
        }
    }

    public record SourceWorkspace(
        String workspaceId,
        List<SourceRoot> sourceRoots,
        List<AnalysisArtifactReference> inputArtifacts
    ) {
        public SourceWorkspace {
            workspaceId = requireJoernWorkspaceId(workspaceId);
            sourceRoots = List.copyOf(Objects.requireNonNull(sourceRoots, "source roots must not be null")).stream()
                .sorted(Comparator.comparing(SourceRoot::relativePath))
                .toList();
            inputArtifacts = List.copyOf(Objects.requireNonNull(inputArtifacts, "input artifacts must not be null")).stream()
                .sorted(Comparator.comparing(reference -> reference.artifact().path()))
                .toList();
            if (sourceRoots.isEmpty()) {
                throw new IllegalArgumentException("source roots must not be empty");
            }
            if (inputArtifacts.isEmpty()) {
                throw new IllegalArgumentException("source workspace input artifacts must not be empty");
            }
        }
    }

    public record JoernCpgPolicy(
        int maxSourceRoots,
        long maxWorkspaceBytes,
        long maxArtifactBytes,
        long timeoutSeconds,
        String joernImageReference,
        String queryBundleVersion,
        boolean requireCallgraph,
        boolean requireControlflow,
        boolean requireDataflow
    ) {
        public JoernCpgPolicy {
            if (maxSourceRoots < 1) {
                throw new IllegalArgumentException("max source roots must be positive");
            }
            if (maxWorkspaceBytes < 1) {
                throw new IllegalArgumentException("max workspace bytes must be positive");
            }
            if (maxArtifactBytes < 1) {
                throw new IllegalArgumentException("max artifact bytes must be positive");
            }
            if (timeoutSeconds < 1 || timeoutSeconds > 86_400) {
                throw new IllegalArgumentException("timeout seconds must be between 1 and 86400");
            }
            joernImageReference = requireSha256ImageReference(joernImageReference, "Joern image reference");
            queryBundleVersion = requireText(queryBundleVersion, "query bundle version");
        }
    }

    public record JoernMaterializationPolicy(
        int maxSourceRoots,
        long maxWorkspaceBytes,
        long maxArtifactBytes,
        long maxArchiveDepth,
        boolean rejectSymlinks,
        boolean rejectHardlinks,
        boolean rejectDeviceFiles,
        boolean rejectDuplicatePaths
    ) {
        public JoernMaterializationPolicy {
            if (maxSourceRoots < 1) {
                throw new IllegalArgumentException("max source roots must be positive");
            }
            if (maxWorkspaceBytes < 1) {
                throw new IllegalArgumentException("max workspace bytes must be positive");
            }
            if (maxArtifactBytes < 1) {
                throw new IllegalArgumentException("max artifact bytes must be positive");
            }
            if (maxArchiveDepth < 1) {
                throw new IllegalArgumentException("max archive depth must be positive");
            }
            if (!rejectSymlinks || !rejectHardlinks || !rejectDeviceFiles || !rejectDuplicatePaths) {
                throw new IllegalArgumentException("Joern materialization must reject unsafe archive entries");
            }
        }
    }

    public record MaterializationMetadata(
        String requestId,
        String idempotencyKey,
        String schemaVersion,
        String correlationId,
        AnalysisRunId analysisRunId,
        AnalysisJobId analysisJobId,
        SourceSnapshotId sourceSnapshotId,
        Map<String, String> safeAttributes
    ) {
        public MaterializationMetadata {
            requestId = requireText(requestId, "request id");
            idempotencyKey = requireText(idempotencyKey, "idempotency key");
            schemaVersion = requireText(schemaVersion, "schema version");
            correlationId = requireText(correlationId, "correlation id");
            analysisRunId = Objects.requireNonNull(analysisRunId, "analysis run id must not be null");
            analysisJobId = Objects.requireNonNull(analysisJobId, "analysis job id must not be null");
            sourceSnapshotId = Objects.requireNonNull(sourceSnapshotId, "source snapshot id must not be null");
            safeAttributes = safeAttributeMap(safeAttributes);
        }
    }

    public record MaterializedPackageDescriptor(
        String packageName,
        PackageAvailability availability,
        ArtifactReference manifestArtifact,
        ArtifactReference packageArtifact,
        String producerService,
        String schemaVersion,
        AnalysisCompleteness completeness,
        ArtifactByteAccess byteAccess
    ) {
        public MaterializedPackageDescriptor {
            packageName = requireText(packageName, "package name");
            availability = Objects.requireNonNull(availability, "package availability must not be null");
            manifestArtifact = Objects.requireNonNull(manifestArtifact, "manifest artifact must not be null");
            packageArtifact = Objects.requireNonNull(packageArtifact, "package artifact must not be null");
            producerService = requireText(producerService, "producer service");
            schemaVersion = requireText(schemaVersion, "schema version");
            completeness = Objects.requireNonNull(completeness, "package completeness must not be null");
            byteAccess = Objects.requireNonNull(byteAccess, "artifact byte access must not be null");
            if (availability != PackageAvailability.AVAILABLE) {
                throw new IllegalArgumentException(packageName + " must be available before Joern materialization");
            }
            if (completeness != AnalysisCompleteness.COMPLETE) {
                throw new IllegalArgumentException(packageName + " must be complete before Joern materialization");
            }
            if (manifestArtifact.sizeBytes() < 1) {
                throw new IllegalArgumentException(packageName + " manifest artifact must contain manifest bytes");
            }
            if (packageArtifact.sizeBytes() < 1) {
                throw new IllegalArgumentException(packageName + " package artifact must contain package bytes");
            }
        }
    }

    public record MaterializeJoernWorkspaceCommand(
        MaterializationMetadata metadata,
        List<SourceRoot> sourceRoots,
        MaterializedPackageDescriptor sourcePackage,
        MaterializedPackageDescriptor buildOutputPackage,
        JoernMaterializationPolicy policy
    ) {
        public MaterializeJoernWorkspaceCommand {
            metadata = Objects.requireNonNull(metadata, "metadata must not be null");
            sourceRoots = List.copyOf(Objects.requireNonNull(sourceRoots, "source roots must not be null")).stream()
                .sorted(Comparator.comparing(SourceRoot::relativePath))
                .toList();
            if (sourceRoots.isEmpty()) {
                throw new IllegalArgumentException("source roots must not be empty");
            }
            sourcePackage = Objects.requireNonNull(sourcePackage, "source package must not be null");
            buildOutputPackage = Objects.requireNonNull(buildOutputPackage, "build-output package must not be null");
            policy = Objects.requireNonNull(policy, "materialization policy must not be null");
            if (sourceRoots.size() > policy.maxSourceRoots()) {
                throw new IllegalArgumentException("source root count exceeds materialization policy");
            }
            if (sourcePackage.packageArtifact().sizeBytes() > policy.maxArtifactBytes()
                || sourcePackage.manifestArtifact().sizeBytes() > policy.maxArtifactBytes()
                || buildOutputPackage.packageArtifact().sizeBytes() > policy.maxArtifactBytes()
                || buildOutputPackage.manifestArtifact().sizeBytes() > policy.maxArtifactBytes()) {
                throw new IllegalArgumentException("package artifact size exceeds materialization policy");
            }
        }
    }

    public record AnalyzeJoernCpgCommand(
        RequestMetadata metadata,
        JoernCpgPolicy policy,
        SourceWorkspace workspace
    ) {
        public AnalyzeJoernCpgCommand {
            metadata = Objects.requireNonNull(metadata, "metadata must not be null");
            policy = Objects.requireNonNull(policy, "policy must not be null");
            workspace = Objects.requireNonNull(workspace, "workspace must not be null");
            if (!workspace.workspaceId().equals("joern-workspace-" + metadata.sourceSnapshotId().value())) {
                throw new IllegalArgumentException("workspace id must match the requested source snapshot");
            }
            if (workspace.sourceRoots().size() > policy.maxSourceRoots()) {
                throw new IllegalArgumentException("source root count exceeds scan policy");
            }
        }
    }

    public record ArtifactReference(String path, String type, String sha256, long sizeBytes) {
        public ArtifactReference {
            path = requireRelativePath(path, "artifact path");
            type = requireText(type, "artifact type");
            sha256 = requireSha256(sha256, "artifact sha256");
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
        AnalysisCompleteness completeness,
        ArtifactByteAccess byteAccess
    ) {
        public AnalysisArtifactReference {
            artifact = Objects.requireNonNull(artifact, "artifact must not be null");
            category = Objects.requireNonNull(category, "artifact category must not be null");
            producerService = requireText(producerService, "producer service");
            schemaVersion = requireText(schemaVersion, "schema version");
            completeness = Objects.requireNonNull(completeness, "completeness must not be null");
            byteAccess = Objects.requireNonNull(byteAccess, "artifact byte access must not be null");
            if (category != AnalysisArtifactCategory.STATIC) {
                throw new IllegalArgumentException("Joern CPG artifacts must be static semantic artifacts");
            }
        }
    }

    public record ArtifactByteAccess(
        String ownerService,
        String retrievalContract,
        String retrievalReference,
        ArtifactByteCustody byteCustody
    ) {
        public ArtifactByteAccess {
            ownerService = requireText(ownerService, "artifact byte owner service");
            retrievalContract = requireText(retrievalContract, "artifact byte retrieval contract");
            retrievalReference = requirePublicReference(retrievalReference, "artifact byte retrieval reference");
            byteCustody = Objects.requireNonNull(byteCustody, "artifact byte custody must not be null");
        }
    }

    public record JoernCpgDiagnostic(
        String code,
        String message,
        DiagnosticSeverity severity,
        SourceSnapshotId sourceSnapshotId,
        String artifactPath,
        boolean retryable,
        boolean affectsCompleteness
    ) {
        public JoernCpgDiagnostic {
            code = requireText(code, "diagnostic code");
            message = sanitizeMessage(message);
            severity = Objects.requireNonNull(severity, "diagnostic severity must not be null");
            sourceSnapshotId = Objects.requireNonNull(sourceSnapshotId, "source snapshot id must not be null");
            artifactPath = artifactPath == null || artifactPath.isBlank() ? "" : requireRelativePath(artifactPath, "artifact path");
        }

        public static JoernCpgDiagnostic info(SourceSnapshotId sourceSnapshotId, String code, String message) {
            return new JoernCpgDiagnostic(code, message, DiagnosticSeverity.INFO, sourceSnapshotId, "", false, false);
        }

        public static JoernCpgDiagnostic warning(
            SourceSnapshotId sourceSnapshotId,
            String code,
            String message,
            String artifactPath,
            boolean affectsCompleteness
        ) {
            return new JoernCpgDiagnostic(
                code,
                message,
                DiagnosticSeverity.WARNING,
                sourceSnapshotId,
                artifactPath,
                false,
                affectsCompleteness
            );
        }

        public static JoernCpgDiagnostic error(
            SourceSnapshotId sourceSnapshotId,
            String code,
            String message,
            boolean retryable
        ) {
            return new JoernCpgDiagnostic(
                code,
                message,
                DiagnosticSeverity.ERROR,
                sourceSnapshotId,
                "",
                retryable,
                true
            );
        }
    }

    public record JoernCpgSummary(
        int sourceRootCount,
        int producedArtifactCount,
        int missingArtifactCount,
        String joernVersion,
        String joernImageReference,
        String queryBundleVersion,
        String producerService,
        String schemaVersion
    ) {
        public JoernCpgSummary {
            if (sourceRootCount < 0 || producedArtifactCount < 0 || missingArtifactCount < 0) {
                throw new IllegalArgumentException("summary counts must not be negative");
            }
            joernVersion = requireText(joernVersion, "Joern version");
            joernImageReference = requireSha256ImageReference(joernImageReference, "Joern image reference");
            queryBundleVersion = requireText(queryBundleVersion, "query bundle version");
            producerService = requireText(producerService, "producer service");
            schemaVersion = requireText(schemaVersion, "schema version");
        }
    }

    public record JoernRuntimeResult(
        String joernVersion,
        String joernImageReference,
        String artifactDirectory,
        List<JoernCpgDiagnostic> diagnostics
    ) {
        public JoernRuntimeResult {
            joernVersion = requireText(joernVersion, "Joern version");
            joernImageReference = requireSha256ImageReference(joernImageReference, "Joern image reference");
            artifactDirectory = requireRelativePath(artifactDirectory, "artifact directory");
            diagnostics = sortedDiagnostics(diagnostics);
        }
    }

    public record JoernArtifactCollectionResult(
        List<AnalysisArtifactReference> artifacts,
        int missingArtifactCount,
        List<JoernCpgDiagnostic> diagnostics
    ) {
        public JoernArtifactCollectionResult {
            artifacts = List.copyOf(Objects.requireNonNull(artifacts, "artifacts must not be null")).stream()
                .sorted(Comparator.comparing(reference -> reference.artifact().path()))
                .toList();
            if (missingArtifactCount < 0) {
                throw new IllegalArgumentException("missing artifact count must not be negative");
            }
            diagnostics = sortedDiagnostics(diagnostics);
        }
    }

    public record AnalyzeJoernCpgResult(
        RequestMetadata metadata,
        AnalysisCompleteness completeness,
        List<AnalysisArtifactReference> semanticArtifacts,
        JoernCpgSummary summary,
        List<JoernCpgDiagnostic> diagnostics
    ) {
        public AnalyzeJoernCpgResult {
            metadata = Objects.requireNonNull(metadata, "metadata must not be null");
            completeness = Objects.requireNonNull(completeness, "completeness must not be null");
            semanticArtifacts = List.copyOf(Objects.requireNonNull(semanticArtifacts, "semantic artifacts must not be null")).stream()
                .sorted(Comparator.comparing(reference -> reference.artifact().path()))
                .toList();
            summary = Objects.requireNonNull(summary, "summary must not be null");
            diagnostics = sortedDiagnostics(diagnostics);
        }
    }

    public record SemanticArtifactBytesRequest(
        String requestId,
        String correlationId,
        AnalysisRunId analysisRunId,
        AnalysisJobId analysisJobId,
        SourceSnapshotId sourceSnapshotId,
        String retrievalReference,
        String expectedSha256,
        long expectedSizeBytes,
        long maxBytes,
        String schemaVersion,
        Map<String, String> safeAttributes
    ) {
        public SemanticArtifactBytesRequest {
            requestId = requireText(requestId, "request id");
            correlationId = requireText(correlationId, "correlation id");
            analysisRunId = Objects.requireNonNull(analysisRunId, "analysis run id must not be null");
            analysisJobId = Objects.requireNonNull(analysisJobId, "analysis job id must not be null");
            sourceSnapshotId = Objects.requireNonNull(sourceSnapshotId, "source snapshot id must not be null");
            retrievalReference = requirePublicReference(retrievalReference, "retrieval reference");
            expectedSha256 = requireSha256(expectedSha256, "expected checksum");
            if (expectedSizeBytes < 0) {
                throw new IllegalArgumentException("expected size must not be negative");
            }
            if (maxBytes < 1 || maxBytes > 1_073_741_824L) {
                throw new IllegalArgumentException("max bytes must be between 1 and 1073741824");
            }
            schemaVersion = requireText(schemaVersion, "schema version");
            safeAttributes = safeAttributeMap(safeAttributes);
        }
    }

    public record SemanticArtifactBytes(
        AnalysisArtifactReference artifact,
        byte[] content,
        Map<String, String> safeAttributes
    ) {
        public SemanticArtifactBytes {
            artifact = Objects.requireNonNull(artifact, "artifact must not be null");
            content = Objects.requireNonNull(content, "content must not be null").clone();
            safeAttributes = safeAttributeMap(safeAttributes);
        }

        @Override
        public byte[] content() {
            return content.clone();
        }
    }

    public record MaterializeJoernWorkspaceResult(
        MaterializationMetadata metadata,
        SourceWorkspace workspace,
        List<JoernCpgDiagnostic> diagnostics
    ) {
        public MaterializeJoernWorkspaceResult {
            metadata = Objects.requireNonNull(metadata, "metadata must not be null");
            workspace = Objects.requireNonNull(workspace, "workspace must not be null");
            diagnostics = sortedDiagnostics(diagnostics);
        }
    }

    public enum AnalysisArtifactCategory {
        STATIC
    }

    public enum AnalysisCompleteness {
        COMPLETE,
        INCOMPLETE,
        UNKNOWN
    }

    public enum ArtifactByteCustody {
        PRODUCER_RETAINED,
        SCOPED_OBJECT_ACCESS,
        EXPLICIT_HANDOFF
    }

    public enum PackageAvailability {
        AVAILABLE,
        PENDING,
        UNAVAILABLE,
        FAILED_INTEGRITY
    }

    public enum DiagnosticSeverity {
        INFO,
        WARNING,
        ERROR
    }

    public static String requireRelativePath(String value, String fieldName) {
        var text = requireText(value, fieldName).replace('\\', '/');
        var lower = text.toLowerCase(Locale.ROOT);
        if (text.startsWith("/")
            || lower.startsWith("file:")
            || WINDOWS_DRIVE_PATH.matcher(text).matches()
            || text.contains("://")) {
            throw new IllegalArgumentException(fieldName + " must be a relative path");
        }
        var parts = List.of(text.split("/"));
        if (parts.stream().anyMatch(part -> part.equals(".") || part.equals("..") || part.isBlank())) {
            throw new IllegalArgumentException(fieldName + " must not contain traversal, current-directory or blank segments");
        }
        return String.join("/", parts);
    }

    public static String sha256(String value) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    public static String requireSha256(String value, String fieldName) {
        var text = requireText(value, fieldName);
        if (!SHA256.matcher(text).matches()) {
            throw new IllegalArgumentException(fieldName + " must be a SHA-256 hex digest");
        }
        return text.toLowerCase(Locale.ROOT);
    }

    public static String requireSha256ImageReference(String value, String fieldName) {
        var text = requireText(value, fieldName);
        if (!SHA256_IMAGE.matcher(text).matches()) {
            throw new IllegalArgumentException(fieldName + " must be sha256-pinned");
        }
        return text;
    }

    public static AnalysisCompleteness completeness(List<JoernCpgDiagnostic> diagnostics) {
        return diagnostics.stream().anyMatch(JoernCpgDiagnostic::affectsCompleteness)
            ? AnalysisCompleteness.INCOMPLETE
            : AnalysisCompleteness.COMPLETE;
    }

    public static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.strip();
    }

    private static List<JoernCpgDiagnostic> sortedDiagnostics(List<JoernCpgDiagnostic> diagnostics) {
        return List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics must not be null")).stream()
            .sorted(Comparator.comparing(JoernCpgDiagnostic::code)
                .thenComparing(JoernCpgDiagnostic::artifactPath)
                .thenComparing(JoernCpgDiagnostic::message))
            .toList();
    }

    private static String requireIdentifier(String value, String fieldName) {
        var text = requireText(value, fieldName);
        if (text.contains("/") || text.contains("\\") || text.contains(":") || text.equals(".") || text.equals("..")) {
            throw new IllegalArgumentException(fieldName + " must be an opaque identifier");
        }
        return text;
    }

    private static String requireJoernWorkspaceId(String value) {
        var text = requireIdentifier(value, "workspace id");
        if (!text.startsWith("joern-workspace-")) {
            throw new IllegalArgumentException("workspace id must be owned by Joern");
        }
        return text;
    }

    private static String requirePublicReference(String value, String fieldName) {
        var text = requireText(value, fieldName).replace('\\', '/');
        var lower = text.toLowerCase(Locale.ROOT);
        if (text.startsWith("/")
            || lower.startsWith("file:")
            || WINDOWS_DRIVE_PATH.matcher(text).matches()
            || text.contains("://")) {
            throw new IllegalArgumentException(fieldName + " must not be a private path or URI");
        }
        var parts = List.of(text.split("/"));
        if (parts.stream().anyMatch(part -> part.equals(".") || part.equals("..") || part.isBlank())) {
            throw new IllegalArgumentException(fieldName + " must not contain traversal, current-directory or blank segments");
        }
        return String.join("/", parts);
    }

    private static Map<String, String> safeAttributeMap(Map<String, String> attributes) {
        var values = Map.copyOf(Objects.requireNonNull(attributes, "safe attributes must not be null"));
        values.forEach((key, value) -> {
            var normalizedKey = requireText(key, "safe attribute key").toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
            if (SENSITIVE_KEYS.stream().anyMatch(normalizedKey::contains)) {
                throw new IllegalArgumentException("safe attributes must not contain sensitive keys");
            }
            var normalizedValue = requireText(value, "safe attribute value");
            if (looksLikePrivatePathOrUri(normalizedValue)) {
                throw new IllegalArgumentException("safe attributes must not contain local paths or URIs");
            }
        });
        return values;
    }

    private static String sanitizeMessage(String value) {
        var text = requireText(value, "diagnostic message")
            .replace('\r', ' ')
            .replace('\n', ' ')
            .replace('\\', '/');
        if (looksLikePrivatePathOrUri(text) || containsSensitiveToken(text) || looksLikeSourceSnippet(text)) {
            return "diagnostic details redacted";
        }
        return text;
    }

    private static boolean looksLikePrivatePathOrUri(String value) {
        var text = value.strip().replace('\\', '/');
        var lower = text.toLowerCase(Locale.ROOT);
        return lower.startsWith("file:")
            || lower.contains("://")
            || text.startsWith("/")
            || text.startsWith("//")
            || WINDOWS_DRIVE_PATH.matcher(text).matches()
            || lower.contains("/mnt/")
            || lower.contains("/home/")
            || lower.contains("/users/")
            || lower.contains("/var/")
            || lower.contains("/tmp/")
            || lower.contains("/root/");
    }

    private static boolean containsSensitiveToken(String value) {
        var normalized = value.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
        return SENSITIVE_KEYS.stream().anyMatch(normalized::contains);
    }

    private static boolean looksLikeSourceSnippet(String value) {
        var text = value.toLowerCase(Locale.ROOT);
        return text.contains("public class ")
            || text.contains("private class ")
            || text.contains("protected class ")
            || text.contains("import ")
            || text.contains("package ");
    }
}
