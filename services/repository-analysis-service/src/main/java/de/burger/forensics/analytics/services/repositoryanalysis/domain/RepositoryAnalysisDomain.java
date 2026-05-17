package de.burger.forensics.analytics.services.repositoryanalysis.domain;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

public final class RepositoryAnalysisDomain {
    private static final long MAX_TIMEOUT_SECONDS = 3_600;
    private static final long MAX_WORKSPACE_BYTES = 107_374_182_400L;
    private static final Pattern SHA_256 = Pattern.compile("[a-fA-F0-9]{64}");
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

    private RepositoryAnalysisDomain() {
    }

    public record AnalysisRunId(String value) {
        public AnalysisRunId {
            value = requireText(value, "analysis run id");
        }
    }

    public record SourceSnapshotId(String value) {
        public SourceSnapshotId {
            value = requireText(value, "source snapshot id");
        }

        public static SourceSnapshotId deterministic(
            RepositoryReference repository,
            RevisionSelector revision,
            String resolvedCommit,
            String manifestSha256
        ) {
            return new SourceSnapshotId("source-snapshot-" + sha256Hex(String.join(
                "\n",
                repository.remoteUrl(),
                revision.branch(),
                revision.commit(),
                requireText(resolvedCommit, "resolved commit"),
                requireSha256(manifestSha256, "manifest checksum")
            )).substring(0, 32));
        }
    }

    public record WorkspaceId(String value) {
        public WorkspaceId {
            value = requireText(value, "workspace id");
            if (looksLikePath(value)) {
                throw new IllegalArgumentException("workspace id must be opaque");
            }
        }
    }

    public record RepositoryReference(String remoteUrl, String provider, Map<String, String> safeAttributes) {
        public RepositoryReference {
            remoteUrl = requireCleanHttpsRemote(remoteUrl);
            provider = optionalText(provider, "provider");
            safeAttributes = RepositoryAnalysisDomain.safeAttributes(safeAttributes);
        }
    }

    public record RevisionSelector(String branch, boolean branchRequired, String commit, boolean commitRequired) {
        public RevisionSelector {
            branch = optionalRef(branch, "branch");
            commit = optionalCommit(commit);
            if (branch.isBlank() && commit.isBlank()) {
                throw new IllegalArgumentException("branch or commit is required");
            }
            if (branchRequired && branch.isBlank()) {
                throw new IllegalArgumentException("branch is required by revision policy");
            }
            if (commitRequired && commit.isBlank()) {
                throw new IllegalArgumentException("commit is required by revision policy");
            }
        }

        public boolean hasBranch() {
            return !branch.isBlank();
        }

        public boolean hasCommit() {
            return !commit.isBlank();
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
            if (allowPartialClone) {
                throw new IllegalArgumentException("partial clone is not supported in Slice 06");
            }
            if (allowSparseCheckout) {
                throw new IllegalArgumentException("sparse checkout is not supported in Slice 06");
            }
            if (timeoutSeconds < 1 || timeoutSeconds > MAX_TIMEOUT_SECONDS) {
                throw new IllegalArgumentException("timeout seconds must be between 1 and " + MAX_TIMEOUT_SECONDS);
            }
            if (maxWorkspaceBytes < 1 || maxWorkspaceBytes > MAX_WORKSPACE_BYTES) {
                throw new IllegalArgumentException("max workspace bytes must be between 1 and " + MAX_WORKSPACE_BYTES);
            }
        }
    }

    public record ArtifactReference(String reference, String type, String sha256, long sizeBytes) {
        public ArtifactReference {
            reference = requireRelativeReference(reference, "artifact reference");
            type = requireText(type, "artifact type");
            sha256 = requireSha256(sha256, "artifact sha256");
            if (sizeBytes < 0) {
                throw new IllegalArgumentException("artifact size must not be negative");
            }
        }
    }

    public record SourceRoot(String relativePath, String language) {
        public SourceRoot {
            relativePath = requireRelativeReference(relativePath, "source root");
            language = requireText(language, "source root language");
        }
    }

