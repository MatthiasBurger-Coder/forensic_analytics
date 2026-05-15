package de.burger.forensics.analytics.rest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.burger.forensics.analytics.application.ingestion.RepositoryAnalysisIngestionException;
import de.burger.forensics.analytics.application.ingestion.RepositoryAnalysisIngestionUseCase;
import de.burger.forensics.analytics.application.ingestion.RepositoryAnalysisQueryUseCase;
import de.burger.forensics.analytics.application.ingestion.RepositoryCheckoutException;
import de.burger.forensics.analytics.application.ingestion.command.AnalyzeRepositoryCommand;
import de.burger.forensics.analytics.application.ingestion.command.BuildContextCommand;
import de.burger.forensics.analytics.application.ingestion.query.RepositoryAnalysisView;
import de.burger.forensics.analytics.application.ingestion.query.RepositoryAnalysisWorkspaceView;
import de.burger.forensics.analytics.application.ingestion.result.AnalyzeRepositoryResult;
import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.analysis.AnalysisSessionState;
import de.burger.forensics.analytics.domain.repository.BranchReference;
import de.burger.forensics.analytics.domain.repository.CommitReference;
import de.burger.forensics.analytics.domain.repository.RepositoryReference;
import de.burger.forensics.analytics.domain.repository.SourceRoot;
import de.burger.forensics.analytics.domain.workspace.WorkspaceCleanupPolicy;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;
import de.burger.forensics.analytics.domain.workspace.WorkspacePolicy;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

final class RepositoryAnalysisHttpHandler implements HttpHandler {
    private static final int MAX_REQUEST_BYTES = 64 * 1024;
    private static final String CONTENT_TYPE = "application/json; charset=utf-8";
    private static final String CORRELATION_HEADER = "X-Correlation-Id";
    private static final String WORKFLOW = "REPOSITORY_SESSION_REGISTRATION";

    private final RepositoryAnalysisIngestionUseCase ingestionUseCase;
    private final RepositoryAnalysisQueryUseCase queryUseCase;
    private final Gson gson = new GsonBuilder().serializeNulls().disableHtmlEscaping().create();
    private final DiagnosticSanitizer diagnostics = new DiagnosticSanitizer();

    RepositoryAnalysisHttpHandler(
        RepositoryAnalysisIngestionUseCase ingestionUseCase,
        RepositoryAnalysisQueryUseCase queryUseCase
    ) {
        this.ingestionUseCase = Objects.requireNonNull(ingestionUseCase, "ingestionUseCase must not be null");
        this.queryUseCase = Objects.requireNonNull(queryUseCase, "queryUseCase must not be null");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        var correlationId = correlationId(exchange);
        try {
            route(exchange, correlationId);
        } catch (RestValidationException error) {
            writeError(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "VALIDATION_ERROR", false, correlationId, error);
        } catch (RestNotFoundException error) {
            writeError(exchange, HttpURLConnection.HTTP_NOT_FOUND, "NOT_FOUND", false, correlationId, error);
        } catch (RepositoryAnalysisIngestionException | RepositoryCheckoutException error) {
            writeError(
                exchange,
                HttpURLConnection.HTTP_UNAVAILABLE,
                "BACKEND_UNAVAILABLE",
                isIdempotent(exchange),
                correlationId,
                error
            );
        } catch (RuntimeException error) {
            writeError(
                exchange,
                HttpURLConnection.HTTP_INTERNAL_ERROR,
                "UNEXPECTED_ERROR",
                isIdempotent(exchange),
                correlationId,
                error
            );
        } finally {
            exchange.close();
        }
    }

    private void route(HttpExchange exchange, String correlationId) throws IOException {
        var method = exchange.getRequestMethod();
        var path = exchange.getRequestURI().getPath();
        if ("/api/repository-analyses".equals(path)) {
            if ("POST".equals(method)) {
                postRepositoryAnalysis(exchange, correlationId);
                return;
            }
            if ("GET".equals(method)) {
                writeJson(exchange, HttpURLConnection.HTTP_OK, new ItemsResponse<>(
                    queryUseCase.listRepositoryAnalyses().stream().map(this::toResponse).toList()
                ), correlationId);
                return;
            }
            throw new RestValidationException("method is not supported for repository analyses");
        }
        if (path.startsWith("/api/repository-analyses/") && "GET".equals(method)) {
            getRepositoryAnalysis(exchange, correlationId, path.substring("/api/repository-analyses/".length()));
            return;
        }
        if ("/api/workspaces".equals(path)) {
            if ("GET".equals(method)) {
                writeJson(exchange, HttpURLConnection.HTTP_OK, new ItemsResponse<>(
                    queryUseCase.listWorkspaces().stream().map(this::toResponse).toList()
                ), correlationId);
                return;
            }
            throw new RestValidationException("method is not supported for workspaces");
        }
        if (path.startsWith("/api/workspaces/") && "GET".equals(method)) {
            getWorkspace(exchange, correlationId, path.substring("/api/workspaces/".length()));
            return;
        }
        throw new RestNotFoundException("REST endpoint was not found");
    }

