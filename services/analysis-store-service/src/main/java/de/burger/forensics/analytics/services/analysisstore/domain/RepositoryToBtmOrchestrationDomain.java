package de.burger.forensics.analytics.services.analysisstore.domain;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

public final class RepositoryToBtmOrchestrationDomain {
    private static final long MAX_TIMEOUT_SECONDS = 3_600;
    private static final long MAX_WORKSPACE_BYTES = 107_374_182_400L;
    private static final Pattern COMMIT = Pattern.compile("[a-fA-F0-9]{7,64}");
    private static final Pattern PRIVATE_IPV4 = Pattern.compile(
        "^(0\\.|127\\.|10\\.|192\\.168\\.|172\\.(1[6-9]|2[0-9]|3[0-1])\\.|169\\.254\\.)"
    );
    private static final Set<String> SENSITIVE_TOKENS = Set.of(
        "authorization",
        "credential",
        "password",
        "secret",
        "token"
    );

    private RepositoryToBtmOrchestrationDomain() {
    }

    public record StartRepositoryToBtmCommand(
        OrchestrationMetadata metadata,
        RepositoryReference repository,
        RevisionSelector revision,
        WorkspacePolicy workspacePolicy,
        BuildContext buildContext,
        List<RequestedOutput> requestedOutputs,
        Map<String, String> attributes
    ) {
        public StartRepositoryToBtmCommand {
            metadata = Objects.requireNonNull(metadata, "metadata must not be null");
            repository = Objects.requireNonNull(repository, "repository must not be null");
            revision = Objects.requireNonNull(revision, "revision must not be null");
            workspacePolicy = Objects.requireNonNull(workspacePolicy, "workspace policy must not be null");
            buildContext = Objects.requireNonNull(buildContext, "build context must not be null");
            requestedOutputs = List.copyOf(Objects.requireNonNull(requestedOutputs, "requested outputs must not be null"));
            if (requestedOutputs.isEmpty() || requestedOutputs.stream().anyMatch(output -> output != RequestedOutput.BTM_RULES)) {
                throw new IllegalArgumentException("requested outputs must contain BTM_RULES");
            }
            attributes = safeAttributes(attributes);
        }

        public String fingerprint() {
            return List.of(metadata, repository, revision, workspacePolicy, buildContext, requestedOutputs, attributes).toString();
        }
    }

    public record OrchestrationMetadata(
        String requestId,
        String schemaVersion,
        String correlationId,
        AnalysisRunId analysisRunId
    ) {
        public OrchestrationMetadata {
            requestId = RequiredText.require(requestId, "requestId");
            schemaVersion = RequiredText.require(schemaVersion, "schemaVersion");
            correlationId = RequiredText.require(correlationId, "correlationId");
            analysisRunId = Objects.requireNonNull(analysisRunId, "analysisRunId must not be null");
        }
    }

    public record RepositoryReference(String remoteUrl, String provider) {
        public RepositoryReference {
            remoteUrl = requireCleanHttpsRemote(remoteUrl);
            provider = optionalPublicText(provider, "provider");
        }
    }

    public record RevisionSelector(String branch, String commit) {
        public RevisionSelector {
            branch = optionalPublicText(branch, "branch");
            commit = optionalPublicText(commit, "commit");
            if (branch.isBlank() && commit.isBlank()) {
                throw new IllegalArgumentException("branch or commit is required");
            }
            if (!branch.isBlank() && (branch.startsWith("-") || branch.contains(".."))) {
                throw new IllegalArgumentException("branch is not a safe ref");
            }
            if (!commit.isBlank() && !COMMIT.matcher(commit).matches()) {
                throw new IllegalArgumentException("commit must be a hex commit id");
            }
            commit = commit.toLowerCase(Locale.ROOT);
        }
    }

    public record WorkspacePolicy(
        boolean ephemeral,
        boolean allowShallowClone,
        boolean allowPartialClone,
        boolean allowSparseCheckout,
        long timeoutSeconds,
        long maxWorkspaceBytes
    ) {
        public WorkspacePolicy {
            if (ephemeral) {
                throw new IllegalArgumentException("gateway orchestration must not request ephemeral workspaces");
            }
            if (allowPartialClone) {
                throw new IllegalArgumentException("partial clone is not supported");
            }
            if (allowSparseCheckout) {
                throw new IllegalArgumentException("sparse checkout is not supported");
            }
            if (timeoutSeconds < 1 || timeoutSeconds > MAX_TIMEOUT_SECONDS) {
                throw new IllegalArgumentException("timeout seconds must be between 1 and " + MAX_TIMEOUT_SECONDS);
            }
            if (maxWorkspaceBytes < 1 || maxWorkspaceBytes > MAX_WORKSPACE_BYTES) {
                throw new IllegalArgumentException("max workspace bytes must be between 1 and " + MAX_WORKSPACE_BYTES);
            }
        }
    }

