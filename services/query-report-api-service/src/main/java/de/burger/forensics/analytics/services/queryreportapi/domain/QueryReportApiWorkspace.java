package de.burger.forensics.analytics.services.queryreportapi.domain;

import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.Diagnostic;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public final class QueryReportApiWorkspace {
    private static final long MAX_TIMEOUT_SECONDS = 3_600;
    private static final long MAX_WORKSPACE_BYTES = 107_374_182_400L;
    private static final Pattern COMMIT = Pattern.compile("[a-fA-F0-9]{7,128}");
    private static final Pattern OPAQUE_WORKSPACE_ID = Pattern.compile("[A-Za-z0-9_-]+");
    private static final Pattern PRIVATE_IPV4 = Pattern.compile(
        "^(0\\.|127\\.|10\\.|192\\.168\\.|172\\.(1[6-9]|2[0-9]|3[0-1])\\.|169\\.254\\.|"
            + "100\\.(6[4-9]|[7-9][0-9]|1[01][0-9]|12[0-7])\\.|192\\.0\\.(0|2)\\.|"
            + "192\\.88\\.99\\.|192\\.31\\.196\\.|192\\.52\\.193\\.|192\\.175\\.48\\.|"
            + "198\\.(1[89]|51\\.100)\\.|203\\.0\\.113\\.|"
            + "(22[4-9]|23[0-9]|24[0-9]|25[0-5])\\.)"
    );

    private QueryReportApiWorkspace() {
    }

    public record WorkspaceFacadeConfiguration(
        String schemaVersion,
        long metadataTimeoutSeconds,
        WorkspacePolicy refreshPolicy
    ) {
        public WorkspaceFacadeConfiguration {
            schemaVersion = requireText(schemaVersion, "workspace schema version");
            if (metadataTimeoutSeconds < 1 || metadataTimeoutSeconds > MAX_TIMEOUT_SECONDS) {
                throw new IllegalArgumentException("metadata timeout seconds must be between 1 and " + MAX_TIMEOUT_SECONDS);
            }
            Objects.requireNonNull(refreshPolicy, "refresh workspace policy must not be null");
        }
    }

    public record WorkspaceMetadataRequest(
        String requestId,
        String idempotencyKey,
        String schemaVersion,
        String correlationId,
        String repositoryUrl,
        long metadataTimeoutSeconds
    ) {
        public WorkspaceMetadataRequest {
            requestId = requireText(requestId, "request id");
            idempotencyKey = requireText(idempotencyKey, "idempotency key");
            schemaVersion = requireText(schemaVersion, "schema version");
            correlationId = requireText(correlationId, "correlation id");
            repositoryUrl = requireCleanHttpsRepositoryUrl(repositoryUrl);
            if (metadataTimeoutSeconds < 1 || metadataTimeoutSeconds > MAX_TIMEOUT_SECONDS) {
                throw new IllegalArgumentException("metadata timeout seconds must be between 1 and " + MAX_TIMEOUT_SECONDS);
            }
        }

        public String fingerprint() {
            return String.join("|", schemaVersion, repositoryUrl, Long.toString(metadataTimeoutSeconds));
        }
    }

    public record CreateWorkspaceRequest(
        String requestId,
        String idempotencyKey,
        String schemaVersion,
        String correlationId,
        String repositoryUrl,
        String selectedBranch,
        WorkspacePolicy workspacePolicy
    ) {
        public CreateWorkspaceRequest {
            requestId = requireText(requestId, "request id");
            idempotencyKey = requireText(idempotencyKey, "idempotency key");
            schemaVersion = requireText(schemaVersion, "schema version");
            correlationId = requireText(correlationId, "correlation id");
            repositoryUrl = requireCleanHttpsRepositoryUrl(repositoryUrl);
            selectedBranch = optionalBranch(selectedBranch);
            Objects.requireNonNull(workspacePolicy, "workspace policy must not be null");
        }
    }

    public record GetWorkspaceRequest(String requestId, String correlationId, String workspaceId) {
        public GetWorkspaceRequest {
            requestId = requireText(requestId, "request id");
            correlationId = requireText(correlationId, "correlation id");
            workspaceId = requireOpaqueId(workspaceId, "workspace id", "workspace-");
        }
    }

    public record RefreshWorkspaceBranchRequest(
        String requestId,
        String idempotencyKey,
        String schemaVersion,
        String correlationId,
        String workspaceId,
        String workspaceBranchId,
        WorkspacePolicy workspacePolicy
    ) {
        public RefreshWorkspaceBranchRequest {
            requestId = requireText(requestId, "request id");
            idempotencyKey = requireText(idempotencyKey, "idempotency key");
            schemaVersion = requireText(schemaVersion, "schema version");
            correlationId = requireText(correlationId, "correlation id");
            workspaceId = requireOpaqueId(workspaceId, "workspace id", "workspace-");
            workspaceBranchId = requireOpaqueId(workspaceBranchId, "workspace branch id", "workspace-branch-");
            Objects.requireNonNull(workspacePolicy, "workspace policy must not be null");
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
                throw new IllegalArgumentException("query report API workspace policy must not request ephemeral workspaces");
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

    public record WorkspaceMetadataResponse(
        String repositoryKey,
        String repositoryHost,
        String repositoryOwner,
        String repositoryName,
        String workspaceTitle,
        String defaultBranch,
        List<Diagnostic> diagnostics
    ) {
        public WorkspaceMetadataResponse {
            repositoryKey = requireRepositoryKey(repositoryKey);
            repositoryHost = requirePublicText(repositoryHost, "repository host");
            repositoryOwner = optionalNullablePublicText(repositoryOwner, "repository owner");
            repositoryName = requirePublicText(repositoryName, "repository name");
            workspaceTitle = requirePublicText(workspaceTitle, "workspace title");
            defaultBranch = optionalNullablePublicText(defaultBranch, "default branch");
            diagnostics = List.copyOf(Objects.requireNonNullElse(diagnostics, List.of()));
        }
    }

    public record RepositoryIdentity(
        String repositoryKey,
        String repositoryUrl,
        String repositoryHost,
        String repositoryOwner,
        String repositoryName,
        String defaultBranch
    ) {
        public RepositoryIdentity {
            repositoryKey = requireRepositoryKey(repositoryKey);
            repositoryUrl = requireCleanHttpsRepositoryUrl(repositoryUrl);
            repositoryHost = requirePublicText(repositoryHost, "repository host");
            repositoryOwner = optionalNullablePublicText(repositoryOwner, "repository owner");
            repositoryName = requirePublicText(repositoryName, "repository name");
            defaultBranch = optionalNullablePublicText(defaultBranch, "default branch");
        }
    }

    public record WorkspaceResponse(
        String workspaceId,
        String workspaceTitle,
        RepositoryIdentity repository,
        List<WorkspaceBranchResponse> branches,
        String status,
        List<Diagnostic> diagnostics
    ) {
        public WorkspaceResponse {
            workspaceId = requireOpaqueId(workspaceId, "workspace id", "workspace-");
            workspaceTitle = requirePublicText(workspaceTitle, "workspace title");
            Objects.requireNonNull(repository, "repository identity must not be null");
            branches = List.copyOf(Objects.requireNonNullElse(branches, List.of()));
            status = workspaceStatus(status);
            diagnostics = List.copyOf(Objects.requireNonNullElse(diagnostics, List.of()));
        }
    }

    public record WorkspaceBranchResponse(
        String workspaceBranchId,
        String repositoryBranch,
        String status,
        String resolvedCommit,
        String sourceSnapshotId,
        List<String> sourceRoots,
        List<Diagnostic> diagnostics
    ) {
        public WorkspaceBranchResponse {
            workspaceBranchId = requireOpaqueId(workspaceBranchId, "workspace branch id", "workspace-branch-");
            repositoryBranch = requireBranch(repositoryBranch, "repository branch");
            status = branchStatus(status);
            resolvedCommit = optionalCommit(resolvedCommit);
            sourceSnapshotId = optionalNullableOpaqueId(sourceSnapshotId, "source snapshot id", "source-snapshot-");
            sourceRoots = List.copyOf(Objects.requireNonNullElse(sourceRoots, List.of()));
            sourceRoots.forEach(QueryReportApiWorkspace::requireSourceRoot);
            diagnostics = List.copyOf(Objects.requireNonNullElse(diagnostics, List.of()));
        }
    }

    public record BranchRefreshResponse(
        String workspaceBranchId,
        String repositoryBranch,
        String status,
        boolean changed,
        String previousCommit,
        String resolvedCommit,
        String sourceSnapshotId,
        List<Diagnostic> diagnostics
    ) {
        public BranchRefreshResponse {
            workspaceBranchId = requireOpaqueId(workspaceBranchId, "workspace branch id", "workspace-branch-");
            repositoryBranch = requireBranch(repositoryBranch, "repository branch");
            status = branchStatus(status);
            previousCommit = optionalCommit(previousCommit);
            resolvedCommit = optionalCommit(resolvedCommit);
            sourceSnapshotId = optionalNullableOpaqueId(sourceSnapshotId, "source snapshot id", "source-snapshot-");
            diagnostics = List.copyOf(Objects.requireNonNullElse(diagnostics, List.of()));
        }
    }

    public static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }

    public static String requireCleanHttpsRepositoryUrl(String remoteUrl) {
        var candidate = requireText(remoteUrl, "repository url");
        var uri = URI.create(candidate);
        if (!"https".equals(uri.getScheme()) || uri.getRawUserInfo() != null
            || uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw new IllegalArgumentException("repository url must be a clean https URL");
        }
        var host = normalizedHost(requireText(uri.getHost(), "repository host"));
        if (isSpecialUseHostName(host)
            || PRIVATE_IPV4.matcher(host).find()
            || "169.254.169.254".equals(host)
            || isPrivateIpv6(host)
            || resolvesToPrivateAddress(host)) {
            throw new IllegalArgumentException("repository url must not target local, private or special-use hosts");
        }
        return candidate;
    }

    private static String requireRepositoryKey(String key) {
        var value = requireText(key, "repository key").toLowerCase(Locale.ROOT);
        var parts = value.split("/");
        if (parts.length != 3 || Arrays.stream(parts).anyMatch(part -> part.isBlank() || part.contains(":")
            || part.contains("?") || part.contains("#") || part.contains("@") || part.contains("\\"))) {
            throw new IllegalArgumentException("repository key must use host/owner/repository form");
        }
        return value;
    }

    private static String requirePublicText(String value, String name) {
        var text = requireText(value, name);
        if (looksLikeLeak(text)) {
            throw new IllegalArgumentException(name + " must be safe public text");
        }
        return text;
    }

    private static String optionalNullablePublicText(String value, String name) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return requirePublicText(value, name);
    }

    private static String optionalBranch(String value) {
        if (value == null) {
            return "";
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("selected branch must not be blank");
        }
        return requireBranch(value, "selected branch");
    }

    private static String requireBranch(String value, String name) {
        var branch = requirePublicText(value, name);
        if (branch.startsWith("-") || branch.contains("..") || branch.contains("\n") || branch.contains("\r")) {
            throw new IllegalArgumentException(name + " must be a safe ref");
        }
        return branch;
    }

    private static String optionalCommit(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        var commit = requireText(value, "commit").toLowerCase(Locale.ROOT);
        if (!COMMIT.matcher(commit).matches()) {
            throw new IllegalArgumentException("commit must be a hex commit id");
        }
        return commit;
    }

    private static String requireOpaqueId(String value, String name, String prefix) {
        var id = requireText(value, name);
        if (!id.startsWith(prefix) || id.length() == prefix.length() || id.contains("/") || id.contains("\\")
            || !OPAQUE_WORKSPACE_ID.matcher(id).matches()) {
            throw new IllegalArgumentException(name + " must be opaque");
        }
        return id;
    }

    private static String optionalNullableOpaqueId(String value, String name, String prefix) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return requireOpaqueId(value, name, prefix);
    }

    private static void requireSourceRoot(String sourceRoot) {
        var root = requirePublicText(sourceRoot, "source root");
        if (root.startsWith("/") || root.startsWith("\\") || root.matches("^[A-Za-z]:.*")
            || hasUnsafePathSegments(root)) {
            throw new IllegalArgumentException("source root must be relative");
        }
    }

    private static String workspaceStatus(String status) {
        var value = requireText(status, "workspace status").toUpperCase(Locale.ROOT);
        return switch (value) {
            case "NEW", "CHECKING_OUT", "READY", "CHECKED_OUT", "CLEANED", "FAILED" -> value;
            default -> throw new IllegalArgumentException("workspace status is not public");
        };
    }

    private static String branchStatus(String status) {
        var value = requireText(status, "branch status").toUpperCase(Locale.ROOT);
        return switch (value) {
            case "CHECKING_OUT", "CHECKED_OUT", "UP_TO_DATE", "UPDATING", "UPDATED", "FAILED" -> value;
            default -> throw new IllegalArgumentException("branch status is not public");
        };
    }

    private static String normalizedHost(String host) {
        var normalized = host.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        while (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        var zoneIndex = normalized.indexOf('%');
        return zoneIndex < 0 ? normalized : normalized.substring(0, zoneIndex);
    }

    private static boolean isSpecialUseHostName(String host) {
        return "localhost".equals(host)
            || "local".equals(host)
            || "test".equals(host)
            || "invalid".equals(host)
            || "example".equals(host)
            || host.endsWith(".localhost")
            || host.endsWith(".local")
            || host.endsWith(".test")
            || host.endsWith(".invalid")
            || host.endsWith(".example");
    }

    private static boolean isPrivateIpv6(String host) {
        if (!host.contains(":")) {
            return false;
        }
        try {
            return isPrivateAddress(InetAddress.getByName(host));
        } catch (UnknownHostException error) {
            return true;
        }
    }

    private static boolean resolvesToPrivateAddress(String host) {
        try {
            return Arrays.stream(InetAddress.getAllByName(host))
                .anyMatch(QueryReportApiWorkspace::isPrivateAddress);
        } catch (UnknownHostException error) {
            return false;
        }
    }

    private static boolean isPrivateAddress(InetAddress address) {
        var raw = address.getAddress();
        return address.isAnyLocalAddress()
            || address.isLoopbackAddress()
            || address.isLinkLocalAddress()
            || address.isSiteLocalAddress()
            || address.isMulticastAddress()
            || PRIVATE_IPV4.matcher(address.getHostAddress()).find()
            || isPrivateIpv6Bytes(raw);
    }

    private static boolean isPrivateIpv6Bytes(byte[] raw) {
        if (raw.length != 16) {
            return false;
        }
        return isIpv4MappedIpv6(raw)
            || ((raw[0] & 0xff) == 0xfc || (raw[0] & 0xff) == 0xfd)
            || ((raw[0] & 0xff) == 0xfe && ((raw[1] & 0xc0) == 0x80))
            || ((raw[0] & 0xff) == 0x01 && raw[1] == 0 && zeroRange(raw, 2, 8))
            || isBytePrefix(raw, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01)
            || isBytePrefix(raw, 0x00, 0x64, 0xff, 0x9b) && zeroRange(raw, 4, 12)
            || isBytePrefix(raw, 0x00, 0x64, 0xff, 0x9b, 0x00, 0x01)
            || ((raw[0] & 0xff) == 0x20 && (raw[1] & 0xff) == 0x01 && (raw[2] & 0xfe) == 0x00)
            || isBytePrefix(raw, 0x20, 0x01, 0x0d, 0xb8)
            || isBytePrefix(raw, 0x20, 0x02)
            || isBytePrefix(raw, 0x26, 0x20, 0x00, 0x4f, 0x80, 0x00)
            || ((raw[0] & 0xff) == 0x3f && (raw[1] & 0xff) == 0xff && (raw[2] & 0xf0) == 0)
            || isBytePrefix(raw, 0x5f, 0x00);
    }

    private static boolean isIpv4MappedIpv6(byte[] raw) {
        return zeroRange(raw, 0, 10) && (raw[10] & 0xff) == 0xff && (raw[11] & 0xff) == 0xff;
    }

    private static boolean zeroRange(byte[] raw, int startInclusive, int endExclusive) {
        for (var index = startInclusive; index < endExclusive; index++) {
            if (raw[index] != 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean isBytePrefix(byte[] raw, int... prefix) {
        for (var index = 0; index < prefix.length; index++) {
            if ((raw[index] & 0xff) != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasUnsafePathSegments(String value) {
        return Arrays.stream(value.replace('\\', '/').split("/"))
            .anyMatch(segment -> ".".equals(segment) || "..".equals(segment));
    }

    private static boolean looksLikeLeak(String value) {
        var lower = value.toLowerCase(Locale.ROOT).replace('\\', '/');
        return lower.startsWith("file:")
            || lower.startsWith("jdbc:")
            || lower.startsWith("/")
            || lower.matches("^[a-z]:.*")
            || lower.contains("h2")
            || lower.contains("https://")
            || lower.contains("http://")
            || lower.contains("/tmp")
            || lower.contains("/mnt/")
            || lower.contains("/home/")
            || lower.contains("/users/")
            || lower.contains("/var/lib/forensic-analytics")
            || lower.contains("repository-source-data")
            || lower.contains("repository-workspaces")
            || lower.contains("raw stdout")
            || lower.contains("raw stderr")
            || lower.contains("stdout")
            || lower.contains("stderr")
            || lower.contains("token")
            || lower.contains("password")
            || lower.contains("secret")
            || lower.contains("credential")
            || lower.contains("authorization");
    }
}