    public record Diagnostic(String code, String message, DiagnosticSeverity severity) {
        public Diagnostic {
            code = requireText(code, "diagnostic code");
            message = requireText(message, "diagnostic message");
            severity = Objects.requireNonNullElse(severity, DiagnosticSeverity.INFO);
        }

        public static Diagnostic info(String code, String message) {
            return new Diagnostic(code, message, DiagnosticSeverity.INFO);
        }

        public static Diagnostic error(String code, String message) {
            return new Diagnostic(code, message, DiagnosticSeverity.ERROR);
        }
    }

    public record CheckoutResult(
        CheckoutStatus status,
        String resolvedRemoteUrl,
        String resolvedCommit,
        String requestedBranch,
        String requestedCommit,
        boolean shallowClone,
        long elapsedMillis,
        List<Diagnostic> diagnostics,
        boolean partialClone,
        boolean sparseCheckout,
        List<SourceRoot> sourceRoots
    ) {
        public CheckoutResult {
            status = Objects.requireNonNull(status, "checkout status must not be null");
            resolvedRemoteUrl = requireCleanHttpsRemote(resolvedRemoteUrl);
            resolvedCommit = requireText(resolvedCommit, "resolved commit");
            requestedBranch = optionalText(requestedBranch, "requested branch");
            requestedCommit = optionalText(requestedCommit, "requested commit");
            if (elapsedMillis < 0) {
                throw new IllegalArgumentException("elapsed millis must not be negative");
            }
            diagnostics = List.copyOf(Objects.requireNonNullElse(diagnostics, List.of()));
            sourceRoots = List.copyOf(Objects.requireNonNullElse(sourceRoots, List.of()));
            if (sourceRoots.isEmpty()) {
                throw new IllegalArgumentException("at least one source root is required");
            }
        }
    }

    public record SourceSnapshot(
        SourceSnapshotId sourceSnapshotId,
        SourceSnapshotCompleteness completeness,
        List<SourceRoot> sourceRoots,
        ArtifactReference manifestArtifact,
        List<String> limitations
    ) {
        public SourceSnapshot {
            Objects.requireNonNull(sourceSnapshotId, "source snapshot id must not be null");
            completeness = Objects.requireNonNullElse(completeness, SourceSnapshotCompleteness.UNKNOWN);
            sourceRoots = List.copyOf(Objects.requireNonNullElse(sourceRoots, List.of()));
            if (sourceRoots.isEmpty()) {
                throw new IllegalArgumentException("at least one source root is required");
            }
            Objects.requireNonNull(manifestArtifact, "manifest artifact must not be null");
            limitations = List.copyOf(Objects.requireNonNullElse(limitations, List.of()));
        }
    }

    public record RepositoryPreparation(
        AnalysisRunId analysisRunId,
        SourceSnapshotId sourceSnapshotId,
        WorkspaceId workspaceId,
        RepositoryReference repository,
        RevisionSelector requestedRevision,
        CheckoutResult checkout,
        SourceSnapshot sourceSnapshot,
        RepositoryWorkspaceStatus workspaceStatus,
        Instant createdAt,
        Instant updatedAt,
        List<Diagnostic> diagnostics,
        Map<String, String> safeAttributes
    ) {
        public RepositoryPreparation {
            Objects.requireNonNull(analysisRunId, "analysis run id must not be null");
            Objects.requireNonNull(sourceSnapshotId, "source snapshot id must not be null");
            Objects.requireNonNull(workspaceId, "workspace id must not be null");
            Objects.requireNonNull(repository, "repository must not be null");
            Objects.requireNonNull(requestedRevision, "requested revision must not be null");
            Objects.requireNonNull(checkout, "checkout must not be null");
            Objects.requireNonNull(sourceSnapshot, "source snapshot must not be null");
            if (!sourceSnapshotId.equals(sourceSnapshot.sourceSnapshotId())) {
                throw new IllegalArgumentException("preparation source snapshot id must match nested snapshot");
            }
            workspaceStatus = Objects.requireNonNullElse(workspaceStatus, RepositoryWorkspaceStatus.CHECKED_OUT);
            Objects.requireNonNull(createdAt, "created at must not be null");
            Objects.requireNonNull(updatedAt, "updated at must not be null");
            diagnostics = List.copyOf(Objects.requireNonNullElse(diagnostics, List.of()));
            safeAttributes = RepositoryAnalysisDomain.safeAttributes(safeAttributes);
        }

        public RepositoryPreparation withWorkspaceStatus(RepositoryWorkspaceStatus status, Instant timestamp) {
            return new RepositoryPreparation(
                analysisRunId,
                sourceSnapshotId,
                workspaceId,
                repository,
                requestedRevision,
                checkout,
                sourceSnapshot,
                status,
                createdAt,
                timestamp,
                diagnostics,
                safeAttributes
            );
        }
    }

