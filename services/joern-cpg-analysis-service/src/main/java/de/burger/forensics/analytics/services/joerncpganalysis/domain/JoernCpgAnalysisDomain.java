package de.burger.forensics.analytics.services.joerncpganalysis.domain;

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
    public static final String PRODUCER_SERVICE = "joern-cpg-analysis-service";
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
            value = requireText(value, "source snapshot id");
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
            workspaceId = requireIdentifier(workspaceId, "workspace id");
            sourceRoots = List.copyOf(Objects.requireNonNull(sourceRoots, "source roots must not be null")).stream()
                .sorted(Comparator.comparing(SourceRoot::relativePath))
                .toList();
            inputArtifacts = List.copyOf(Objects.requireNonNull(inputArtifacts, "input artifacts must not be null")).stream()
                .sorted(Comparator.comparing(reference -> reference.artifact().path()))
                .toList();
            if (sourceRoots.isEmpty()) {
                throw new IllegalArgumentException("source roots must not be empty");
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

    public record AnalyzeJoernCpgCommand(
        RequestMetadata metadata,
        JoernCpgPolicy policy,
        SourceWorkspace workspace
    ) {
        public AnalyzeJoernCpgCommand {
            metadata = Objects.requireNonNull(metadata, "metadata must not be null");
            policy = Objects.requireNonNull(policy, "policy must not be null");
            workspace = Objects.requireNonNull(workspace, "workspace must not be null");
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
        AnalysisCompleteness completeness
    ) {
        public AnalysisArtifactReference {
            artifact = Objects.requireNonNull(artifact, "artifact must not be null");
            category = Objects.requireNonNull(category, "artifact category must not be null");
            producerService = requireText(producerService, "producer service");
            schemaVersion = requireText(schemaVersion, "schema version");
            completeness = Objects.requireNonNull(completeness, "completeness must not be null");
            if (category != AnalysisArtifactCategory.STATIC) {
                throw new IllegalArgumentException("Joern CPG artifacts must be static semantic artifacts");
            }
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

    public enum AnalysisArtifactCategory {
        STATIC
    }

    public enum AnalysisCompleteness {
        COMPLETE,
        INCOMPLETE,
        UNKNOWN
    }

    public enum DiagnosticSeverity {
        INFO,
        WARNING,
        ERROR
    }

    public static String requireRelativePath(String value, String fieldName) {
        var text = requireText(value, fieldName).replace('\\', '/');
        if (text.startsWith("/")
            || text.startsWith("file:")
            || WINDOWS_DRIVE_PATH.matcher(text).matches()
            || text.contains("://")) {
            throw new IllegalArgumentException(fieldName + " must be a relative path");
        }
        var parts = List.of(text.split("/"));
        if (parts.stream().anyMatch(part -> part.equals("..") || part.isBlank())) {
            throw new IllegalArgumentException(fieldName + " must not contain parent traversal or blank segments");
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

    private static Map<String, String> safeAttributeMap(Map<String, String> attributes) {
        var values = Map.copyOf(Objects.requireNonNull(attributes, "safe attributes must not be null"));
        values.forEach((key, value) -> {
            var normalizedKey = requireText(key, "safe attribute key").toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
            if (SENSITIVE_KEYS.stream().anyMatch(normalizedKey::contains)) {
                throw new IllegalArgumentException("safe attributes must not contain sensitive keys");
            }
            var normalizedValue = requireText(value, "safe attribute value");
            if (normalizedValue.startsWith("file:")
                || normalizedValue.contains("://")
                || WINDOWS_DRIVE_PATH.matcher(normalizedValue).matches()) {
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
        if (text.startsWith("file:") || WINDOWS_DRIVE_PATH.matcher(text).matches()) {
            return "diagnostic details redacted";
        }
        return text;
    }
}
