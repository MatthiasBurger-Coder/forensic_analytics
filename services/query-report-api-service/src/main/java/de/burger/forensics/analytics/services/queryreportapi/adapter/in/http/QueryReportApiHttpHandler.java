package de.burger.forensics.analytics.services.queryreportapi.adapter.in.http;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.burger.forensics.analytics.services.queryreportapi.application.QueryReportApiIdempotencyConflictException;
import de.burger.forensics.analytics.services.queryreportapi.application.QueryReportApiRepositoryAnalysisException;
import de.burger.forensics.analytics.services.queryreportapi.application.QueryReportApiRepositoryAnalysisSubmissionService;
import de.burger.forensics.analytics.services.queryreportapi.application.QueryReportApiStatusService;
import de.burger.forensics.analytics.services.queryreportapi.application.QueryReportApiWorkspaceException;
import de.burger.forensics.analytics.services.queryreportapi.application.QueryReportApiWorkspaceService;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.BuildContext;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.StatusRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.SubmissionRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.WorkspacePolicy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public final class QueryReportApiHttpHandler implements HttpHandler {
    private static final int MAX_REQUEST_BODY_BYTES = 64 * 1024;
    private static final Pattern SAFE_MUTATION_HEADER = Pattern.compile("[A-Za-z0-9._:-]{1,128}");
    private static final String HEALTH = "{\"status\":\"UP\"}";
    private static final String NOT_FOUND = "{\"code\":\"NOT_FOUND\",\"message\":\"Query report API endpoint is not available in this slice\"}";
    private static final String METHOD_NOT_ALLOWED = "{\"code\":\"METHOD_NOT_ALLOWED\",\"message\":\"Query report API endpoint does not support this method\"}";

    private final QueryReportApiStatusService statusService;
    private final QueryReportApiRepositoryAnalysisSubmissionService repositoryAnalysisSubmissionService;
    private final QueryReportApiWorkspaceService workspaceService;
    private final Gson gson;

    public QueryReportApiHttpHandler(
        QueryReportApiStatusService statusService,
        QueryReportApiRepositoryAnalysisSubmissionService repositoryAnalysisSubmissionService,
        QueryReportApiWorkspaceService workspaceService
    ) {
        this.statusService = statusService;
        this.repositoryAnalysisSubmissionService = repositoryAnalysisSubmissionService;
        this.workspaceService = workspaceService;
        this.gson = new Gson();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if ("GET".equals(exchange.getRequestMethod())) {
                handleGet(exchange);
                return;
            }
            if ("POST".equals(exchange.getRequestMethod())) {
                handlePost(exchange);
                return;
            }
            write(exchange, 405, METHOD_NOT_ALLOWED);
        } finally {
            exchange.close();
        }
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        var path = exchange.getRequestURI().getPath();
        switch (path) {
            case "/health", "/api/health" -> write(exchange, 200, HEALTH);
            case "/api/status" -> write(exchange, 200, gson.toJson(statusService.currentStatus()));
            default -> {
                if (path.startsWith("/api/workspaces/")) {
                    if (isWorkspaceCheckoutResultRoute(path)) {
                        handleWorkspaceCheckoutResultGet(exchange, path);
                        return;
                    }
                    handleWorkspaceGet(exchange, path);
                    return;
                }
                handleRepositoryAnalysisStatusGet(exchange, path);
            }
        }
    }

    private void handleWorkspaceGet(HttpExchange exchange, String path) throws IOException {
        var prefix = "/api/workspaces/";
        var workspaceId = path.substring(prefix.length());
        if (workspaceId.isBlank() || workspaceId.contains("/") || workspaceId.contains("\\")) {
            write(exchange, 404, NOT_FOUND);
            return;
        }
        var correlationId = "";
        try {
            correlationId = requiredMutationHeader(exchange, "X-Correlation-Id");
            var workspace = workspaceService.get(
                generatedRequestId("workspace-get", correlationId, workspaceId),
                correlationId,
                workspaceId
            );
            write(exchange, 200, gson.toJson(workspace), correlationId);
        } catch (QueryReportApiWorkspaceException error) {
            writeError(exchange, error.statusCode(), error.errorCode(), error.retryable(), error.getMessage(), correlationId);
        } catch (IllegalArgumentException | NullPointerException error) {
            writeError(exchange, 400, "VALIDATION_ERROR", false, "Invalid repository workspace request", correlationId);
        }
    }

    private void handleWorkspaceCheckoutResultGet(HttpExchange exchange, String path) throws IOException {
        var correlationId = "";
        try {
            correlationId = requiredMutationHeader(exchange, "X-Correlation-Id");
            var workspaceId = workspaceCheckoutResultWorkspaceId(path);
            var workspace = workspaceService.waitForCheckout(
                generatedRequestId("workspace-checkout-result", correlationId, workspaceId),
                correlationId,
                workspaceId
            );
            write(exchange, 200, gson.toJson(workspace), correlationId);
        } catch (QueryReportApiWorkspaceException error) {
            writeError(exchange, error.statusCode(), error.errorCode(), error.retryable(), error.getMessage(), correlationId);
        } catch (IllegalArgumentException | NullPointerException error) {
            writeError(exchange, 400, "VALIDATION_ERROR", false, "Invalid repository workspace request", correlationId);
        }
    }

    private void handleRepositoryAnalysisStatusGet(HttpExchange exchange, String path) throws IOException {
        var prefix = "/api/repository-analyses/";
        if (!path.startsWith(prefix) || path.endsWith("/jobs")) {
            write(exchange, 404, NOT_FOUND);
            return;
        }
        var analysisRunId = path.substring(prefix.length());
        if (analysisRunId.isBlank() || analysisRunId.contains("/")) {
            write(exchange, 404, NOT_FOUND);
            return;
        }
        var correlationId = "";
        try {
            correlationId = requiredMutationHeader(exchange, "X-Correlation-Id");
            var status = repositoryAnalysisSubmissionService.status(new StatusRequest(
                "query-report-status-" + analysisRunId,
                correlationId,
                analysisRunId
            ));
            write(exchange, 200, gson.toJson(status), correlationId);
        } catch (QueryReportApiRepositoryAnalysisException error) {
            writeError(exchange, error.statusCode(), error.errorCode(), error.retryable(), error.getMessage(), correlationId);
        } catch (IllegalArgumentException | NullPointerException error) {
            writeError(exchange, 400, "VALIDATION_ERROR", false, "Invalid repository analysis status request", correlationId);
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        var path = exchange.getRequestURI().getPath();
        switch (path) {
            case "/api/workspace-metadata" -> {
                handleWorkspaceMetadataPost(exchange);
                return;
            }
            case "/api/workspaces" -> {
                handleWorkspacePost(exchange);
                return;
            }
            case "/api/repository-analyses" -> {
                handleRepositoryAnalysisPost(exchange);
                return;
            }
            default -> {
                if (isWorkspaceRefreshRoute(path)) {
                    handleWorkspaceBranchRefreshPost(exchange, path);
                    return;
                }
                write(exchange, isKnownGetRoute(path) ? 405 : 404, isKnownGetRoute(path) ? METHOD_NOT_ALLOWED : NOT_FOUND);
            }
        }
    }

    private void handleWorkspaceMetadataPost(HttpExchange exchange) throws IOException {
        var correlationId = "";
        try {
            correlationId = requiredMutationHeader(exchange, "X-Correlation-Id");
            var idempotencyKey = requiredMutationHeader(exchange, "Idempotency-Key");
            var payload = workspaceMetadataPayload(readRequestBody(exchange));
            var metadata = workspaceService.previewMetadata(
                generatedRequestId("workspace-metadata", correlationId, payload.repositoryUrl),
                idempotencyKey,
                correlationId,
                payload.repositoryUrl
            );
            write(exchange, 200, gson.toJson(metadata), correlationId);
        } catch (QueryReportApiIdempotencyConflictException error) {
            writeError(exchange, 409, "IDEMPOTENCY_CONFLICT", false, "The idempotency key was already used with different input.", correlationId);
        } catch (QueryReportApiWorkspaceException error) {
            writeError(exchange, error.statusCode(), error.errorCode(), error.retryable(), error.getMessage(), correlationId);
        } catch (IllegalArgumentException | NullPointerException | JsonSyntaxException error) {
            writeError(exchange, 400, "VALIDATION_ERROR", false, "Invalid repository workspace request", correlationId);
        }
    }

    private void handleWorkspacePost(HttpExchange exchange) throws IOException {
        var correlationId = "";
        try {
            correlationId = requiredMutationHeader(exchange, "X-Correlation-Id");
            var idempotencyKey = requiredMutationHeader(exchange, "Idempotency-Key");
            var payload = workspacePayload(readRequestBody(exchange));
            var workspace = workspaceService.create(
                generatedRequestId("workspace-create", correlationId, payload.repositoryUrl + "|" + payload.selectedBranch),
                idempotencyKey,
                correlationId,
                payload.repositoryUrl,
                payload.selectedBranch,
                payload.workspacePolicy.toWorkspaceDomain()
            );
            write(exchange, 200, gson.toJson(workspace), correlationId);
        } catch (QueryReportApiIdempotencyConflictException error) {
            writeError(exchange, 409, "IDEMPOTENCY_CONFLICT", false, "The idempotency key was already used with different input.", correlationId);
        } catch (QueryReportApiWorkspaceException error) {
            writeError(exchange, error.statusCode(), error.errorCode(), error.retryable(), error.getMessage(), correlationId);
        } catch (IllegalArgumentException | NullPointerException | JsonSyntaxException error) {
            writeError(exchange, 400, "VALIDATION_ERROR", false, "Invalid repository workspace request", correlationId);
        }
    }

    private void handleWorkspaceBranchRefreshPost(HttpExchange exchange, String path) throws IOException {
        var correlationId = "";
        try {
            correlationId = requiredMutationHeader(exchange, "X-Correlation-Id");
            var idempotencyKey = requiredMutationHeader(exchange, "Idempotency-Key");
            var route = workspaceRefreshRoute(path);
            var refresh = workspaceService.refresh(
                generatedRequestId("workspace-refresh", correlationId, route.workspaceId() + "|" + route.workspaceBranchId()),
                idempotencyKey,
                correlationId,
                route.workspaceId(),
                route.workspaceBranchId()
            );
            write(exchange, 200, gson.toJson(refresh), correlationId);
        } catch (QueryReportApiIdempotencyConflictException error) {
            writeError(exchange, 409, "IDEMPOTENCY_CONFLICT", false, "The idempotency key was already used with different input.", correlationId);
        } catch (QueryReportApiWorkspaceException error) {
            writeError(exchange, error.statusCode(), error.errorCode(), error.retryable(), error.getMessage(), correlationId);
        } catch (IllegalArgumentException | NullPointerException error) {
            writeError(exchange, 400, "VALIDATION_ERROR", false, "Invalid repository workspace request", correlationId);
        }
    }

    private void handleRepositoryAnalysisPost(HttpExchange exchange) throws IOException {
        var correlationId = "";
        try {
            correlationId = requiredMutationHeader(exchange, "X-Correlation-Id");
            var request = payload(readRequestBody(exchange)).toCommand(
                requiredMutationHeader(exchange, "Idempotency-Key"),
                correlationId
            );
            var submission = repositoryAnalysisSubmissionService.submit(request);
            write(exchange, 202, gson.toJson(submission), submission.correlationId());
        } catch (QueryReportApiIdempotencyConflictException error) {
            writeError(exchange, 409, "CONFLICT", false, "Idempotency key conflicts with a previous request", correlationId);
        } catch (QueryReportApiRepositoryAnalysisException error) {
            writeError(exchange, error.statusCode(), error.errorCode(), error.retryable(), error.getMessage(), correlationId);
        } catch (IllegalArgumentException | NullPointerException | JsonSyntaxException error) {
            writeError(exchange, 400, "VALIDATION_ERROR", false, "Invalid repository analysis request", correlationId);
        }
    }

    private static boolean isKnownGetRoute(String path) {
        return "/health".equals(path)
            || "/api/health".equals(path)
            || "/api/status".equals(path)
            || "/api/workspace-metadata".equals(path)
            || path.startsWith("/api/workspaces/");
    }

    private StartRepositoryAnalysisPayload payload(String requestBody) {
        return gson.fromJson(requestBody, StartRepositoryAnalysisPayload.class);
    }

    private WorkspaceMetadataPayload workspaceMetadataPayload(String requestBody) {
        return gson.fromJson(requestBody, WorkspaceMetadataPayload.class);
    }

    private WorkspacePayload workspacePayload(String requestBody) {
        return gson.fromJson(requestBody, WorkspacePayload.class);
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (var body = exchange.getRequestBody()) {
            var bytes = body.readNBytes(MAX_REQUEST_BODY_BYTES + 1);
            if (bytes.length > MAX_REQUEST_BODY_BYTES) {
                throw new IllegalArgumentException("request body exceeds the query report API limit");
            }
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private static void write(HttpExchange exchange, int statusCode, String body) throws IOException {
        write(exchange, statusCode, body, null);
    }

    private static void write(HttpExchange exchange, int statusCode, String body, String correlationId) throws IOException {
        var bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        if (correlationId != null && !correlationId.isBlank()) {
            exchange.getResponseHeaders().set("X-Correlation-Id", correlationId);
        }
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static void writeError(
        HttpExchange exchange,
        int statusCode,
        String code,
        boolean retryable,
        String message,
        String correlationId
    ) throws IOException {
        var safeCorrelationId = correlationId == null || correlationId.isBlank() ? "unknown" : correlationId;
        write(
            exchange,
            statusCode,
            new Gson().toJson(new ErrorEnvelope(code, message, retryable, safeCorrelationId, List.of())),
            safeCorrelationId
        );
    }

    private static String firstHeader(HttpExchange exchange, String name) {
        return exchange.getRequestHeaders().getFirst(name);
    }

    private static String requiredMutationHeader(HttpExchange exchange, String name) {
        var value = firstHeader(exchange, name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        var safeValue = value.trim();
        if (!SAFE_MUTATION_HEADER.matcher(safeValue).matches()) {
            throw new IllegalArgumentException(name + " contains unsupported characters");
        }
        return safeValue;
    }

    private static boolean isWorkspaceRefreshRoute(String path) {
        return path.startsWith("/api/workspaces/") && path.endsWith("/refresh") && path.contains("/branches/");
    }

    private static boolean isWorkspaceCheckoutResultRoute(String path) {
        return path.startsWith("/api/workspaces/") && path.endsWith("/checkout-result");
    }

    private static String workspaceCheckoutResultWorkspaceId(String path) {
        var prefix = "/api/workspaces/";
        var suffix = "/checkout-result";
        if (!path.startsWith(prefix) || !path.endsWith(suffix)) {
            throw new IllegalArgumentException("workspace checkout result route is invalid");
        }
        var workspaceId = path.substring(prefix.length(), path.length() - suffix.length());
        if (workspaceId.isBlank() || workspaceId.contains("/") || workspaceId.contains("\\")) {
            throw new IllegalArgumentException("workspace checkout result route is invalid");
        }
        return workspaceId;
    }

    private static WorkspaceRefreshRoute workspaceRefreshRoute(String path) {
        var prefix = "/api/workspaces/";
        var branchSeparator = "/branches/";
        var refreshSuffix = "/refresh";
        if (!path.startsWith(prefix) || !path.endsWith(refreshSuffix)) {
            throw new IllegalArgumentException("workspace refresh route is invalid");
        }
        var branchSeparatorIndex = path.indexOf(branchSeparator, prefix.length());
        if (branchSeparatorIndex < 0) {
            throw new IllegalArgumentException("workspace refresh route is invalid");
        }
        var workspaceId = path.substring(prefix.length(), branchSeparatorIndex);
        var branchStart = branchSeparatorIndex + branchSeparator.length();
        var branchEnd = path.length() - refreshSuffix.length();
        var workspaceBranchId = path.substring(branchStart, branchEnd);
        if (workspaceId.isBlank() || workspaceBranchId.isBlank()
            || workspaceId.contains("/") || workspaceBranchId.contains("/")) {
            throw new IllegalArgumentException("workspace refresh route is invalid");
        }
        return new WorkspaceRefreshRoute(workspaceId, workspaceBranchId);
    }

    private static String generatedRequestId(String prefix, String correlationId, String discriminator) {
        return prefix + "-" + Integer.toUnsignedString(Objects.hash(correlationId, discriminator), 16);
    }

    private record WorkspaceRefreshRoute(String workspaceId, String workspaceBranchId) {
    }

    private record ErrorEnvelope(
        String code,
        String message,
        boolean retryable,
        String correlationId,
        List<Object> diagnostics
    ) {
    }

    private static final class WorkspaceMetadataPayload {
        private String repositoryUrl;
    }

    private static final class WorkspacePayload {
        private String repositoryUrl;
        private String selectedBranch;
        private WorkspacePolicyPayload workspacePolicy;
    }

    private static final class StartRepositoryAnalysisPayload {
        private String requestId;
        private String schemaVersion;
        private List<String> requestedOutputs;
        private String repositoryUrl;
        private String provider;
        private String branch;
        private String commit;
        private String workspaceName;
        private BuildContextPayload buildContext;
        private WorkspacePolicyPayload workspacePolicy;

        private SubmissionRequest toCommand(String idempotencyKey, String correlationId) {
            return new SubmissionRequest(
                requestId,
                idempotencyKey,
                schemaVersion,
                correlationId,
                requestedOutputs,
                repositoryUrl,
                provider,
                branch,
                commit,
                workspaceName,
                buildContext.toDomain(),
                workspacePolicy.toDomain()
            );
        }
    }

    private static final class BuildContextPayload {
        private String buildTool;
        private String buildId;
        private String rootProjectName;
        private List<String> declaredModules;
        private Map<String, String> attributes;

        private BuildContext toDomain() {
            return new BuildContext(buildTool, buildId, rootProjectName, declaredModules, attributes);
        }
    }

    private static final class WorkspacePolicyPayload {
        private Boolean ephemeral;
        private Boolean allowShallowClone;
        private Boolean allowPartialClone;
        private Boolean allowSparseCheckout;
        private Long timeoutSeconds;
        private Long maxWorkspaceBytes;

        private WorkspacePolicy toDomain() {
            return new WorkspacePolicy(
                required(ephemeral, "ephemeral"),
                required(allowShallowClone, "allow shallow clone"),
                required(allowPartialClone, "allow partial clone"),
                required(allowSparseCheckout, "allow sparse checkout"),
                required(timeoutSeconds, "timeout seconds"),
                required(maxWorkspaceBytes, "max workspace bytes")
            );
        }

        private de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspacePolicy toWorkspaceDomain() {
            return new de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspacePolicy(
                required(ephemeral, "ephemeral"),
                required(allowShallowClone, "allow shallow clone"),
                required(allowPartialClone, "allow partial clone"),
                required(allowSparseCheckout, "allow sparse checkout"),
                required(timeoutSeconds, "timeout seconds"),
                required(maxWorkspaceBytes, "max workspace bytes")
            );
        }
    }

    private static boolean required(Boolean value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    private static long required(Long value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