    private void postRepositoryAnalysis(HttpExchange exchange, String correlationId) throws IOException {
        var request = readRequest(exchange, RepositoryAnalysisRequest.class);
        var command = toCommand(request);
        var result = ingestionUseCase.analyze(command);
        writeJson(exchange, HttpURLConnection.HTTP_CREATED, toResponse(command, result), correlationId);
    }

    private void getRepositoryAnalysis(HttpExchange exchange, String correlationId, String rawAnalysisRunId)
        throws IOException {
        var analysisRunId = textPathSegment(rawAnalysisRunId, "analysisRunId");
        var view = queryUseCase.findRepositoryAnalysis(new AnalysisRunId(analysisRunId))
            .orElseThrow(() -> new RestNotFoundException("repository analysis was not found"));
        writeJson(exchange, HttpURLConnection.HTTP_OK, toResponse(view), correlationId);
    }

    private void getWorkspace(HttpExchange exchange, String correlationId, String rawWorkspaceId) throws IOException {
        var workspaceId = textPathSegment(rawWorkspaceId, "workspaceId");
        var view = queryUseCase.findWorkspace(new WorkspaceId(workspaceId))
            .orElseThrow(() -> new RestNotFoundException("repository-analysis workspace was not found"));
        writeJson(exchange, HttpURLConnection.HTTP_OK, toResponse(view), correlationId);
    }

    private AnalyzeRepositoryCommand toCommand(RepositoryAnalysisRequest request) {
        if (request == null) {
            throw new RestValidationException("request body must be a JSON object");
        }
        rejectWorkspaceName(request.workspaceName());
        var branch = optionalText(request.branch(), "branch");
        var commit = optionalText(request.commit(), "commit");
        if (branch.isEmpty() && commit.isEmpty()) {
            throw new RestValidationException("branch or commit must be provided");
        }
        var workspacePolicy = workspacePolicy(request.workspacePolicy());
        return new AnalyzeRepositoryCommand(
            new RepositoryReference(
                requiredHttpsRepositoryUrl(request.repositoryUrl()),
                optionalText(request.provider(), "provider"),
                Map.of()
            ),
            new BranchReference(branch, branch.isPresent()),
            new CommitReference(commit, commit.isPresent()),
            workspacePolicy,
            buildContext(request.buildContext()),
            requiredText(request.requestId(), "requestId"),
            requiredText(request.schemaVersion(), "schemaVersion")
        );
    }

    private BuildContextCommand buildContext(BuildContextRequest request) {
        if (request == null) {
            throw new RestValidationException("buildContext is required");
        }
        return new BuildContextCommand(
            requiredText(request.buildTool(), "buildContext.buildTool"),
            requiredText(request.buildId(), "buildContext.buildId"),
            optionalText(request.rootProjectName(), "buildContext.rootProjectName"),
            requiredTextList(request.declaredModules(), "buildContext.declaredModules"),
            requiredTextMap(request.attributes(), "buildContext.attributes")
        );
    }

    private WorkspacePolicy workspacePolicy(WorkspacePolicyRequest request) {
        if (request == null) {
            throw new RestValidationException("workspacePolicy is required");
        }
        var ephemeral = requiredBoolean(request.ephemeral(), "workspacePolicy.ephemeral");
        var allowShallowClone = requiredBoolean(request.allowShallowClone(), "workspacePolicy.allowShallowClone");
        var allowPartialClone = requiredBoolean(request.allowPartialClone(), "workspacePolicy.allowPartialClone");
        var allowSparseCheckout = requiredBoolean(request.allowSparseCheckout(), "workspacePolicy.allowSparseCheckout");
        var maxWorkspaceBytes = requiredNonNegativeLong(request.maxWorkspaceBytes(), "workspacePolicy.maxWorkspaceBytes");
        rejectUnsupportedWorkspacePolicy(ephemeral, allowShallowClone, allowPartialClone, allowSparseCheckout, maxWorkspaceBytes);
        return new WorkspacePolicy(
            ephemeral,
            allowShallowClone,
            allowPartialClone,
            allowSparseCheckout,
            Duration.ofSeconds(requiredNonNegativeLong(request.timeoutSeconds(), "workspacePolicy.timeoutSeconds")),
            maxWorkspaceBytes,
            ephemeral ? WorkspaceCleanupPolicy.DELETE_ON_COMPLETION : WorkspaceCleanupPolicy.RETAIN_FOR_REVIEW
        );
    }

