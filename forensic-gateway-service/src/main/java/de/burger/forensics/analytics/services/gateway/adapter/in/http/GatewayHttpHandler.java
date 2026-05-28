package de.burger.forensics.analytics.services.gateway.adapter.in.http;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.burger.forensics.analytics.services.gateway.application.GatewayIdempotencyConflictException;
import de.burger.forensics.analytics.services.gateway.application.GatewayRepositoryAnalysisException;
import de.burger.forensics.analytics.services.gateway.application.GatewayRepositoryAnalysisSubmissionService;
import de.burger.forensics.analytics.services.gateway.application.GatewayStatusService;
import de.burger.forensics.analytics.services.gateway.domain.GatewayRepositoryAnalysis.BuildContext;
import de.burger.forensics.analytics.services.gateway.domain.GatewayRepositoryAnalysis.StatusRequest;
import de.burger.forensics.analytics.services.gateway.domain.GatewayRepositoryAnalysis.SubmissionRequest;
import de.burger.forensics.analytics.services.gateway.domain.GatewayRepositoryAnalysis.WorkspacePolicy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class GatewayHttpHandler implements HttpHandler {
    private static final int MAX_REQUEST_BODY_BYTES = 64 * 1024;
    private static final Pattern SAFE_MUTATION_HEADER = Pattern.compile("[A-Za-z0-9._:-]{1,128}");
    private static final String HEALTH = "{\"status\":\"UP\"}";
    private static final String NOT_FOUND = "{\"code\":\"NOT_FOUND\",\"message\":\"Gateway endpoint is not available in this slice\"}";
    private static final String METHOD_NOT_ALLOWED = "{\"code\":\"METHOD_NOT_ALLOWED\",\"message\":\"Gateway endpoint does not support this method\"}";

    private final GatewayStatusService statusService;
    private final GatewayRepositoryAnalysisSubmissionService repositoryAnalysisSubmissionService;
    private final Gson gson;

    public GatewayHttpHandler(
        GatewayStatusService statusService,
        GatewayRepositoryAnalysisSubmissionService repositoryAnalysisSubmissionService
    ) {
        this.statusService = statusService;
        this.repositoryAnalysisSubmissionService = repositoryAnalysisSubmissionService;
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
            default -> handleRepositoryAnalysisStatusGet(exchange, path);
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
                "gateway-status-" + analysisRunId,
                correlationId,
                analysisRunId
            ));
            write(exchange, 200, gson.toJson(status), status.correlationId());
        } catch (GatewayRepositoryAnalysisException error) {
            writeError(exchange, error.statusCode(), error.errorCode(), error.retryable(), error.getMessage(), correlationId);
        } catch (IllegalArgumentException | NullPointerException error) {
            writeError(exchange, 400, "VALIDATION_ERROR", false, "Invalid repository analysis status request", correlationId);
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        var path = exchange.getRequestURI().getPath();
        if (!"/api/repository-analyses".equals(path)) {
            write(exchange, isKnownGetRoute(path) ? 405 : 404, isKnownGetRoute(path) ? METHOD_NOT_ALLOWED : NOT_FOUND);
            return;
        }
        var correlationId = "";
        try {
            correlationId = requiredMutationHeader(exchange, "X-Correlation-Id");
            var request = payload(readRequestBody(exchange)).toCommand(
                requiredMutationHeader(exchange, "Idempotency-Key"),
                correlationId
            );
            var submission = repositoryAnalysisSubmissionService.submit(request);
            write(exchange, 202, gson.toJson(submission), submission.correlationId());
        } catch (GatewayIdempotencyConflictException error) {
            writeError(exchange, 409, "CONFLICT", false, "Idempotency key conflicts with a previous request", correlationId);
        } catch (GatewayRepositoryAnalysisException error) {
            writeError(exchange, error.statusCode(), error.errorCode(), error.retryable(), error.getMessage(), correlationId);
        } catch (IllegalArgumentException | NullPointerException | JsonSyntaxException error) {
            writeError(exchange, 400, "VALIDATION_ERROR", false, "Invalid repository analysis request", correlationId);
        }
    }

    private static boolean isKnownGetRoute(String path) {
        return "/health".equals(path) || "/api/health".equals(path) || "/api/status".equals(path);
    }

    private StartRepositoryAnalysisPayload payload(String requestBody) {
        return gson.fromJson(requestBody, StartRepositoryAnalysisPayload.class);
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (var body = exchange.getRequestBody()) {
            var bytes = body.readNBytes(MAX_REQUEST_BODY_BYTES + 1);
            if (bytes.length > MAX_REQUEST_BODY_BYTES) {
                throw new IllegalArgumentException("request body exceeds the Gateway limit");
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
        write(exchange, statusCode, """
            {"code":"%s","message":"%s","retryable":%s,"correlationId":"%s","diagnostics":[]}
            """.formatted(code, message, retryable, safeCorrelationId).trim(), safeCorrelationId);
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