    public record BuildContext(
        String buildTool,
        String buildId,
        String rootProjectName,
        List<String> declaredModules,
        Map<String, String> attributes
    ) {
        public BuildContext {
            buildTool = RequiredText.require(buildTool, "buildTool");
            buildId = RequiredText.require(buildId, "buildId");
            rootProjectName = optionalPublicText(rootProjectName, "rootProjectName");
            declaredModules = List.copyOf(Objects.requireNonNullElse(declaredModules, List.of()));
            declaredModules.forEach(module -> RequiredText.require(module, "declaredModule"));
            attributes = safeAttributes(attributes);
        }
    }

    public record RepositoryToBtmOrchestrationStatus(
        OperationStatus status,
        AnalysisRunId analysisRunId,
        AnalysisJobId repositoryAnalysisJobId,
        SourceSnapshotId sourceSnapshotId,
        AnalysisCompleteness completeness,
        OrchestrationState state,
        BtmDeliveryReadiness btmDeliveryReadiness,
        boolean joernSkipped,
        List<RepositoryToBtmDiagnostic> diagnostics,
        List<AnalysisArtifactReference> acceptedGeneratedArtifacts,
        Map<String, String> attributes
    ) {
        public RepositoryToBtmOrchestrationStatus {
            status = Objects.requireNonNull(status, "status must not be null");
            analysisRunId = Objects.requireNonNull(analysisRunId, "analysisRunId must not be null");
            repositoryAnalysisJobId = Objects.requireNonNull(repositoryAnalysisJobId, "repositoryAnalysisJobId must not be null");
            sourceSnapshotId = Objects.requireNonNull(sourceSnapshotId, "sourceSnapshotId must not be null");
            completeness = Objects.requireNonNull(completeness, "completeness must not be null");
            state = Objects.requireNonNull(state, "state must not be null");
            btmDeliveryReadiness = Objects.requireNonNull(btmDeliveryReadiness, "btmDeliveryReadiness must not be null");
            diagnostics = List.copyOf(Objects.requireNonNullElse(diagnostics, List.of()));
            acceptedGeneratedArtifacts = List.copyOf(Objects.requireNonNullElse(acceptedGeneratedArtifacts, List.of()));
            attributes = safeAttributes(attributes);
        }
    }

    public record OperationStatus(String code, String message, boolean retryable, String correlationId, List<String> diagnostics) {
        public OperationStatus {
            code = RequiredText.require(code, "operation status code");
            message = publicMessage(message);
            correlationId = RequiredText.require(correlationId, "correlationId");
            diagnostics = List.copyOf(Objects.requireNonNullElse(diagnostics, List.of()));
        }

        public static OperationStatus accepted(String correlationId, List<RepositoryToBtmDiagnostic> diagnostics) {
            return new OperationStatus(
                "REPOSITORY_TO_BTM_ACCEPTED",
                "Repository-to-BTM orchestration accepted by Analysis Store",
                false,
                correlationId,
                diagnostics.stream().map(RepositoryToBtmDiagnostic::code).toList()
            );
        }
    }

    public record RepositoryToBtmDiagnostic(
        String code,
        String message,
        DiagnosticSeverity severity,
        boolean retryable,
        boolean affectsCompleteness
    ) {
        public RepositoryToBtmDiagnostic {
            code = publicCode(code);
            message = publicMessage(message);
            severity = Objects.requireNonNull(severity, "diagnostic severity must not be null");
        }

        public static RepositoryToBtmDiagnostic warning(String code, String message, boolean affectsCompleteness) {
            return new RepositoryToBtmDiagnostic(code, message, DiagnosticSeverity.WARNING, false, affectsCompleteness);
        }
    }

    public enum RequestedOutput {
        BTM_RULES
    }

    public enum OrchestrationState {
        ACCEPTED,
        WAITING_FOR_REPOSITORY,
        READY_FOR_BTM_DELIVERY,
        INCOMPLETE,
        FAILED
    }

    public enum BtmDeliveryReadiness {
        NOT_READY,
        READY,
        UNAVAILABLE,
        UNKNOWN
    }

    public enum DiagnosticSeverity {
        INFO,
        WARNING,
        ERROR
    }

