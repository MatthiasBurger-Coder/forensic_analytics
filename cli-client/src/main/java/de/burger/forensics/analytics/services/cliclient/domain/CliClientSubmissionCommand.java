package de.burger.forensics.analytics.services.cliclient.domain;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

public record CliClientSubmissionCommand(
    URI publicApiBaseUrl,
    String repositoryUrl,
    String branch,
    String commit,
    String requestId,
    String schemaVersion,
    List<String> requestedOutputs,
    String provider,
    String buildTool,
    String buildId,
    String rootProjectName,
    List<String> declaredModules,
    String correlationId,
    String idempotencyKey,
    long timeoutSeconds,
    long maxWorkspaceBytes,
    boolean allowShallowClone
) {
    private static final long MAX_TIMEOUT_SECONDS = 3_600L;
    private static final long MAX_WORKSPACE_BYTES = 107_374_182_400L;

    public CliClientSubmissionCommand {
        publicApiBaseUrl = validatePublicApiBaseUrl(publicApiBaseUrl);
        repositoryUrl = validateRepositoryUrl(repositoryUrl);
        branch = optionalText(branch);
        commit = optionalText(commit);
        if (branch.isBlank() && commit.isBlank()) {
            throw new CliClientValidationException("gateway-submit requires --branch or --commit.");
        }
        requestId = requireText(requestId, "--request-id");
        schemaVersion = requireText(schemaVersion, "--schema-version");
        requestedOutputs = List.copyOf(Objects.requireNonNull(requestedOutputs, "requestedOutputs must not be null"));
        if (requestedOutputs.isEmpty()) {
            throw new CliClientValidationException("gateway-submit requires at least one --requested-outputs value.");
        }
        requestedOutputs.forEach(output -> {
            if (!"BTM_RULES".equals(requireText(output, "--requested-outputs"))) {
                throw new CliClientValidationException("Unsupported gateway-submit requested output: " + output);
            }
        });
        provider = optionalText(provider);
        buildTool = requireText(buildTool, "--build-tool");
        buildId = requireText(buildId, "--build-id");
        rootProjectName = requireText(rootProjectName, "--root-project");
        declaredModules = List.copyOf(Objects.requireNonNull(declaredModules, "declaredModules must not be null"));
        if (declaredModules.isEmpty()) {
            throw new CliClientValidationException("gateway-submit requires at least one --declared-modules value.");
        }
        declaredModules.forEach(module -> requireText(module, "--declared-modules"));
        correlationId = requireSafeHeader(correlationId, "--correlation-id");
        idempotencyKey = requireSafeHeader(idempotencyKey, "--idempotency-key");
        if (timeoutSeconds < 1 || timeoutSeconds > MAX_TIMEOUT_SECONDS) {
            throw new CliClientValidationException("gateway-submit --timeout-seconds must be between 1 and " + MAX_TIMEOUT_SECONDS + ".");
        }
        if (maxWorkspaceBytes < 1 || maxWorkspaceBytes > MAX_WORKSPACE_BYTES) {
            throw new CliClientValidationException("gateway-submit --max-workspace-bytes must be between 1 and " + MAX_WORKSPACE_BYTES + ".");
        }
    }

    public URI repositoryAnalysesUri() {
        var base = publicApiBaseUrl.toString();
        return URI.create(base + (base.endsWith("/") ? "" : "/") + "repository-analyses");
    }

    public Duration timeout() {
        return Duration.ofSeconds(timeoutSeconds);
    }

    private static URI validatePublicApiBaseUrl(URI uri) {
        var value = Objects.requireNonNull(uri, "publicApiBaseUrl must not be null").normalize();
        if (!"http".equalsIgnoreCase(value.getScheme()) && !"https".equalsIgnoreCase(value.getScheme())) {
            throw new CliClientValidationException("gateway-submit --gateway must be an http or https URL.");
        }
        if (value.getHost() == null || value.getHost().isBlank()) {
            throw new CliClientValidationException("gateway-submit --gateway must include a host.");
        }
        if (value.getUserInfo() != null || value.getQuery() != null || value.getFragment() != null) {
            throw new CliClientValidationException("gateway-submit --gateway must not include user info, query or fragment.");
        }
        return value;
    }

    private static String validateRepositoryUrl(String repositoryUrl) {
        var value = requireText(repositoryUrl, "--repo-url");
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException e) {
            throw new CliClientValidationException("Invalid gateway-submit --repo-url.");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new CliClientValidationException("gateway-submit --repo-url must be an HTTPS URL.");
        }
        if (uri.getUserInfo() != null) {
            throw new CliClientValidationException("gateway-submit --repo-url must not include user information.");
        }
        return value;
    }

    private static String requireSafeHeader(String value, String fieldName) {
        var text = requireText(value, fieldName);
        if (!text.matches("[A-Za-z0-9._:-]{1,128}")) {
            throw new CliClientValidationException("gateway-submit " + fieldName + " contains unsupported characters.");
        }
        return text;
    }

    private static String optionalText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new CliClientValidationException("gateway-submit requires " + fieldName + ".");
        }
        return value.trim();
    }
}