    private RepositoryAnalysisResponse toResponse(AnalyzeRepositoryCommand command, AnalyzeRepositoryResult result) {
        var checkout = result.checkoutResult();
        return new RepositoryAnalysisResponse(
            result.analysisSessionId().value(),
            result.workspaceId().value(),
            command.repository().remoteUrl(),
            command.branch().name().orElse(null),
            command.commit().hash().orElse(null),
            checkout.resolvedRemoteUrl(),
            checkout.resolvedCommit(),
            checkout.checkoutStatus(),
            AnalysisSessionState.REGISTERED.name(),
            WORKFLOW,
            null,
            checkout.detectedSourceRoots().stream().map(SourceRoot::path).toList(),
            sanitizedDiagnostics(checkout.diagnostics())
        );
    }

    private RepositoryAnalysisResponse toResponse(RepositoryAnalysisView view) {
        return new RepositoryAnalysisResponse(
            view.analysisRunId().value(),
            view.workspaceId().value(),
            view.repositoryUrl(),
            view.branch().orElse(null),
            view.commit().orElse(null),
            view.resolvedRemoteUrl(),
            view.resolvedCommit(),
            view.checkoutStatus(),
            view.status().name(),
            view.workflow().name(),
            view.createdAt().map(Instant::toString).orElse(null),
            view.sourceRoots(),
            sanitizedDiagnostics(view.diagnostics())
        );
    }

    private WorkspaceResponse toResponse(RepositoryAnalysisWorkspaceView view) {
        return new WorkspaceResponse(
            view.workspaceId().value(),
            view.name().orElse(null),
            view.status().orElse(null),
            view.createdAt().map(Instant::toString).orElse(null),
            view.updatedAt().map(Instant::toString).orElse(null),
            view.repositoryAnalyses().stream().map(this::toResponse).toList()
        );
    }

    private <T> T readRequest(HttpExchange exchange, Class<T> requestType) throws IOException {
        var body = requestBody(exchange);
        if (body.isBlank()) {
            throw new RestValidationException("request body must not be blank");
        }
        try {
            return gson.fromJson(body, requestType);
        } catch (JsonParseException error) {
            throw new RestValidationException("request body must be valid JSON", error);
        }
    }

    private String requestBody(HttpExchange exchange) throws IOException {
        try (var input = exchange.getRequestBody()) {
            var body = input.readNBytes(MAX_REQUEST_BYTES + 1);
            if (body.length > MAX_REQUEST_BYTES) {
                throw new RestValidationException("request body exceeds 65536 bytes");
            }
            return new String(body, StandardCharsets.UTF_8);
        }
    }