    public static AnalysisJobId repositoryAnalysisJobId(AnalysisRunId analysisRunId) {
        return new AnalysisJobId("repository-analysis-" + digest(analysisRunId.value()).substring(0, 24));
    }

    public static SourceSnapshotId pendingSourceSnapshotId(AnalysisRunId analysisRunId) {
        return new SourceSnapshotId("source-snapshot-pending-" + digest(analysisRunId.value()).substring(0, 16));
    }

    private static String requireCleanHttpsRemote(String remoteUrl) {
        var candidate = RequiredText.require(remoteUrl, "repository remote url");
        var uri = URI.create(candidate);
        if (!"https".equals(uri.getScheme())) {
            throw new IllegalArgumentException("repository remote url must use https");
        }
        if (uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw new IllegalArgumentException("repository remote url must not contain userinfo, query or fragment");
        }
        var host = normalizedHost(RequiredText.require(uri.getHost(), "repository host"));
        if ("localhost".equals(host) || host.endsWith(".localhost") || PRIVATE_IPV4.matcher(host).find() || isPrivateIpv6(host)) {
            throw new IllegalArgumentException("repository remote url must not target local or private hosts");
        }
        return candidate;
    }

    private static String optionalPublicText(String value, String name) {
        if (value == null || value.isBlank()) {
            return "";
        }
        var text = RequiredText.require(value, name);
        if (containsSensitiveToken(text) || looksLikePrivatePath(text)) {
            throw new IllegalArgumentException(name + " must not contain sensitive or private values");
        }
        return text;
    }

    private static Map<String, String> safeAttributes(Map<String, String> attributes) {
        var sorted = new TreeMap<String, String>();
        Objects.requireNonNullElse(attributes, Map.<String, String>of()).forEach((key, value) -> {
            var safeKey = RequiredText.require(key, "attribute key");
            var safeValue = optionalPublicText(value, "attribute value");
            if (containsSensitiveToken(safeKey)) {
                throw new IllegalArgumentException("attribute key must not contain sensitive values");
            }
            sorted.put(safeKey, safeValue);
        });
        return Map.copyOf(sorted);
    }

    private static String publicCode(String value) {
        var code = RequiredText.require(value, "diagnostic code").trim();
        if (!code.matches("[A-Z0-9_:-]{1,96}") || containsSensitiveToken(code) || looksLikePrivatePath(code)) {
            return "DIAGNOSTIC_REDACTED";
        }
        return code;
    }

    private static String publicMessage(String value) {
        var message = RequiredText.require(value, "diagnostic message")
            .replace('\r', ' ')
            .replace('\n', ' ')
            .replace('\\', '/');
        if (containsSensitiveToken(message) || looksLikePrivatePath(message) || looksLikeRepositoryCoordinate(message)) {
            return "Diagnostic details redacted";
        }
        return message;
    }

    private static boolean containsSensitiveToken(String value) {
        var lower = value.toLowerCase(Locale.ROOT);
        return SENSITIVE_TOKENS.stream().anyMatch(lower::contains);
    }

    private static boolean looksLikePrivatePath(String value) {
        var lower = value.toLowerCase(Locale.ROOT).trim().replace('\\', '/');
        return lower.startsWith("file:")
            || lower.startsWith("/")
            || lower.matches("^[a-z]:.*")
            || lower.contains("/home/")
            || lower.contains("/users/")
            || lower.contains("/mnt/")
            || lower.contains("/tmp/")
            || lower.contains("/var/lib/forensic-analytics")
            || lower.contains("workspace-")
            || lower.contains("repository-workspaces");
    }

    private static boolean looksLikeRepositoryCoordinate(String value) {
        return value.toLowerCase(Locale.ROOT).matches(".*https?://\\S+.*");
    }

    private static String normalizedHost(String host) {
        var normalized = host.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        var zoneIndex = normalized.indexOf('%');
        return zoneIndex < 0 ? normalized : normalized.substring(0, zoneIndex);
    }

    private static boolean isPrivateIpv6(String host) {
        return host.contains(":")
            && ("::".equals(host)
            || "::1".equals(host)
            || "0:0:0:0:0:0:0:0".equals(host)
            || "0:0:0:0:0:0:0:1".equals(host)
            || host.startsWith("::ffff:")
            || host.startsWith("0:0:0:0:0:ffff:")
            || host.startsWith("fc")
            || host.startsWith("fd")
            || host.startsWith("fe80:"));
    }

    private static String digest(String value) {
        try {
            var bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
    }
}
