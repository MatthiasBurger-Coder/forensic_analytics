package de.burger.forensics.analytics.services.cliclient.adapter.out.http;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import de.burger.forensics.analytics.services.cliclient.application.port.out.RepositoryAnalysisSubmissionPort;
import de.burger.forensics.analytics.services.cliclient.domain.CliClientSubmissionCommand;
import de.burger.forensics.analytics.services.cliclient.domain.CliClientSubmissionResult;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

public final class HttpRepositoryAnalysisSubmissionClient implements RepositoryAnalysisSubmissionPort {
    private static final int HTTP_ACCEPTED = 202;

    private final HttpClient httpClient;
    private final Gson gson;

    public HttpRepositoryAnalysisSubmissionClient(HttpClient httpClient, Gson gson) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.gson = Objects.requireNonNull(gson, "gson must not be null");
    }

    @Override
    public CliClientSubmissionResult submit(CliClientSubmissionCommand command) {
        var response = send(command);
        if (response.statusCode() == HTTP_ACCEPTED) {
            return success(response.body());
        }
        throw error(response.body(), response.statusCode());
    }

    private HttpResponse<String> send(CliClientSubmissionCommand command) {
        var request = HttpRequest.newBuilder(command.repositoryAnalysesUri())
            .timeout(command.timeout())
            .header("Accept", "application/json")
            .header("Content-Type", "application/json; charset=utf-8")
            .header("X-Correlation-Id", command.correlationId())
            .header("Idempotency-Key", command.idempotencyKey())
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload(command)), StandardCharsets.UTF_8))
            .build();
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new PublicApiClientException("Public API request failed");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PublicApiClientException("Public API request was interrupted");
        }
    }

    private static Map<String, Object> payload(CliClientSubmissionCommand command) {
        return Map.of(
            "requestId", command.requestId(),
            "schemaVersion", command.schemaVersion(),
            "requestedOutputs", command.requestedOutputs(),
            "repositoryUrl", command.repositoryUrl(),
            "provider", command.provider(),
            "branch", command.branch(),
            "commit", command.commit(),
            "buildContext", Map.of(
                "buildTool", command.buildTool(),
                "buildId", command.buildId(),
                "rootProjectName", command.rootProjectName(),
                "declaredModules", command.declaredModules(),
                "attributes", Map.of()
            ),
            "workspacePolicy", Map.of(
                "ephemeral", false,
                "allowShallowClone", command.allowShallowClone(),
                "allowPartialClone", false,
                "allowSparseCheckout", false,
                "timeoutSeconds", command.timeoutSeconds(),
                "maxWorkspaceBytes", command.maxWorkspaceBytes()
            )
        );
    }

    private CliClientSubmissionResult success(String body) {
        var json = parseObject(body);
        return new CliClientSubmissionResult(
            requiredString(json, "analysisRunId"),
            requiredString(json, "status"),
            requiredString(json, "statusUrl"),
            requiredString(json, "jobsUrl"),
            requiredString(json, "btmDeliveryStatus"),
            optionalString(json, "btmDeliveryService"),
            requiredString(json, "correlationId"),
            diagnosticCount(json)
        );
    }

    private PublicApiClientException error(String body, int statusCode) {
        try {
            var json = parseObject(body);
            var code = optionalString(json, "code");
            var retryable = json.has("retryable") && json.get("retryable").isJsonPrimitive()
                ? json.get("retryable").getAsBoolean()
                : false;
            var correlationId = optionalString(json, "correlationId");
            return new PublicApiClientException(
                "Public API error status=" + statusCode
                    + " code=" + emptyAsUnknown(code)
                    + " retryable=" + retryable
                    + " correlationId=" + emptyAsUnknown(correlationId)
            );
        } catch (RuntimeException ignored) {
            return new PublicApiClientException(
                "Public API error status=" + statusCode + " code=UNKNOWN retryable=false correlationId=unknown"
            );
        }
    }

    private static JsonObject parseObject(String body) {
        try {
            return JsonParser.parseString(body).getAsJsonObject();
        } catch (IllegalStateException | JsonSyntaxException e) {
            throw new PublicApiClientException("Public API returned invalid JSON");
        }
    }

    private static String requiredString(JsonObject json, String name) {
        var value = optionalString(json, name);
        if (value.isBlank()) {
            throw new PublicApiClientException("Public API response is missing public field: " + name);
        }
        return value;
    }

    private static String optionalString(JsonObject json, String name) {
        if (!json.has(name) || json.get(name).isJsonNull() || !json.get(name).isJsonPrimitive()) {
            return "";
        }
        return json.get(name).getAsString();
    }

    private static int diagnosticCount(JsonObject json) {
        if (!json.has("diagnostics") || !json.get("diagnostics").isJsonArray()) {
            return 0;
        }
        return json.getAsJsonArray("diagnostics").size();
    }

    private static String emptyAsUnknown(String value) {
        return value.isBlank() ? "unknown" : value;
    }
}
