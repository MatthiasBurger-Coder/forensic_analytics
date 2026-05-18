package de.burger.forensics.analytics.services.gateway.domain;

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

public final class GatewayRepositoryAnalysis {
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

    private GatewayRepositoryAnalysis() {
    }

    public record SubmissionRequest(
        String requestId,
        String idempotencyKey,
        String schemaVersion,
        String correlationId,
        List<String> requestedOutputs,
        String repositoryUrl,
        String provider,
        String branch,
        String commit,
        String workspaceName,
        BuildContext buildContext,
        WorkspacePolicy workspacePolicy
    ) {
        public SubmissionRequest {
            requestId = requireText(requestId, "request id");
            idempotencyKey = requireText(idempotencyKey, "idempotency key");
            schemaVersion = requireText(schemaVersion, "schema version");
            correlationId = requireText(correlationId, "correlation id");
            requestedOutputs = validatedRequestedOutputs(requestedOutputs);
            repositoryUrl = requireCleanHttpsRemote(repositoryUrl);
            provider = optionalSafeText(provider, "provider");
            branch = optionalRef(branch, "branch");
            commit = optionalCommit(commit);
            if (branch.isBlank() && commit.isBlank()) {
                throw new IllegalArgumentException("branch or commit is required");
            }
            if (workspaceName != null && !workspaceName.isBlank()) {
                throw new IllegalArgumentException("workspace name must not be provided");
            }
            workspaceName = "";
            Objects.requireNonNull(buildContext, "build context must not be null");
            Objects.requireNonNull(workspacePolicy, "workspace policy must not be null");
        }

        public String analysisRunId() {
            return "analysis-run-" + sha256Hex(String.join(
                "\n",
                requestId,
                schemaVersion,
                repositoryUrl,
                branch,
                commit
            )).substring(0, 32);
        }

        public String fingerprint() {
            return String.join(
                "|",
                requestId,
                schemaVersion,
                correlationId,
                requestedOutputs.toString(),
                repositoryUrl,
                provider,
                branch,
                commit,
                buildContext.fingerprint(),
                workspacePolicy.fingerprint()
            );
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
            buildTool = requireText(buildTool, "build tool");
            buildId = requireText(buildId, "build id");
            rootProjectName = optionalSafeText(rootProjectName, "root project name");
            declaredModules = List.copyOf(Objects.requireNonNullElse(declaredModules, List.of()));
            declaredModules.forEach(module -> requireText(module, "declared module"));
            attributes = safeAttributes(attributes);
        }

        private String fingerprint() {
            return String.join("|", buildTool, buildId, rootProjectName, declaredModules.toString(), attributes.toString());
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
                throw new IllegalArgumentException("gateway workspace policy must not request ephemeral workspaces");
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

        private String fingerprint() {
            return String.join(
                "|",
                Boolean.toString(ephemeral),
                Boolean.toString(allowShallowClone),
                Boolean.toString(allowPartialClone),
                Boolean.toString(allowSparseCheckout),
                Long.toString(timeoutSeconds),
                Long.toString(maxWorkspaceBytes)
            );
        }
    }

    public record RepositoryPreparationCommand(
        String analysisRunId,
        SubmissionRequest request
    ) {
        public RepositoryPreparationCommand {
            analysisRunId = requireText(analysisRunId, "analysis run id");
            Objects.requireNonNull(request, "request must not be null");
        }
    }

    public record RepositoryPreparationResult(
        String analysisRunId,
        String sourceSnapshotId,
        String checkoutStatus,
        List<Diagnostic> diagnostics
    ) {
        public RepositoryPreparationResult {
            analysisRunId = requireText(analysisRunId, "analysis run id");
            sourceSnapshotId = requireText(sourceSnapshotId, "source snapshot id");
            checkoutStatus = optionalSafeText(checkoutStatus, "checkout status");
            diagnostics = List.copyOf(Objects.requireNonNullElse(diagnostics, List.of()));
        }
    }

    public record RepositoryToBtmSubmission(
        String analysisRunId,
        String status,
        String statusUrl,
        String jobsUrl,
        String btmDeliveryStatus,
        String btmDeliveryService,
        String correlationId,
        List<Diagnostic> diagnostics
    ) {
        public RepositoryToBtmSubmission {
            analysisRunId = requireText(analysisRunId, "analysis run id");
            status = requireText(status, "status");
            statusUrl = requireText(statusUrl, "status url");
            jobsUrl = requireText(jobsUrl, "jobs url");
            btmDeliveryStatus = requireText(btmDeliveryStatus, "btm delivery status");
            btmDeliveryService = optionalSafeText(btmDeliveryService, "btm delivery service");
            correlationId = requireText(correlationId, "correlation id");
            diagnostics = List.copyOf(Objects.requireNonNullElse(diagnostics, List.of()));
        }
    }

    public record Diagnostic(String severity, String code, String message) {
        public Diagnostic {
            severity = optionalSafeText(severity, "diagnostic severity");
            code = optionalSafeText(code, "diagnostic code");
            message = requireText(message, "diagnostic message");
        }

        public static Diagnostic info(String code, String message) {
            return new Diagnostic("INFO", code, message);
        }
    }

    public static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }

    private static List<String> validatedRequestedOutputs(List<String> outputs) {
        var requested = List.copyOf(Objects.requireNonNullElse(outputs, List.of()));
        if (requested.isEmpty()) {
            throw new IllegalArgumentException("requested outputs must not be empty");
        }
        requested.forEach(output -> {
            if (!"BTM_RULES".equals(output)) {
                throw new IllegalArgumentException("unsupported requested output");
            }
        });
        return requested;
    }

    private static Map<String, String> safeAttributes(Map<String, String> attributes) {
        var sorted = new TreeMap<String, String>();
        Objects.requireNonNullElse(attributes, Map.<String, String>of()).forEach((key, value) -> {
            var safeKey = requireText(key, "safe attribute key");
            var safeValue = requireText(value, "safe attribute value");
            if (containsSensitiveToken(safeKey) || containsSensitiveToken(safeValue) || looksLikePath(safeValue)) {
                throw new IllegalArgumentException("safe attributes must not contain sensitive or private values");
            }
            sorted.put(safeKey, safeValue);
        });
        return Map.copyOf(sorted);
    }

    private static String requireCleanHttpsRemote(String remoteUrl) {
        var candidate = requireText(remoteUrl, "repository url");
        var uri = URI.create(candidate);
        if (!"https".equals(uri.getScheme())) {
            throw new IllegalArgumentException("repository url must use https");
        }
        if (uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw new IllegalArgumentException("repository url must not contain userinfo, query or fragment");
        }
        var host = normalizedHost(requireText(uri.getHost(), "repository host"));
        if ("localhost".equals(host) || host.endsWith(".localhost") || PRIVATE_IPV4.matcher(host).find()
            || "169.254.169.254".equals(host) || isPrivateIpv6(host)) {
            throw new IllegalArgumentException("repository url must not target local or private hosts");
        }
        return candidate;
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

    private static String optionalSafeText(String value, String name) {
        if (value == null || value.isBlank()) {
            return "";
        }
        var text = requireText(value, name);
        if (containsSensitiveToken(text) || looksLikePath(text)) {
            throw new IllegalArgumentException(name + " must not contain sensitive or private values");
        }
        return text;
    }

    private static String optionalRef(String value, String name) {
        var ref = optionalSafeText(value, name);
        if (ref.isBlank()) {
            return "";
        }
        if (ref.startsWith("-") || ref.contains("..") || ref.contains("\n") || ref.contains("\r")) {
            throw new IllegalArgumentException(name + " is not a safe ref");
        }
        return ref;
    }

    private static String optionalCommit(String value) {
        var commit = optionalSafeText(value, "commit");
        if (!commit.isBlank() && !COMMIT.matcher(commit).matches()) {
            throw new IllegalArgumentException("commit must be a hex commit id");
        }
        return commit.toLowerCase(Locale.ROOT);
    }

    private static String sha256Hex(String value) {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            var builder = new StringBuilder(digest.length * 2);
            for (byte current : digest) {
                builder.append("%02x".formatted(current));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
    }

    private static boolean containsSensitiveToken(String value) {
        var lower = value.toLowerCase(Locale.ROOT);
        return SENSITIVE_TOKENS.stream().anyMatch(lower::contains);
    }

    private static boolean looksLikePath(String value) {
        var lower = value.toLowerCase(Locale.ROOT).trim();
        return lower.startsWith("file:")
            || lower.startsWith("/")
            || lower.startsWith("\\")
            || lower.matches("^[a-z]:.*")
            || lower.contains("/home/")
            || lower.contains("\\users\\")
            || lower.contains("/users/")
            || lower.contains("/mnt/");
    }
}