    public enum CheckoutStatus {
        ACCEPTED,
        WORKSPACE_PREPARED,
        CHECKED_OUT,
        FAILED
    }

    public enum RepositoryWorkspaceStatus {
        READY,
        CHECKED_OUT,
        CLEANED,
        FAILED
    }

    public enum SourceSnapshotCompleteness {
        COMPLETE,
        INCOMPLETE,
        UNKNOWN
    }

    public enum DiagnosticSeverity {
        INFO,
        WARNING,
        ERROR
    }

    public static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }

    public static Map<String, String> safeAttributes(Map<String, String> attributes) {
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

    public static String sha256Hex(String value) {
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

    private static String requireCleanHttpsRemote(String remoteUrl) {
        var candidate = requireText(remoteUrl, "repository remote url");
        var uri = URI.create(candidate);
        if (!"https".equals(uri.getScheme())) {
            throw new IllegalArgumentException("repository remote url must use https");
        }
        if (uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw new IllegalArgumentException("repository remote url must not contain userinfo, query or fragment");
        }
        var host = normalizedHost(requireText(uri.getHost(), "repository remote host"));
        if ("localhost".equals(host) || host.endsWith(".localhost") || PRIVATE_IPV4.matcher(host).find()
            || "169.254.169.254".equals(host) || isPrivateIpv6(host)) {
            throw new IllegalArgumentException("repository remote url must not target local or private hosts");
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

    private static String optionalText(String value, String name) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return requireText(value, name);
    }

    private static String optionalRef(String value, String name) {
        var ref = optionalText(value, name);
        if (ref.isBlank()) {
            return "";
        }
        if (ref.startsWith("-") || ref.contains("..") || ref.contains("\n") || ref.contains("\r")) {
            throw new IllegalArgumentException(name + " is not a safe ref");
        }
        return ref;
    }

    private static String optionalCommit(String value) {
        var commit = optionalText(value, "commit");
        if (!commit.isBlank() && !COMMIT.matcher(commit).matches()) {
            throw new IllegalArgumentException("commit must be a hex commit id");
        }
        return commit.toLowerCase(Locale.ROOT);
    }

    private static String requireRelativeReference(String value, String name) {
        var reference = requireText(value, name);
        var lower = reference.toLowerCase(Locale.ROOT);
        if (reference.startsWith("/") || reference.startsWith("\\") || reference.contains("\\")
            || lower.startsWith("file:") || reference.matches("^[A-Za-z]:.*")
            || reference.contains("../") || reference.contains("/..") || "..".equals(reference)
            || reference.contains("\n") || reference.contains("\r")) {
            throw new IllegalArgumentException(name + " must be relative and service-owned");
        }
        return reference;
    }

    private static String requireSha256(String value, String name) {
        var checksum = requireText(value, name).toLowerCase(Locale.ROOT);
        if (!SHA_256.matcher(checksum).matches()) {
            throw new IllegalArgumentException(name + " must be a SHA-256 hex value");
        }
        return checksum;
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