    private void writeJson(HttpExchange exchange, int statusCode, Object response, String correlationId)
        throws IOException {
        var body = gson.toJson(response).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE);
        exchange.getResponseHeaders().set(CORRELATION_HEADER, correlationId);
        exchange.sendResponseHeaders(statusCode, body.length);
        try (var output = exchange.getResponseBody()) {
            output.write(body);
        }
    }

    private void writeError(
        HttpExchange exchange,
        int statusCode,
        String code,
        boolean retryable,
        String correlationId,
        RuntimeException error
    ) throws IOException {
        var message = switch (code) {
            case "VALIDATION_ERROR" -> "Request validation failed";
            case "NOT_FOUND" -> "Resource was not found";
            case "BACKEND_UNAVAILABLE" -> "Backend dependency is unavailable";
            default -> "Unexpected REST API failure";
        };
        writeJson(exchange, statusCode, new ErrorResponse(
            code,
            message,
            retryable,
            correlationId,
            List.of(diagnostics.sanitize(error.getMessage()))
        ), correlationId);
    }

    private List<String> sanitizedDiagnostics(List<String> rawDiagnostics) {
        return rawDiagnostics.stream().map(diagnostics::sanitize).toList();
    }

    private static String correlationId(HttpExchange exchange) {
        var header = exchange.getRequestHeaders().getFirst(CORRELATION_HEADER);
        if (header == null || header.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return header.strip().replaceAll("[^A-Za-z0-9_.:-]", "_");
    }

    private static boolean isIdempotent(HttpExchange exchange) {
        return "GET".equals(exchange.getRequestMethod());
    }

    private static String textPathSegment(String rawValue, String fieldName) {
        var decoded = URLDecoder.decode(rawValue, StandardCharsets.UTF_8);
        if (decoded.isBlank() || decoded.contains("/")) {
            throw new RestValidationException(fieldName + " path segment must not be blank");
        }
        return decoded;
    }

    private static void rejectWorkspaceName(String value) {
        if (value != null && !value.isBlank()) {
            throw new RestValidationException("workspaceName is not supported by the repository-analysis session model");
        }
    }

    private static String requiredHttpsRepositoryUrl(String value) {
        var repositoryUrl = requiredText(value, "repositoryUrl");
        var uri = parseRepositoryUri(repositoryUrl);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getHost().isBlank()) {
            throw new RestValidationException("repositoryUrl must use an https URL");
        }
        if (uri.getUserInfo() != null) {
            throw new RestValidationException("repositoryUrl must not include user information");
        }
        return repositoryUrl;
    }

    private static URI parseRepositoryUri(String repositoryUrl) {
        try {
            return URI.create(repositoryUrl);
        } catch (IllegalArgumentException error) {
            throw new RestValidationException("repositoryUrl must be a valid https URL", error);
        }
    }

    private static void rejectUnsupportedWorkspacePolicy(
        boolean ephemeral,
        boolean allowShallowClone,
        boolean allowPartialClone,
        boolean allowSparseCheckout,
        long maxWorkspaceBytes
    ) {
        if (ephemeral) {
            throw new RestValidationException("workspacePolicy.ephemeral is not supported by this REST slice");
        }
        if (allowShallowClone || allowPartialClone || allowSparseCheckout) {
            throw new RestValidationException("workspacePolicy clone mode options are not supported by this REST slice");
        }
        if (maxWorkspaceBytes > 0) {
            throw new RestValidationException("workspacePolicy.maxWorkspaceBytes is not enforced by this REST slice");
        }
    }

    private static String requiredText(String value, String fieldName) {
        var text = value == null ? "" : value.strip();
        if (text.isBlank()) {
            throw new RestValidationException(fieldName + " is required");
        }
        return text;
    }

    private static Optional<String> optionalText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        var text = value.strip();
        if (text.isBlank()) {
            throw new RestValidationException(fieldName + " must not be blank");
        }
        return Optional.of(text);
    }

    private static List<String> requiredTextList(List<String> values, String fieldName) {
        if (values == null) {
            throw new RestValidationException(fieldName + " is required");
        }
        return List.copyOf(values).stream()
            .map(value -> requiredText(value, fieldName))
            .toList();
    }

    private static Map<String, String> requiredTextMap(Map<String, String> values, String fieldName) {
        if (values == null) {
            throw new RestValidationException(fieldName + " is required");
        }
        values.forEach((key, value) -> {
            requiredText(key, fieldName + ".key");
            requiredText(value, fieldName + "[" + key + "]");
        });
        return Map.copyOf(values);
    }

    private static boolean requiredBoolean(Boolean value, String fieldName) {
        if (value == null) {
            throw new RestValidationException(fieldName + " is required");
        }
        return value;
    }

    private static long requiredNonNegativeLong(Long value, String fieldName) {
        if (value == null) {
            throw new RestValidationException(fieldName + " is required");
        }
        if (value < 0) {
            throw new RestValidationException(fieldName + " must not be negative");
        }
        return value;
    }

    private record ItemsResponse<T>(List<T> items) {
    }

    private record RepositoryAnalysisRequest(
        String repositoryUrl,
        String provider,
        String branch,
        String commit,
        String workspaceName,
        String requestId,
        String schemaVersion,
        BuildContextRequest buildContext,
        WorkspacePolicyRequest workspacePolicy
    ) {
    }

    private record BuildContextRequest(
        String buildTool,
        String buildId,
        String rootProjectName,
        List<String> declaredModules,
        Map<String, String> attributes
    ) {
    }

    private record WorkspacePolicyRequest(
        Boolean ephemeral,
        Boolean allowShallowClone,
        Boolean allowPartialClone,
        Boolean allowSparseCheckout,
        Long timeoutSeconds,
        Long maxWorkspaceBytes
    ) {
    }

    private record RepositoryAnalysisResponse(
        String analysisRunId,
        String workspaceId,
        String repositoryUrl,
        String branch,
        String commit,
        String resolvedRemoteUrl,
        String resolvedCommit,
        String checkoutStatus,
        String status,
        String workflow,
        String createdAt,
        List<String> sourceRoots,
        List<String> diagnostics
    ) {
    }

    private record WorkspaceResponse(
        String workspaceId,
        String name,
        String status,
        String createdAt,
        String updatedAt,
        List<RepositoryAnalysisResponse> repositoryAnalyses
    ) {
    }

    private record ErrorResponse(
        String code,
        String message,
        boolean retryable,
        String correlationId,
        List<String> diagnostics
    ) {
    }

    private static final class RestValidationException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        RestValidationException(String message) {
            super(message);
        }

        RestValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static final class RestNotFoundException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        RestNotFoundException(String message) {
            super(message);
        }
    }
}
